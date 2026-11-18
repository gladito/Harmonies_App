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

    @org.springframework.beans.factory.annotation.Autowired private com.example.demo.Services.CommunityService community;
    private final PostService postService;
    private final GroupService groupService;

    public HomeController(PostService postService, GroupService groupService) {
        this.postService = postService;
        this.groupService = groupService;
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model,
                       @org.springframework.web.bind.annotation.RequestParam(defaultValue="0") int page,
                       @org.springframework.web.bind.annotation.RequestParam(defaultValue="") String tag,
                       @org.springframework.web.bind.annotation.RequestParam(defaultValue="false") boolean following) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);

        var result=postService.feed(userId,page,tag,following);
        List<PostFeedResponseDto> feed = result.posts();
        model.addAttribute("page",result.page());model.addAttribute("hasMore",result.hasMore());model.addAttribute("tag",result.tag());
        model.addAttribute("onlyFollowing",following);
        model.addAttribute("feed", feed);
        model.addAttribute("reactions",community.reactions(feed.stream().map(PostFeedResponseDto::getId).toList(),userId));

        List<GroupSummaryDto> groups = groupService.getGroupsForUser(userId);
        model.addAttribute("groups", groups);
        model.addAttribute("hasGroups", !groups.isEmpty());

        return "home";
    }
}

