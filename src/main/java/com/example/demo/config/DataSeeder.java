package com.example.demo.config;

import com.example.demo.Models.Comment;
import com.example.demo.Models.Group;
import com.example.demo.Models.Post;
import com.example.demo.Models.User;
import com.example.demo.Models.Visibility;
import com.example.demo.Repositories.CommentRepository;
import com.example.demo.Repositories.GroupRepository;
import com.example.demo.Repositories.PostRepository;
import com.example.demo.Repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final GroupRepository groupRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      PostRepository postRepository,
                      GroupRepository groupRepository,
                      CommentRepository commentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.groupRepository = groupRepository;
        this.commentRepository = commentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User owner = userRepository.findByUsernameAndDeletedFalse("gladito")
                .orElseGet(this::createOwnerUser);

        User collaborator = userRepository.findByUsernameAndDeletedFalse("pepito")
                .orElseGet(this::createCollaboratorUser);

        Post ownerPost = postRepository.findAllByAuthorAndDeletedFalse(owner)
                .stream()
                .filter(post -> "Nebula Jam Session".equals(post.getTitle()))
                .findFirst()
                .orElseGet(() -> createOwnerPost(owner));

        ensureCollaboratorFeedback(ownerPost, collaborator);
        ensureShowcasePost(collaborator);
     //ensureDefaultGroup(owner);
    }

    private User createOwnerUser() {
        User newUser = new User(
                "Agustin",
                "Navarro",
                LocalDate.of(1995, 5, 15),
                "gladito",
                "ejemplo1@gmail.com",
                passwordEncoder.encode("Hola1")
        );
        return userRepository.save(newUser);
    }

    private User createCollaboratorUser() {
        User newUser = new User(
                "Pepito",
                "Pepito",
                LocalDate.of(1998, 7, 8),
                "pepito",
                "pepito@gmail.com",
                passwordEncoder.encode("Hola1")
        );
        return userRepository.save(newUser);
    }

    private Post createOwnerPost(User owner) {
        Post post = new Post(
                "Nebula Jam Session",
                "Layers atmosféricos con sintetizadores analógicos y batería suave.",
                Visibility.PUBLIC,
                "https://cdn.example.com/audio/nebula-session.mp3",
                214,
                owner
        );
        return postRepository.save(post);
    }

    private void ensureCollaboratorFeedback(Post post, User collaborator) {
        boolean alreadyCommented = commentRepository.findAllByPostAndDeletedFalseOrderByCreatedAtAsc(post)
                .stream()
                .anyMatch(comment -> collaborator.equals(comment.getAuthor()));

        if (!alreadyCommented) {
            Comment comment = new Comment(
                    post,
                    collaborator,
                    "¡Suena increíble! Podríamos agregar un bajo sutil al segundo verso."
            );
            commentRepository.save(comment);
        }
    }

    private void ensureShowcasePost(User collaborator) {
        boolean hasShowcase = postRepository.findAllByAuthorAndDeletedFalse(collaborator)
                .stream()
                .anyMatch(post -> "Midnight Groove".equals(post.getTitle()));
        if (!hasShowcase) {
            Post post = new Post(
                    "Midnight Groove",
                    "Groove funk con toques de neo-soul, ideal para improvisar sobre él.",
                    Visibility.PUBLIC,
                    "https://cdn.example.com/audio/midnight-groove.mp3",
                    196,
                    collaborator
            );
            postRepository.save(post);
        }
    }

    /*
    private void ensureDefaultGroup(User owner) {
        groupRepository.findByNameAndDeletedFalse("Nebula Waves")
                .orElseGet(() -> {
                    Group group = new Group(
                            "Nebula Waves",
                            "Colectivo de productores lo-fi y synthwave enfocado en soundtracks espaciales.",
                            owner
                    );
                    return groupRepository.save(group);
                });
    }

     */
}

