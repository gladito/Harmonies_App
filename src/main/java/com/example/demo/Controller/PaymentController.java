package com.example.demo.Controller;

import com.example.demo.Models.Payment;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.PaymentStatus;
import com.example.demo.Services.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    private Long actor(HttpSession s) {
        return (Long) s.getAttribute("userId");
    }

    @GetMapping
    public String list(HttpSession s, Model m) {
        Long userId = actor(s);
        if (userId == null) {
            return "redirect:/login";
        }
        List<Payment> outgoing = payments.myPaymentsAsPayer(userId);
        List<Payment> incoming = payments.myPaymentsAsRecipient(userId);
        List<Payment> pendingToPay = outgoing.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING_PAYMENT)
                .toList();

        int totalPaid = outgoing.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .mapToInt(Payment::getAmount)
                .sum();
        int totalReceived = incoming.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .mapToInt(Payment::getAmount)
                .sum();
        int totalPending = pendingToPay.stream()
                .mapToInt(Payment::getAmount)
                .sum();

        m.addAttribute("outgoing", outgoing);
        m.addAttribute("incoming", incoming);
        m.addAttribute("pendingToPay", pendingToPay);
        m.addAttribute("totalPaid", totalPaid);
        m.addAttribute("totalReceived", totalReceived);
        m.addAttribute("totalPending", totalPending);
        m.addAttribute("methods", PaymentMethod.values());
        return "payments";
    }

    @PostMapping("/{id}/pay")
    public String pay(@PathVariable Long id,
                      @RequestParam(required = false) String paymentMethod,
                      @RequestParam(required = false) Integer amount,
                      @RequestParam(defaultValue = "/payments") String back,
                      HttpSession s,
                      RedirectAttributes ra) {
        Long userId = actor(s);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            PaymentMethod method = StudioController.parse(PaymentMethod.class, paymentMethod);
            Payment p = payments.completePayment(userId, id, method, amount);
            ra.addFlashAttribute("message", "¡Pago acreditado! Comprobante: " + p.getReceiptCode());
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("message", ex.getMessage());
        }
        if (back != null && (back.startsWith("/payments") || back.startsWith("/studios") || back.startsWith("/jobs"))) {
            return "redirect:" + back;
        }
        return "redirect:/payments";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         HttpSession s,
                         RedirectAttributes ra) {
        Long userId = actor(s);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            payments.cancelPendingOrder(userId, id);
            ra.addFlashAttribute("message", "Orden de contratación cancelada.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("message", ex.getMessage());
        }
        return "redirect:/payments";
    }
}
