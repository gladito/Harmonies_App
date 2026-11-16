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

    @org.springframework.beans.factory.annotation.Autowired private CommunityService community;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PostFeedResponseDto> getLatestPublicPosts(int limit, Long viewer) {
        List<Post> posts = postRepository.findTop20ByVisibilityAndDeletedFalseOrderByCreatedAtDesc(Visibility.PUBLIC);
        return posts.stream().filter(p -> community.canSee(p,viewer))
                .limit(limit)
                .map(this::toFeedDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PostDetailDto> getPostDetail(Long postId, Long viewer) {
        return postRepository.findById(postId)
                .filter(post -> !post.isDeleted())
                .filter(p -> community.canSee(p,viewer)).map(p -> toDetailDto(p,viewer));
    }

    @Transactional(readOnly = true)
    public Optional<PostUpdateRequestDto> getPostForEdit(Long postId, Long authorId) {
        return postRepository.findById(postId)
                .filter(post -> !post.isDeleted())
                .filter(post -> post.getAuthor() != null && post.getAuthor().getId().equals(authorId))
                .filter(post -> post.getBand()==null || community.permitted(post.getBand(),authorId,true))
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

        validatePost(request.getTitle(), request.getDescription(), request.getAudioUrl(), request.getDurationInSeconds(), request.getVisibility());
        Post post = new Post(
                request.getTitle(),
                request.getDescription(),
                request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC,
                request.getAudioUrl(),
                request.getDurationInSeconds(),
                author
        );

        applyTags(post,request.getTags());
        post.setProfile(author.getProfile());
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

        validatePost(request.getTitle(), request.getDescription(), request.getAudioUrl(), request.getDurationInSeconds(), request.getVisibility());
        applyTags(post,request.getTags());
        post.setTitle(request.getTitle());
        if(post.getBand()!=null) community.publishingBand(post.getBand().getId(),authorId);
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

        if(post.getBand()!=null) community.publishingBand(post.getBand().getId(),authorId);
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
        PostFeedResponseDto dto = new PostFeedResponseDto(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                authorUsername,
                post.getCreatedAt()
        );
        dto.setTags(post.getTags().stream().map(com.example.demo.Models.Tag::getValue).sorted().toList());
        dto.setAuthorId(post.getAuthor().getId());dto.setAudioUrl(post.getAudioUrl());
        dto.setAuthorAvatarUrl(post.getAuthor().getProfile() != null ? post.getAuthor().getProfile().getAvatarUrl() : null);
        dto.setBandName(post.getBand()==null?null:post.getBand().getName());
        dto.setBandAvatarUrl(post.getBand()==null?null:post.getBand().getAvatarUrl());
        return dto;
    }

    private PostDetailDto toDetailDto(Post post, Long viewer) {
        PostDetailDto dto = new PostDetailDto();
        dto.setId(post.getId());
        dto.setAuthorId(post.getAuthor() != null ? post.getAuthor().getId() : null);
        dto.setTitle(post.getTitle());
        dto.setDescription(post.getDescription());
        dto.setAudioUrl(post.getAudioUrl());
        dto.setDurationInSeconds(post.getDurationInSeconds());
        dto.setAuthorUsername(post.getAuthor() != null ? post.getAuthor().getUsername() : "Usuario desconocido");
        dto.setAuthorAvatarUrl(post.getAuthor() != null && post.getAuthor().getProfile() != null ? post.getAuthor().getProfile().getAvatarUrl() : null);
        dto.setCreatedAt(post.getCreatedAt());
        dto.setComments(post.getComments().stream()
                .filter(comment -> !comment.isDeleted() && community.canSeeUser(viewer,comment.getAuthor().getId()))
                .toList());
        return dto;
    }

    @Transactional
    public Post createBandPost(Long group,Long actor,PostCreateRequestDto request) {
        var band=community.publishingBand(group,actor);
        Post p=createPost(actor,request);p.setBand(band);return p;
    }
    public record FeedPage(java.util.List<PostFeedResponseDto> posts,int page,boolean hasMore,String tag) {}
    @Transactional(readOnly=true)
    public FeedPage feed(Long viewer,int requestedPage,String tag) {
        community.user(viewer);
        int page=Math.max(0,Math.min(requestedPage,10000));
        String filter=tag==null?"":tag.trim().toLowerCase(java.util.Locale.ROOT);
        if(filter.length()>50)throw new IllegalArgumentException("Etiqueta demasiado larga");
        String hql="select p from Post p join fetch p.author a left join fetch p.band b where p.deleted=false and a.deleted=false and a.enabled=true and p.visibility=:visibility and (b is null or b.deleted=false) and not exists (select block.id from UserBlock block where block.owner=a and block.target.id=:viewer) ";
        if(!filter.isEmpty())hql+="and exists (select t.id from Post tagged join tagged.tags t where tagged=p and t.value=:tag) ";
        var query=em.createQuery(hql+"order by p.createdAt desc,p.id desc",Post.class).setParameter("visibility",Visibility.PUBLIC).setParameter("viewer",viewer);
        if(!filter.isEmpty())query.setParameter("tag",filter);
        var rows=query.setFirstResult(page*12).setMaxResults(13).getResultList();
        return new FeedPage(rows.stream().limit(12).map(this::toFeedDto).toList(),page,rows.size()>12,filter);
    }
    private void applyTags(Post post,String raw) {
        var values=new java.util.LinkedHashSet<String>();
        if(raw!=null && raw.length()>254)throw new IllegalArgumentException("Usá hasta cinco etiquetas cortas");
        if(raw!=null)for(String part:raw.split(",")){
            String value=part.trim().toLowerCase(java.util.Locale.ROOT);
            if(value.isEmpty())continue;
            if(value.length()>50 || value.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Etiqueta inválida");
            values.add(value);
        }
        if(values.size()>5)throw new IllegalArgumentException("Usá hasta cinco etiquetas");
        post.getTags().clear();
        for(String value:values){
            var found=em.createQuery("from Tag t where t.value=:value",com.example.demo.Models.Tag.class).setParameter("value",value).getResultList();
            var tag=found.isEmpty()?new com.example.demo.Models.Tag(value):found.get(0);
            if(found.isEmpty())em.persist(tag);
            post.addTag(tag);
        }
    }
    private void validatePost(String title,String description,String url,Integer duration,Visibility visibility) {
        if(title.length()>120 || description!=null && description.length()>500 || duration!=null && duration<0)
            throw new IllegalArgumentException("Revisá título, descripción y duración");
        try { var uri=java.net.URI.create(url); if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || url.length()>255)throw new IllegalArgumentException(); }
        catch(RuntimeException ex){throw new IllegalArgumentException("Usá un enlace HTTPS válido de hasta 255 caracteres");}
        if(visibility==Visibility.FOLLOWERS)throw new IllegalArgumentException("Por ahora elegí público o privado");
    }
    private PostUpdateRequestDto toUpdateDto(Post post) {
        PostUpdateRequestDto dto = new PostUpdateRequestDto();
        dto.setId(post.getId());
        dto.setTags(post.getTags().stream().map(com.example.demo.Models.Tag::getValue).sorted().collect(java.util.stream.Collectors.joining(", ")));
        dto.setTitle(post.getTitle());
        dto.setDescription(post.getDescription());
        dto.setAudioUrl(post.getAudioUrl());
        dto.setDurationInSeconds(post.getDurationInSeconds());
        dto.setVisibility(post.getVisibility());
        return dto;
    }
}

