package com.example.demo.Controller;

import com.example.demo.Models.JobApplication;
import com.example.demo.Models.JobType;
import com.example.demo.Models.PaymentConceptType;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Services.JobService;
import com.example.demo.Services.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Bolsa de trabajo: avisos para contratar músicos y postulaciones. */
@Controller
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobs;
    private final PaymentService payments;

    public JobController(JobService jobs, PaymentService payments) {
        this.jobs = jobs;
        this.payments = payments;
    }

    private Long actor(HttpSession s) { return (Long) s.getAttribute("userId"); }

    @GetMapping
    public String list(@RequestParam(required = false) String type, HttpSession s, Model m) {
        JobType filter = StudioController.parse(JobType.class, type);
        List<JobApplication> myApps = jobs.myApplications(actor(s));
        m.addAttribute("jobs", jobs.list(actor(s), filter));
        m.addAttribute("types", JobType.values());
        m.addAttribute("type", filter);
        m.addAttribute("applications", myApps);
        m.addAttribute("appPayments", payments.mapByReferences(
                PaymentConceptType.JOB_CONTRACT,
                myApps.stream().map(JobApplication::getId).toList()
        ));
        m.addAttribute("postings", jobs.myPostings(actor(s)));
        return "jobs";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession s, Model m) {
        var job = jobs.job(id, actor(s));
        var mine = jobs.myApplication(id, actor(s)).orElse(null);
        List<JobApplication> apps = jobs.applications(id, actor(s));
        m.addAttribute("job", job);
        m.addAttribute("isAuthor", job.getAuthor().getId().equals(actor(s)));
        m.addAttribute("mine", mine);
        m.addAttribute("myPayment", mine != null
                ? payments.findByReference(PaymentConceptType.JOB_CONTRACT, mine.getId()).orElse(null)
                : null);
        m.addAttribute("applications", apps);
        m.addAttribute("appPayments", payments.mapByReferences(
                PaymentConceptType.JOB_CONTRACT,
                apps.stream().map(JobApplication::getId).toList()
        ));
        m.addAttribute("methods", PaymentMethod.values());
        return "job-detail";
    }

    @PostMapping
    public String create(@RequestParam String title, @RequestParam(required = false) String type,
                         @RequestParam(required = false) String description, @RequestParam(required = false) String instrument,
                         @RequestParam String location, @RequestParam(defaultValue = "false") boolean remote,
                         @RequestParam String pay, HttpSession s, RedirectAttributes a) {
        try {
            var job = jobs.create(actor(s), title, StudioController.parse(JobType.class, type), description, instrument, location, remote, pay);
            a.addFlashAttribute("success", true);
            return "redirect:/jobs/" + job.getId();
        } catch (IllegalArgumentException ex) {
            a.addFlashAttribute("message", ex.getMessage());
            return "redirect:/jobs";
        }
    }

    @PostMapping("/{id}/apply")
    public String apply(@PathVariable Long id, @RequestParam(required = false) String message, HttpSession s, RedirectAttributes a) {
        try {
            jobs.apply(actor(s), id, message);
            a.addFlashAttribute("applied", true);
        } catch (IllegalArgumentException ex) {
            a.addFlashAttribute("message", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/applications/{applicationId}")
    public String decide(@PathVariable Long id, @PathVariable Long applicationId, @RequestParam boolean accept,
                         HttpSession s, RedirectAttributes a) {
        try {
            jobs.decide(actor(s), applicationId, accept);
        } catch (IllegalArgumentException ex) {
            a.addFlashAttribute("message", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/applications/{applicationId}/cancel")
    public String cancelAcceptance(@PathVariable Long id, @PathVariable Long applicationId,
                                   HttpSession s, RedirectAttributes a) {
        try {
            jobs.cancelAcceptance(actor(s), applicationId);
            a.addFlashAttribute("message", "Contratación cancelada y orden de pago anulada.");
        } catch (IllegalArgumentException ex) {
            a.addFlashAttribute("message", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/open")
    public String open(@PathVariable Long id, @RequestParam boolean open, HttpSession s, RedirectAttributes a) {
        try {
            jobs.setOpen(actor(s), id, open);
        } catch (IllegalArgumentException ex) {
            a.addFlashAttribute("message", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }
}
