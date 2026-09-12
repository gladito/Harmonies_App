package com.example.demo.Controller;

import com.example.demo.Dtos.Groups.GroupSummaryDto;
import com.example.demo.Dtos.Posts.PostFeedResponseDto;
import com.example.demo.Services.GroupService;
import com.example.demo.Services.PostService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final PostService postService;
    private final GroupService groupService;

    public HomeController(PostService postService, GroupService groupService) {
        this.postService = postService;
        this.groupService = groupService;
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);

        List<PostFeedResponseDto> feed = postService.getLatestPublicPosts(12);
        model.addAttribute("feed", feed);

        List<GroupSummaryDto> groups = groupService.getGroupsForUser(userId);
        model.addAttribute("groups", groups);
        model.addAttribute("hasGroups", !groups.isEmpty());

        return "home";
    }
}

