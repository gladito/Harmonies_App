package com.example.demo.Controller;

import com.example.demo.Dtos.Auth.AuthLoginRequestDto;
import com.example.demo.Dtos.Auth.AuthRegisterRequestDto;
import com.example.demo.Dtos.Auth.PasswordChangeRequestDto;
import com.example.demo.Dtos.Auth.ProfileUpdateRequestDto;
import com.example.demo.Models.User;
import com.example.demo.Services.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

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

    @GetMapping({"/", "/login"})
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

    @PostMapping("/login")
    public String processLogin(@ModelAttribute("loginForm") AuthLoginRequestDto loginForm,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        try {
            User user = authService.authenticate(loginForm);
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            return "redirect:/home";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("loginError", ex.getMessage());
            return "redirect:/login";
        }
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

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String showProfile(HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        authService.getUserById(userId).ifPresent(user -> {
            session.setAttribute("username", user.getUsername());
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

