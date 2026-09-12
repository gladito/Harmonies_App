package com.example.demo.Repositories;

import com.example.demo.Models.Comment;
import com.example.demo.Models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByPostAndDeletedFalseOrderByCreatedAtAsc(Post post);
}


