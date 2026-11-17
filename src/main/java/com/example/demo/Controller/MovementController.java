package com.example.demo.Controller;

import com.example.demo.Models.MovementEntityType;
import com.example.demo.Services.CommunityService;
import com.example.demo.Services.StateMovementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MovementController {

    private final StateMovementService movementService;
    private final CommunityService community;

    public MovementController(StateMovementService movementService, CommunityService community) {
        this.movementService = movementService;
        this.community = community;
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
        if (!community.isAdmin(userId)) {
            throw new IllegalArgumentException("Acceso restringido: solo un administrador puede consultar el registro de trazabilidad y errores.");
        }
        model.addAttribute("movements", movementService.list(userId, type, mine));
        model.addAttribute("selectedType", type);
        model.addAttribute("onlyMine", mine);
        model.addAttribute("types", MovementEntityType.values());
        return "movements";
    }
}
