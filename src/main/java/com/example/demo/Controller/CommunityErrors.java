package com.example.demo.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@ControllerAdvice
public class CommunityErrors {
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})
    public String invalid(RuntimeException ex,RedirectAttributes a){a.addFlashAttribute("message",ex.getMessage());return "redirect:/home";}
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public String duplicate(RedirectAttributes a){a.addFlashAttribute("message","Los datos ya existen o no son válidos. Revisá el formulario.");return "redirect:/home";}
}
