package com.example.demo.Controller;

import com.example.demo.Services.*;
import com.example.demo.Dtos.Posts.PostCreateRequestDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Controller
public class CommunityController {
    private final CommunityService c;
    private final PostService posts;
    private final AvatarStorageService avatars;

    public CommunityController(CommunityService c, PostService posts, AvatarStorageService avatars) {
        this.c = c;
        this.posts = posts;
        this.avatars = avatars;
    }

    private Long actor(HttpSession s) {
        return (Long) s.getAttribute("userId");
    }

    @GetMapping("/avatars/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        Path file = avatars.resolveStoredAvatar(filename);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(avatars.mediaTypeFor(filename))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(new FileSystemResource(file));
    }

    @GetMapping("/people")
    public String people(HttpSession s, Model m) {
        Long me = actor(s);
        m.addAttribute("people", c.people(me));
        m.addAttribute("blocked", c.blockedUsers(me));
        m.addAttribute("followedIds", c.followedIds(me));
        m.addAttribute("followingUsers", c.followingList(me, me));
        m.addAttribute("me", me);
        return "people";
    }

    @GetMapping("/profile")
    public String mine(HttpSession s) {
        return "redirect:/people/" + actor(s);
    }

    @GetMapping("/people/{id}")
    public String profile(@PathVariable Long id, HttpSession s, Model m) {
        Long me = actor(s);
        m.addAttribute("person", c.profile(id, me));
        m.addAttribute("rating", c.rating(id, me));
        m.addAttribute("follow", c.followSummary(id, me));
        m.addAttribute("followers", c.followersList(id, me));
        m.addAttribute("followingList", c.followingList(id, me));
        m.addAttribute("posts", c.profilePosts(id, me));
        m.addAttribute("mine", id.equals(me));
        m.addAttribute("blockedByMe", c.blocked(me, id));
        return "public-profile";
    }

    @PostMapping("/people/{id}/follow")
    public String follow(@PathVariable Long id,
                         @RequestParam boolean following,
                         @RequestParam(defaultValue = "") String back,
                         HttpSession s) {
        c.follow(actor(s), id, following);
        if ("people".equalsIgnoreCase(back)) {
            return "redirect:/people";
        }
        return "redirect:/people/" + id;
    }

    @PostMapping("/profile/bio")
    public String bio(@RequestParam String bio, HttpSession s) {
        c.bio(actor(s), bio);
        return "redirect:/profile";
    }

    @PostMapping("/profile/avatar")
    public String updateAvatar(@RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                               @RequestParam(value = "avatarUrl", required = false) String avatarUrl,
                               @RequestParam(value = "remove", defaultValue = "false") boolean remove,
                               @RequestParam(value = "from", defaultValue = "profile") String from,
                               HttpSession s,
                               RedirectAttributes ra) {
        Long userId = actor(s);
        if (userId == null) {
            return "redirect:/login";
        }
        String targetRedirect = "account".equalsIgnoreCase(from) ? "redirect:/account" : "redirect:/people/" + userId;
        try {
            if (remove) {
                c.avatar(userId, null);
                ra.addFlashAttribute("message", "Foto de perfil eliminada.");
                ra.addFlashAttribute("profileSuccess", "Foto de perfil eliminada.");
            } else if (avatarFile != null && !avatarFile.isEmpty()) {
                String storedUrl = avatars.storeAvatar("user", userId, avatarFile);
                c.avatar(userId, storedUrl);
                ra.addFlashAttribute("message", "Foto de perfil actualizada.");
                ra.addFlashAttribute("profileSuccess", "Foto de perfil actualizada.");
            } else {
                String normalized = avatars.normalizeAvatarUrl(avatarUrl);
                c.avatar(userId, normalized);
                ra.addFlashAttribute("message", normalized == null ? "Foto de perfil eliminada." : "Foto de perfil actualizada.");
                ra.addFlashAttribute("profileSuccess", normalized == null ? "Foto de perfil eliminada." : "Foto de perfil actualizada.");
            }
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("message", ex.getMessage());
            ra.addFlashAttribute("profileError", ex.getMessage());
        }
        return targetRedirect;
    }

    @PostMapping("/people/{id}/rating")
    public String rate(@PathVariable Long id, @RequestParam int score, HttpSession s) {
        c.rate(actor(s), id, score);
        return "redirect:/people/" + id;
    }

    @PostMapping("/people/{id}/block")
    public String block(@PathVariable Long id, @RequestParam boolean blocked, HttpSession s) {
        c.block(actor(s), id, blocked);
        return "redirect:/people";
    }

    @PostMapping("/posts/{id}/reaction")
    public String react(@PathVariable Long id, @RequestParam int value, HttpSession s) {
        c.react(actor(s), id, value);
        return "redirect:/home";
    }

    @GetMapping("/bands/{id}")
    public String band(@PathVariable Long id, HttpSession s, Model m) {
        var g = c.band(id);
        m.addAttribute("band", g);
        m.addAttribute("members", c.members(id, actor(s)));
        m.addAttribute("isOwner", g.getOwner().getId().equals(actor(s)));
        m.addAttribute("isMember", g.hasMember(c.user(actor(s))));
        m.addAttribute("canPublish", c.permitted(g, actor(s), true));
        m.addAttribute("canApprove", c.permitted(g, actor(s), false));
        m.addAttribute("posts", c.bandPosts(id, actor(s)));
        return "band-profile";
    }

    @PostMapping("/bands/{id}/avatar")
    public String updateBandAvatar(@PathVariable Long id,
                                   @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                   @RequestParam(value = "avatarUrl", required = false) String avatarUrl,
                                   @RequestParam(value = "remove", defaultValue = "false") boolean remove,
                                   HttpSession s,
                                   RedirectAttributes ra) {
        Long userId = actor(s);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            if (remove) {
                c.bandAvatar(id, userId, null);
                ra.addFlashAttribute("message", "Foto de la banda eliminada.");
            } else if (avatarFile != null && !avatarFile.isEmpty()) {
                String storedUrl = avatars.storeAvatar("band", id, avatarFile);
                c.bandAvatar(id, userId, storedUrl);
                ra.addFlashAttribute("message", "Foto de la banda actualizada.");
            } else {
                String normalized = avatars.normalizeAvatarUrl(avatarUrl);
                c.bandAvatar(id, userId, normalized);
                ra.addFlashAttribute("message", normalized == null ? "Foto de la banda eliminada." : "Foto de la banda actualizada.");
            }
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("message", ex.getMessage());
        }
        return "redirect:/bands/" + id;
    }

    @PostMapping("/bands/{id}/members/{userId}")
    public String member(@PathVariable Long id, @PathVariable Long userId, @RequestParam String instrument,
                         @RequestParam(defaultValue = "false") boolean publish, @RequestParam(defaultValue = "false") boolean approve, HttpSession s) {
        c.member(id, actor(s), userId, instrument, publish, approve);
        return "redirect:/bands/" + id;
    }

    @PostMapping("/bands/{id}/transfer")
    public String transfer(@PathVariable Long id, @RequestParam Long target, HttpSession s) {
        c.transfer(id, actor(s), target);
        return "redirect:/bands/" + id;
    }

    @PostMapping("/bands/{id}/posts")
    public String post(@PathVariable Long id, @ModelAttribute PostCreateRequestDto request, HttpSession s) {
        var p = posts.createBandPost(id, actor(s), request);
        return "redirect:/posts/" + p.getId();
    }
}
