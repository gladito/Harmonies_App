package com.example.demo.Controller;

import com.example.demo.Models.JobType;
import com.example.demo.Services.JobService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Bolsa de trabajo: avisos para contratar músicos y postulaciones. */
@Controller
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobs;

    public JobController(JobService jobs) {
        this.jobs = jobs;
    }

    private Long actor(HttpSession s) { return (Long) s.getAttribute("userId"); }

    @GetMapping
    public String list(@RequestParam(required = false) String type, HttpSession s, Model m) {
        JobType filter = StudioController.parse(JobType.class, type);
        m.addAttribute("jobs", jobs.list(actor(s), filter));
        m.addAttribute("types", JobType.values());
        m.addAttribute("type", filter);
        m.addAttribute("applications", jobs.myApplications(actor(s)));
        m.addAttribute("postings", jobs.myPostings(actor(s)));
        return "jobs";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession s, Model m) {
        var job = jobs.job(id, actor(s));
        m.addAttribute("job", job);
        m.addAttribute("isAuthor", job.getAuthor().getId().equals(actor(s)));
        m.addAttribute("mine", jobs.myApplication(id, actor(s)).orElse(null));
        m.addAttribute("applications", jobs.applications(id, actor(s)));
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
