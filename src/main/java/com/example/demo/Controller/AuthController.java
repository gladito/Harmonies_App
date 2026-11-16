package com.example.demo.Controller;

import com.example.demo.Dtos.Auth.AuthLoginRequestDto;
import com.example.demo.Dtos.Auth.AuthRegisterRequestDto;
import com.example.demo.Dtos.Auth.PasswordChangeRequestDto;
import com.example.demo.Dtos.Auth.ProfileUpdateRequestDto;
import com.example.demo.Models.Group;
import com.example.demo.Models.User;
import com.example.demo.Services.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    @PersistenceContext
    private EntityManager em;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @ModelAttribute("loginForm")
    public AuthLoginRequestDto loginForm() {
        return new AuthLoginRequestDto();
    }

    @ModelAttribute("registerForm")
    public AuthRegisterRequestDto registerForm() {
        return new AuthRegisterRequestDto();
    }

    @ModelAttribute("profileForm")
    public ProfileUpdateRequestDto profileForm() {
        return new ProfileUpdateRequestDto();
    }

    @ModelAttribute("passwordForm")
    public PasswordChangeRequestDto passwordForm() {
        return new PasswordChangeRequestDto();
    }

    @GetMapping("/")
    public String showIndex(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/home";
        }
        model.addAttribute("musicianCount", em.createQuery("select count(u) from User u where u.deleted=false and u.enabled=true", Long.class).getSingleResult());
        model.addAttribute("bandCount", em.createQuery("select count(g) from Group g where g.deleted=false", Long.class).getSingleResult());
        model.addAttribute("postCount", em.createQuery("select count(p) from Post p where p.deleted=false", Long.class).getSingleResult());
        model.addAttribute("studioCount", em.createQuery("select count(s) from Studio s where s.deleted=false", Long.class).getSingleResult());
        model.addAttribute("jobCount", em.createQuery("select count(j) from JobPosting j where j.deleted=false and j.open=true", Long.class).getSingleResult());
        model.addAttribute("featuredBands", em.createQuery("select g from Group g where g.deleted=false order by g.id asc", Group.class).setMaxResults(5).getResultList());
        return "index";
    }

    @GetMapping("/login")
    public String showLogin(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/home";
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegister(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/home";
        }
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(@ModelAttribute("registerForm") AuthRegisterRequestDto registerForm,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = authService.register(registerForm);
            redirectAttributes.addFlashAttribute("registerSuccess", "Cuenta creada para " + user.getUsername() + ". Ahora puedes iniciar sesión.");
            return "redirect:/login";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("registerError", ex.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/account")
    public String showProfile(HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        authService.getUserById(userId).ifPresent(user -> {
            session.setAttribute("username", user.getUsername());
            model.addAttribute("avatarUrl", user.getProfile() != null ? user.getProfile().getAvatarUrl() : null);
        });
        return "profile";
    }

    @PostMapping("/profile/username")
    public String updateUsername(@ModelAttribute("profileForm") ProfileUpdateRequestDto profileForm,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            User updated = authService.updateProfile(userId, profileForm.getUsername());
            session.setAttribute("username", updated.getUsername());
            redirectAttributes.addFlashAttribute("profileSuccess", "Nombre de usuario actualizado.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("profileError", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@ModelAttribute("passwordForm") PasswordChangeRequestDto passwordForm,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            authService.updatePassword(userId, passwordForm.getCurrentPassword(), passwordForm.getNewPassword());
            redirectAttributes.addFlashAttribute("passwordSuccess", "Contraseña actualizada correctamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("passwordError", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
