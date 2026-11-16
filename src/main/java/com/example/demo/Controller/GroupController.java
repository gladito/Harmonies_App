package com.example.demo.Controller;

import com.example.demo.Dtos.Groups.GroupCreateRequestDto;
import com.example.demo.Dtos.Groups.GroupJoinRequestCreateDto;
import com.example.demo.Dtos.Groups.GroupSummaryDto;
import com.example.demo.Models.MovementEntityType;
import com.example.demo.Services.AvatarStorageService;
import com.example.demo.Services.GroupJoinRequestService;
import com.example.demo.Services.GroupService;
import com.example.demo.Services.StateMovementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GroupController {

    private final GroupService groupService;
    private final GroupJoinRequestService groupJoinRequestService;
    private final AvatarStorageService avatarStorageService;
    private final StateMovementService movementService;

    public GroupController(GroupService groupService,
                           GroupJoinRequestService groupJoinRequestService,
                           AvatarStorageService avatarStorageService,
                           StateMovementService movementService) {
        this.groupService = groupService;
        this.groupJoinRequestService = groupJoinRequestService;
        this.avatarStorageService = avatarStorageService;
        this.movementService = movementService;
    }

    @GetMapping("/groups")
    public String listGroups(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        var groupSummaries = groupService.getGroupsForUser(userId);
        model.addAttribute("groups", groupSummaries);
        model.addAttribute("hasGroups", !groupSummaries.isEmpty());
        boolean ownsAtLeastOneGroup = groupSummaries.stream().anyMatch(GroupSummaryDto::isOwner);
        model.addAttribute("hasOwnedGroups", ownsAtLeastOneGroup);
        model.addAttribute("username", session.getAttribute("username"));
        return "groups";
    }

    @GetMapping("/groups/explore")
    public String exploreGroups(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        var available = groupService.getAvailableGroupsForUser(userId);
        model.addAttribute("availableGroups", available);
        model.addAttribute("hasAvailable", !available.isEmpty());
        model.addAttribute("username", session.getAttribute("username"));
        return "groups-explore";
    }

    @GetMapping("/groups/new")
    public String showCreateForm(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        if (!model.containsAttribute("groupForm")) {
            model.addAttribute("groupForm", new GroupCreateRequestDto());
        }
        return "group-create";
    }

    @GetMapping("/groups/{groupId}/edit")
    public String showEditForm(@PathVariable Long groupId,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            if (!model.containsAttribute("groupForm")) {
                model.addAttribute("groupForm", groupService.getGroupForEdit(groupId, userId));
            }
            model.addAttribute("groupId", groupId);
            return "group-edit";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("createError", ex.getMessage());
            return "redirect:/groups";
        }
    }

    @PostMapping("/groups")
    public String createGroup(@ModelAttribute("groupForm") GroupCreateRequestDto groupForm,
                              @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            if (avatarFile != null && !avatarFile.isEmpty()) {
                groupForm.setAvatarUrl(avatarStorageService.storeAvatar("band", userId, avatarFile));
            } else if (groupForm.getAvatarUrl() != null && !groupForm.getAvatarUrl().isBlank()) {
                groupForm.setAvatarUrl(avatarStorageService.normalizeAvatarUrl(groupForm.getAvatarUrl()));
            }
            groupService.createGroup(userId, groupForm);
            redirectAttributes.addFlashAttribute("createSuccess", "¡Tu banda se creó correctamente!");
            return "redirect:/groups";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("createError", ex.getMessage());
            redirectAttributes.addFlashAttribute("groupForm", groupForm);
            return "redirect:/groups/new";
        }
    }

    @GetMapping("/groups/requests")
    public String listRequests(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        var requests = groupJoinRequestService.getPendingRequestsForOwner(userId);
        var myRequests = groupJoinRequestService.getPendingRequestsByRequester(userId);
        model.addAttribute("requests", requests);
        model.addAttribute("hasRequests", !requests.isEmpty());
        model.addAttribute("myRequests", myRequests);
        model.addAttribute("hasMyRequests", !myRequests.isEmpty());
        model.addAttribute("requestMovements", movementService.list(userId, MovementEntityType.GROUP_JOIN_REQUEST, false));
        model.addAttribute("username", session.getAttribute("username"));
        return "group-requests";
    }

    @PostMapping("/groups/{groupId}/request")
    public String requestJoin(@PathVariable Long groupId,
                              @ModelAttribute("joinRequest") GroupJoinRequestCreateDto joinRequest,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        joinRequest.setGroupId(groupId);
        try {
            groupJoinRequestService.submitRequest(userId, joinRequest);
            redirectAttributes.addFlashAttribute("createSuccess", "Solicitud enviada al dueño de la banda.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("createError", ex.getMessage());
        }
        return "redirect:/groups/explore";
    }

    @PostMapping("/groups/{groupId}/leave")
    public String leaveGroup(@PathVariable Long groupId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            groupService.leaveGroup(groupId, userId);
            redirectAttributes.addFlashAttribute("createSuccess", "Saliste de la banda.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("createError", ex.getMessage());
        }
        return "redirect:/groups";
    }

    @PostMapping("/groups/{groupId}/delete")
    public String deleteGroup(@PathVariable Long groupId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            groupService.deleteGroup(groupId, userId);
            redirectAttributes.addFlashAttribute("createSuccess", "Eliminaste la banda.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("createError", ex.getMessage());
        }
        return "redirect:/groups";
    }

    @PostMapping("/groups/{groupId}/edit")
    public String editGroup(@PathVariable Long groupId,
                            @ModelAttribute("groupForm") GroupCreateRequestDto groupForm,
                            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                            @RequestParam(value = "removeAvatar", defaultValue = "false") boolean removeAvatar,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            if (removeAvatar) {
                groupForm.setAvatarUrl("");
            } else if (avatarFile != null && !avatarFile.isEmpty()) {
                groupForm.setAvatarUrl(avatarStorageService.storeAvatar("band", groupId, avatarFile));
            } else if (groupForm.getAvatarUrl() != null && !groupForm.getAvatarUrl().isBlank()) {
                groupForm.setAvatarUrl(avatarStorageService.normalizeAvatarUrl(groupForm.getAvatarUrl()));
            }
            groupService.updateGroup(groupId, userId, groupForm);
            redirectAttributes.addFlashAttribute("createSuccess", "Banda actualizada.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("createError", ex.getMessage());
            redirectAttributes.addFlashAttribute("groupForm", groupForm);
            return "redirect:/groups/" + groupId + "/edit";
        }
        return "redirect:/groups";
    }

    @PostMapping("/groups/requests/{requestId}/approve")
    public String approveRequest(@PathVariable Long requestId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            groupJoinRequestService.approveRequest(requestId, userId);
            redirectAttributes.addFlashAttribute("requestSuccess", "Solicitud aprobada.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("requestError", ex.getMessage());
        }
        return "redirect:/groups/requests";
    }

    @PostMapping("/groups/requests/{requestId}/reject")
    public String rejectRequest(@PathVariable Long requestId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            groupJoinRequestService.rejectRequest(requestId, userId);
            redirectAttributes.addFlashAttribute("requestSuccess", "Solicitud rechazada.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("requestError", ex.getMessage());
        }
        return "redirect:/groups/requests";
    }

    @PostMapping("/groups/requests/{requestId}/cancel")
    public String cancelRequest(@PathVariable Long requestId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            groupJoinRequestService.cancelRequest(requestId, userId);
            redirectAttributes.addFlashAttribute("requestSuccess", "Solicitud cancelada.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("requestError", ex.getMessage());
        }
        return "redirect:/groups/requests";
    }
}
