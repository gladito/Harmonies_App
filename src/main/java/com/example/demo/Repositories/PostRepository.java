package com.example.demo.Repositories;

import com.example.demo.Models.Post;
import com.example.demo.Models.User;
import com.example.demo.Models.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByDeletedFalse();

    Optional<Post> findByIdAndDeletedFalse(Long id);

    List<Post> findAllByAuthorAndDeletedFalse(User author);

    List<Post> findTop20ByVisibilityAndDeletedFalseOrderByCreatedAtDesc(Visibility visibility);
}

