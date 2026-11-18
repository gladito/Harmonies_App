package com.example.demo.Models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "concept_type", nullable = false, length = 32)
    private PaymentConceptType conceptType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(nullable = false, length = 180)
    private String description;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status = PaymentStatus.PENDING_PAYMENT;

    @Column(name = "receipt_code", length = 64)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_user_id", nullable = false)
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public Payment() {
    }

    public Payment(PaymentConceptType conceptType,
                   Long referenceId,
                   String description,
                   Integer amount,
                   PaymentMethod paymentMethod,
                   User payer,
                   User recipient) {
        this.conceptType = Objects.requireNonNull(conceptType);
        this.referenceId = Objects.requireNonNull(referenceId);
        this.description = Objects.requireNonNull(description);
        this.amount = Objects.requireNonNull(amount);
        this.paymentMethod = Objects.requireNonNull(paymentMethod);
        this.payer = Objects.requireNonNull(payer);
        this.recipient = Objects.requireNonNull(recipient);
        this.status = PaymentStatus.PENDING_PAYMENT;
    }

    public void markAsPaid(PaymentMethod method, String receiptCode) {
        if (method != null) {
            this.paymentMethod = method;
        }
        this.status = PaymentStatus.PAID;
        this.receiptCode = receiptCode;
        this.paidAt = LocalDateTime.now();
    }

    public void resetToPending() {
        this.status = PaymentStatus.PENDING_PAYMENT;
        this.receiptCode = null;
        this.paidAt = null;
    }

    public void markAsRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }

    public void markAsCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }

    public PaymentConceptType getConceptType() {
        return conceptType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public User getPayer() {
        return payer;
    }

    public User getRecipient() {
        return recipient;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
