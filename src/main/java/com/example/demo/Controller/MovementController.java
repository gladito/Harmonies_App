package com.example.demo.Controller;

import com.example.demo.Models.MovementEntityType;
import com.example.demo.Services.StateMovementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MovementController {

    private final StateMovementService movementService;

    public MovementController(StateMovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping("/movements")
    public String listMovements(@RequestParam(required = false) MovementEntityType type,
                                @RequestParam(defaultValue = "false") boolean mine,
                                HttpSession session,
                                Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        model.addAttribute("movements", movementService.list(userId, type, mine));
        model.addAttribute("selectedType", type);
        model.addAttribute("onlyMine", mine);
        model.addAttribute("types", MovementEntityType.values());
        return "movements";
    }
}
