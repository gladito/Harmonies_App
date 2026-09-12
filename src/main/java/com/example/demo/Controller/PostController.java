package com.example.demo.Controller;

import com.example.demo.Dtos.Comments.CommentCreateRequestDto;
import com.example.demo.Dtos.Posts.PostCreateRequestDto;
import com.example.demo.Dtos.Posts.PostUpdateRequestDto;
import com.example.demo.Services.CommentService;
import com.example.demo.Services.PostService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        if (!model.containsAttribute("postForm")) {
            model.addAttribute("postForm", new PostCreateRequestDto());
        }
        model.addAttribute("mode", "create");
        return "post-create";
    }

    @PostMapping
    public String createPost(@ModelAttribute("postForm") PostCreateRequestDto postForm,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            var post = postService.createPost(userId, postForm);
            redirectAttributes.addFlashAttribute("createSuccess", "Tu publicación fue compartida con la comunidad.");
            return "redirect:/posts/" + post.getId();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("createError", ex.getMessage());
            redirectAttributes.addFlashAttribute("postForm", postForm);
            return "redirect:/posts/new";
        }
    }

    @GetMapping("/{postId}/edit")
    public String showEditForm(@PathVariable Long postId,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        return postService.getPostForEdit(postId, userId)
                .map(form -> {
                    if (!model.containsAttribute("postForm")) {
                        model.addAttribute("postForm", form);
                    }
                    model.addAttribute("postId", postId);
                    return "post-edit";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("feedError", "No tienes permisos para editar esa publicación.");
                    return "redirect:/home";
                });
    }

    @PostMapping("/{postId}/edit")
    public String updatePost(@PathVariable Long postId,
                             @ModelAttribute("postForm") PostUpdateRequestDto postForm,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            postService.updatePost(postId, userId, postForm);
            redirectAttributes.addFlashAttribute("postSuccess", "Tu publicación se actualizó correctamente.");
            return "redirect:/posts/" + postId;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("updateError", ex.getMessage());
            redirectAttributes.addFlashAttribute("postForm", postForm);
            return "redirect:/posts/" + postId + "/edit";
        }
    }

    @PostMapping("/{postId}/delete")
    public String deletePost(@PathVariable Long postId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            postService.deletePost(postId, userId);
            redirectAttributes.addFlashAttribute("postSuccess", "La publicación se eliminó.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("postError", ex.getMessage());
        }
        return "redirect:/home";
    }

    @GetMapping("/{postId}")
    public String viewPost(@PathVariable Long postId,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        return postService.getPostDetail(postId)
                .map(detail -> {
                    model.addAttribute("post", detail);
                    if (!model.containsAttribute("commentForm")) {
                        model.addAttribute("commentForm", new CommentCreateRequestDto());
                    }
                    Long userId = (Long) session.getAttribute("userId");
                    boolean isOwner = detail.getAuthorId() != null && detail.getAuthorId().equals(userId);
                    model.addAttribute("isOwner", isOwner);
                    return "post-detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("feedError", "No encontramos la publicación.");
                    return "redirect:/home";
                });
    }

    @PostMapping("/{postId}/comments")
    public String addComment(@PathVariable Long postId,
                             @ModelAttribute("commentForm") CommentCreateRequestDto commentForm,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        commentForm.setPostId(postId);
        try {
            commentService.createComment(userId, commentForm);
            redirectAttributes.addFlashAttribute("commentSuccess", "Comentario publicado.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("commentError", ex.getMessage());
            redirectAttributes.addFlashAttribute("commentForm", commentForm);
        }
        return "redirect:/posts/" + postId;
    }

    @PostMapping("/{postId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long postId,
                                @PathVariable Long commentId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            commentService.deleteCommentAsPostOwner(postId, commentId, userId);
            redirectAttributes.addFlashAttribute("commentSuccess", "Comentario eliminado.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("commentError", ex.getMessage());
        }
        return "redirect:/posts/" + postId;
    }
}