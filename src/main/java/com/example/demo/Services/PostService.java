package com.example.demo.Services;

import com.example.demo.Dtos.Posts.PostCreateRequestDto;
import com.example.demo.Dtos.Posts.PostDetailDto;
import com.example.demo.Dtos.Posts.PostFeedResponseDto;
import com.example.demo.Dtos.Posts.PostUpdateRequestDto;
import com.example.demo.Models.Post;
import com.example.demo.Models.User;
import com.example.demo.Models.Visibility;
import com.example.demo.Repositories.PostRepository;
import com.example.demo.Repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PostFeedResponseDto> getLatestPublicPosts(int limit) {
        List<Post> posts = postRepository.findTop20ByVisibilityAndDeletedFalseOrderByCreatedAtDesc(Visibility.PUBLIC);
        return posts.stream()
                .limit(limit)
                .map(this::toFeedDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PostDetailDto> getPostDetail(Long postId) {
        return postRepository.findById(postId)
                .filter(post -> !post.isDeleted())
                .map(this::toDetailDto);
    }

    @Transactional(readOnly = true)
    public Optional<PostUpdateRequestDto> getPostForEdit(Long postId, Long authorId) {
        return postRepository.findById(postId)
                .filter(post -> !post.isDeleted())
                .filter(post -> post.getAuthor() != null && post.getAuthor().getId().equals(authorId))
                .map(this::toUpdateDto);
    }

    @Transactional
    public Post createPost(Long authorId, PostCreateRequestDto request) {
        if (authorId == null) {
            throw new IllegalArgumentException("No se proporcionó usuario autor.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Los datos del post son obligatorios.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio.");
        }
        if (request.getAudioUrl() == null || request.getAudioUrl().isBlank()) {
            throw new IllegalArgumentException("La URL del audio es obligatoria.");
        }

        User author = userRepository.findByIdAndDeletedFalse(authorId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el autor."));

        Post post = new Post(
                request.getTitle(),
                request.getDescription(),
                request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC,
                request.getAudioUrl(),
                request.getDurationInSeconds(),
                author
        );

        return postRepository.save(post);
    }

    @Transactional
    public Post updatePost(Long postId, Long authorId, PostUpdateRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del post son obligatorios.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos la publicación."));

        if (post.isDeleted()) {
            throw new IllegalStateException("La publicación ya fue eliminada.");
        }

        if (post.getAuthor() == null || !post.getAuthor().getId().equals(authorId)) {
            throw new IllegalStateException("No tienes permisos para editar esta publicación.");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio.");
        }
        if (request.getAudioUrl() == null || request.getAudioUrl().isBlank()) {
            throw new IllegalArgumentException("La URL del audio es obligatoria.");
        }

        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setAudioUrl(request.getAudioUrl());
        post.setDurationInSeconds(request.getDurationInSeconds());
        post.setVisibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC);

        return postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long postId, Long authorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos la publicación."));

        if (post.isDeleted()) {
            return;
        }

        if (post.getAuthor() == null || !post.getAuthor().getId().equals(authorId)) {
            throw new IllegalStateException("No tienes permisos para eliminar esta publicación.");
        }

        post.markAsDeleted();
        post.getComments().forEach(comment -> {
            if (!comment.isDeleted()) {
                comment.markAsDeleted();
            }
        });
        postRepository.save(post);
    }

    private PostFeedResponseDto toFeedDto(Post post) {
        String authorUsername = post.getAuthor() != null ? post.getAuthor().getUsername() : "Usuario desconocido";
        return new PostFeedResponseDto(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                authorUsername,
                post.getCreatedAt()
        );
    }

    private PostDetailDto toDetailDto(Post post) {
        PostDetailDto dto = new PostDetailDto();
        dto.setId(post.getId());
        dto.setAuthorId(post.getAuthor() != null ? post.getAuthor().getId() : null);
        dto.setTitle(post.getTitle());
        dto.setDescription(post.getDescription());
        dto.setAudioUrl(post.getAudioUrl());
        dto.setDurationInSeconds(post.getDurationInSeconds());
        dto.setAuthorUsername(post.getAuthor() != null ? post.getAuthor().getUsername() : "Usuario desconocido");
        dto.setCreatedAt(post.getCreatedAt());
        dto.setComments(post.getComments().stream()
                .filter(comment -> !comment.isDeleted())
                .toList());
        return dto;
    }

    private PostUpdateRequestDto toUpdateDto(Post post) {
        PostUpdateRequestDto dto = new PostUpdateRequestDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setDescription(post.getDescription());
        dto.setAudioUrl(post.getAudioUrl());
        dto.setDurationInSeconds(post.getDurationInSeconds());
        dto.setVisibility(post.getVisibility());
        return dto;
    }
}

