package com.example.demo.Services;

import com.example.demo.Models.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PaymentService {

    @PersistenceContext
    private EntityManager em;
    private final CommunityService community;
    private final StateMovementService movementService;

    public PaymentService(CommunityService community, StateMovementService movementService) {
        this.community = community;
        this.movementService = movementService;
    }

    public Payment createOrder(PaymentConceptType conceptType,
                               Long referenceId,
                               String description,
                               Integer amount,
                               PaymentMethod method,
                               boolean payImmediately,
                               User payer,
                               User recipient) {
        PaymentMethod effectiveMethod = method != null ? method : PaymentMethod.BANK_TRANSFER;
        int safeAmount = (amount != null && amount > 0) ? amount : 20000;
        String safeDesc = description != null && description.length() > 180
                ? description.substring(0, 180)
                : (description != null ? description : conceptType.name() + " #" + referenceId);

        Payment payment = new Payment(conceptType, referenceId, safeDesc, safeAmount, effectiveMethod, payer, recipient);
        em.persist(payment);
        em.flush();

        movementService.record(
                MovementEntityType.PAYMENT,
                payment.getId(),
                payer.getUsername() + " → " + recipient.getUsername() + " ($ " + safeAmount + ")",
                null,
                PaymentStatus.PENDING_PAYMENT.name(),
                payer,
                "Orden de pago generada (Pendiente de transferencia/pago): " + safeDesc
        );

        return payment;
    }

    public Payment completePayment(Long actorId,
                                   Long paymentId,
                                   PaymentMethod method,
                                   Integer customAmount,
                                   String operationReference,
                                   String cardHolder,
                                   String cardNumber) {
        User actor = community.user(actorId);
        Payment payment = paymentId == null ? null : em.find(Payment.class, paymentId, LockModeType.PESSIMISTIC_WRITE);
        if (payment == null || payment.isDeleted()) {
            throw new IllegalArgumentException("Orden de pago no disponible.");
        }
        if (!payment.getPayer().getId().equals(actorId)) {
            throw new IllegalArgumentException("Solo el usuario responsable del pago puede registrar el pago.");
        }
        if (payment.getStatus() != PaymentStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Esta orden de pago ya fue procesada.");
        }
        if (customAmount != null && customAmount > 0 && customAmount <= 10_000_000) {
            payment.setAmount(customAmount);
        }

        PaymentMethod chosenMethod = method != null ? method : payment.getPaymentMethod();
        String receipt = buildValidatedReceipt(chosenMethod, operationReference, cardHolder, cardNumber);

        String prevStatus = payment.getStatus().name();
        payment.markAsPaid(chosenMethod, receipt);

        movementService.record(
                MovementEntityType.PAYMENT,
                payment.getId(),
                payment.getPayer().getUsername() + " → " + payment.getRecipient().getUsername() + " ($ " + payment.getAmount() + ")",
                prevStatus,
                payment.getStatus().name(),
                actor,
                "Pago registrado vía " + methodDisplayName(chosenMethod) + " · N° Operación/Comprobante: " + receipt
        );
        return payment;
    }

    private String buildValidatedReceipt(PaymentMethod method,
                                         String operationReference,
                                         String cardHolder,
                                         String cardNumber) {
        if (method == PaymentMethod.CARD) {
            String holder = cardHolder == null ? "" : cardHolder.trim();
            String digits = cardNumber == null ? "" : cardNumber.replaceAll("[^0-9]", "");
            if (holder.length() < 3) {
                throw new IllegalArgumentException("Ingresá el nombre del titular de la tarjeta.");
            }
            if (digits.length() < 4) {
                throw new IllegalArgumentException("Ingresá al menos los últimos 4 dígitos de la tarjeta.");
            }
            String last4 = digits.substring(digits.length() - 4);
            String cleanHolder = holder.length() > 24 ? holder.substring(0, 24) : holder;
            return "TARJETA-****" + last4 + " (" + cleanHolder + ")";
        } else {
            String ref = operationReference == null ? "" : operationReference.trim();
            if (ref.length() < 4 || ref.length() > 50) {
                throw new IllegalArgumentException("Ingresá el número de operación o comprobante de la transferencia (mínimo 4 caracteres).");
            }
            String prefix = (method == PaymentMethod.MERCADO_PAGO) ? "MP-" : "TRANSF-";
            return ref.toUpperCase(Locale.ROOT).startsWith("MP-") || ref.toUpperCase(Locale.ROOT).startsWith("TRANSF-")
                    ? ref
                    : prefix + ref;
        }
    }

    public void refundOrCancelForReference(PaymentConceptType conceptType, Long referenceId, User actor, String reason) {
        findByReference(conceptType, referenceId).ifPresent(payment -> {
            em.lock(payment, LockModeType.PESSIMISTIC_WRITE);
            String prevStatus = payment.getStatus().name();
            if (payment.getStatus() == PaymentStatus.PAID) {
                payment.markAsRefunded();
                movementService.record(
                        MovementEntityType.PAYMENT,
                        payment.getId(),
                        payment.getPayer().getUsername() + " → " + payment.getRecipient().getUsername() + " ($ " + payment.getAmount() + ")",
                        prevStatus,
                        payment.getStatus().name(),
                        actor,
                        reason + " — Pago reembolsado al pagador"
                );
            } else if (payment.getStatus() == PaymentStatus.PENDING_PAYMENT) {
                payment.markAsCancelled();
                movementService.record(
                        MovementEntityType.PAYMENT,
                        payment.getId(),
                        payment.getPayer().getUsername() + " → " + payment.getRecipient().getUsername() + " ($ " + payment.getAmount() + ")",
                        prevStatus,
                        payment.getStatus().name(),
                        actor,
                        reason + " — Orden de pago anulada"
                );
            }
        });
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByReference(PaymentConceptType conceptType, Long referenceId) {
        return em.createQuery(
                "select p from Payment p join fetch p.payer join fetch p.recipient " +
                "where p.deleted=false and p.conceptType=:ct and p.referenceId=:ref order by p.id desc", Payment.class)
                .setParameter("ct", conceptType)
                .setParameter("ref", referenceId)
                .getResultStream()
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Map<Long, Payment> mapByReferences(PaymentConceptType conceptType, Collection<Long> referenceIds) {
        Map<Long, Payment> map = new HashMap<>();
        if (referenceIds == null || referenceIds.isEmpty()) return map;
        List<Payment> rows = em.createQuery(
                "select p from Payment p join fetch p.payer join fetch p.recipient " +
                "where p.deleted=false and p.conceptType=:ct and p.referenceId in :refs", Payment.class)
                .setParameter("ct", conceptType)
                .setParameter("refs", referenceIds)
                .getResultList();
        for (Payment p : rows) {
            map.put(p.getReferenceId(), p);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<Payment> myPaymentsAsPayer(Long userId) {
        community.user(userId);
        return em.createQuery(
                "select p from Payment p join fetch p.payer join fetch p.recipient " +
                "where p.deleted=false and p.payer.id=:u order by p.createdAt desc, p.id desc", Payment.class)
                .setParameter("u", userId)
                .setMaxResults(100)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public List<Payment> myPaymentsAsRecipient(Long userId) {
        community.user(userId);
        return em.createQuery(
                "select p from Payment p join fetch p.payer join fetch p.recipient " +
                "where p.deleted=false and p.recipient.id=:u order by p.createdAt desc, p.id desc", Payment.class)
                .setParameter("u", userId)
                .setMaxResults(100)
                .getResultList();
    }

    public static int parseAmountFromPayText(String payText) {
        if (payText == null || payText.isBlank()) return 20000;
        String digits = payText.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 20000;
        try {
            int val = Integer.parseInt(digits);
            return (val >= 100 && val <= 10_000_000) ? val : 20000;
        } catch (NumberFormatException ex) {
            return 20000;
        }
    }

    public static String methodDisplayName(PaymentMethod method) {
        if (method == null) return "Transferencia Bancaria";
        return switch (method) {
            case MERCADO_PAGO -> "Mercado Pago";
            case BANK_TRANSFER -> "Transferencia Bancaria (CBU/Alias)";
            case CARD -> "Tarjeta de Crédito / Débito";
        };
    }
}
