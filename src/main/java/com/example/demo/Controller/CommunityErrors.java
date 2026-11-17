package com.example.demo.Controller;
import com.example.demo.Services.StateMovementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@ControllerAdvice
public class CommunityErrors {
    private final StateMovementService movementService;
    public CommunityErrors(StateMovementService movementService) {
        this.movementService = movementService;
    }
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})
    public String invalid(RuntimeException ex, HttpServletRequest req, RedirectAttributes a){
        logError(req, ex.getClass().getSimpleName(), ex.getMessage());
        a.addFlashAttribute("message",ex.getMessage());
        return "redirect:/home";
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public String duplicate(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest req, RedirectAttributes a){
        String msg = "Los datos ya existen o no son válidos. Revisá el formulario.";
        logError(req, "INTEGRITY_ERROR", msg + " (" + ex.getMostSpecificCause().getMessage() + ")");
        a.addFlashAttribute("message", msg);
        return "redirect:/home";
    }
    private void logError(HttpServletRequest req, String errorCode, String message) {
        try {
            HttpSession session = req != null ? req.getSession(false) : null;
            Long userId = session != null ? (Long) session.getAttribute("userId") : null;
            String label = req != null ? (req.getMethod() + " " + req.getRequestURI()) : "SYSTEM";
            movementService.recordSystemError(userId, label, errorCode, message);
        } catch (Exception ignored) {}
    }
}
