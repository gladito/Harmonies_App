package com.example.demo.Services;

import com.example.demo.Dtos.Comments.CommentCreateRequestDto;
import com.example.demo.Models.Comment;
import com.example.demo.Models.Post;
import com.example.demo.Models.User;
import com.example.demo.Repositories.CommentRepository;
import com.example.demo.Repositories.PostRepository;
import com.example.demo.Repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Comment createComment(Long authorId, CommentCreateRequestDto request) {
        if (authorId == null) {
            throw new IllegalArgumentException("Se requiere un usuario para comentar.");
        }
        if (request == null || request.getPostId() == null) {
            throw new IllegalArgumentException("Datos incompletos para comentar.");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("El comentario no puede estar vacío.");
        }
        if (request.getContent().length() > 280) {
            throw new IllegalArgumentException("El comentario no puede superar los 280 caracteres.");
        }

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el post."));

        User author = userRepository.findByIdAndDeletedFalse(authorId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));

        Comment comment = new Comment(post, author, request.getContent().trim());
        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsForPost(Post post) {
        return commentRepository.findAllByPostAndDeletedFalseOrderByCreatedAtAsc(post);
    }

    @Transactional
    public void deleteCommentAsPostOwner(Long postId, Long commentId, Long ownerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos la publicación."));

        if (post.isDeleted()) {
            throw new IllegalStateException("La publicación ya fue eliminada.");
        }
        if (post.getAuthor() == null || !post.getAuthor().getId().equals(ownerId)) {
            throw new IllegalStateException("No tienes permisos para moderar esta publicación.");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos el comentario."));

        if (!comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("El comentario no pertenece a la publicación.");
        }

        if (!comment.isDeleted()) {
            comment.markAsDeleted();
            commentRepository.save(comment);
        }
    }
}

