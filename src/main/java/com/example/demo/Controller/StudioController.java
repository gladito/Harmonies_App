package com.example.demo.Controller;

import com.example.demo.Models.PaymentConceptType;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.StudioBooking;
import com.example.demo.Models.StudioType;
import com.example.demo.Services.PaymentService;
import com.example.demo.Services.StudioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Salas de ensayo, estudios y cabinas: listado, publicación y reservas. */
@Controller
@RequestMapping("/studios")
public class StudioController {

    private final StudioService studios;
    private final PaymentService payments;

    public StudioController(StudioService studios, PaymentService payments) {
        this.studios = studios;
        this.payments = payments;
    }

    private Long actor(HttpSession s) { return (Long) s.getAttribute("userId"); }

    static <E extends Enum<E>> E parse(Class<E> type, String raw) {
        if (raw == null || raw.isBlank()) return null;
        return Arrays.stream(type.getEnumConstants()).filter(e -> e.name().equalsIgnoreCase(raw.trim())).findFirst().orElse(null);
    }

    @GetMapping
    public String list(@RequestParam(required = false) String type, HttpSession s, Model m) {
        StudioType filter = parse(StudioType.class, type);
        List<StudioBooking> myBookings = studios.myBookings(actor(s));
        m.addAttribute("studios", studios.list(actor(s), filter));
        m.addAttribute("types", StudioType.values());
        m.addAttribute("type", filter);
        m.addAttribute("bookings", myBookings);
        m.addAttribute("bookingPayments", payments.mapByReferences(
                PaymentConceptType.STUDIO_BOOKING,
                myBookings.stream().map(StudioBooking::getId).toList()
        ));
        m.addAttribute("owned", studios.owned(actor(s)));
        return "studios";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession s, Model m) {
        var studio = studios.studio(id, actor(s));
        List<StudioBooking> upcoming = studios.upcoming(id);
        m.addAttribute("studio", studio);
        m.addAttribute("isOwner", studio.getOwner().getId().equals(actor(s)));
        m.addAttribute("upcoming", upcoming);
        m.addAttribute("bookingPayments", payments.mapByReferences(
                PaymentConceptType.STUDIO_BOOKING,
                upcoming.stream().map(StudioBooking::getId).toList()
        ));
        m.addAttribute("methods", PaymentMethod.values());
        m.addAttribute("me", actor(s));
        m.addAttribute("minDate", LocalDate.now().plusDays(1).atTime(18, 0).toString());
        m.addAttribute("maxHours", StudioService.MAX_HOURS);
        return "studio-detail";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam(required = false) String type,
                         @RequestParam(required = false) String description, @RequestParam String location,
                         @RequestParam(required = false) String amenities, @RequestParam(required = false) Integer hourlyRate,
                         HttpSession s, RedirectAttributes a) {
        try {
            var studio = studios.create(actor(s), name, parse(StudioType.class, type), description, location, amenities, hourlyRate);
            a.addFlashAttribute("success", true);
            return "redirect:/studios/" + studio.getId();
        } catch (IllegalArgumentException ex) {
            a.addFlashAttribute("message", ex.getMessage());
            return "redirect:/studios";
        }
    }

    @PostMapping("/{id}/book")
    public String book(@PathVariable Long id,
                       @RequestParam(required = false) String start,
                       @RequestParam(required = false) Integer hours,
                       @RequestParam(required = false) String paymentMethod,
                       @RequestParam(defaultValue = "false") boolean payNow,
                       HttpSession s,
                       RedirectAttributes a) {
        try {
            PaymentMethod method = parse(PaymentMethod.class, paymentMethod);
            studios.book(actor(s), id, start, hours, method != null ? method : PaymentMethod.MERCADO_PAGO, payNow);
            a.addFlashAttribute("booked", true);
        } catch (IllegalArgumentException ex) {
            a.addFlashAttribute("message", ex.getMessage());
        }
        return "redirect:/studios/" + id;
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancel(@PathVariable Long bookingId, @RequestParam(defaultValue = "/studios") String back,
                         HttpSession s, RedirectAttributes a) {
        try {
            studios.cancel(actor(s), bookingId);
            a.addFlashAttribute("cancelled", true);
        } catch (IllegalArgumentException ex) {
            a.addFlashAttribute("message", ex.getMessage());
        }
        return "redirect:" + (back.matches("/studios(/\\d+)?") ? back : "/studios");
    }

    @PostMapping("/{id}/delete")
    public String remove(@PathVariable Long id, HttpSession s, RedirectAttributes a) {
        try {
            studios.remove(actor(s), id);
        } catch (IllegalArgumentException ex) {
            a.addFlashAttribute("message", ex.getMessage());
            return "redirect:/studios/" + id;
        }
        return "redirect:/studios";
    }
}
