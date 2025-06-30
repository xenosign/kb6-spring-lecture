package org.example.kb6spring.repository.post.jpa;

import org.example.kb6spring.domain.post.Comment;
import org.example.kb6spring.domain.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentJpaRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);
}
