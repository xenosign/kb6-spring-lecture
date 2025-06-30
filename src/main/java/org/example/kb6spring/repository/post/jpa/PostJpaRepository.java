package org.example.kb6spring.repository.post.jpa;

import org.example.kb6spring.domain.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostJpaRepository extends JpaRepository<Post, Long> {}
