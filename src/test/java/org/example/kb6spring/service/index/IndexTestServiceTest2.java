package org.example.kb6spring.service.index;

import org.example.kb6spring.config.JpaConfig;
import org.example.kb6spring.config.RootConfig;
import org.example.kb6spring.domain.post.Comment;
import org.example.kb6spring.domain.post.Post;
import org.example.kb6spring.repository.post.jpa.CommentJpaRepository;
import org.example.kb6spring.repository.post.jpa.PostJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RootConfig.class, JpaConfig.class })
@TestPropertySource(properties = "spring.profiles.active=test")
public class IndexTestServiceTest2 {

    @Autowired
    private PostJpaRepository postRepository;

    @Autowired
    private CommentJpaRepository commentRepository;

    private static final long COMMENT_COUNT = 10000;

    private Post post;

    @BeforeEach
    @Transactional
    public void setup() {
        // 테스트용 게시글 1개 저장
        post = new Post();
        post.setTitle("성능 테스트용 게시글");
        postRepository.save(post);

        // 댓글 10,000개 미리 삽입
        for (int i = 0; i < COMMENT_COUNT; i++) {
            Comment comment = new Comment();
            comment.setPost(post);
            comment.setCommenter("user" + i);
            comment.setContent("내용" + i);
            commentRepository.save(comment);
        }

        // flush로 실제 DB 반영
        commentRepository.flush();
    }

    @Test
    @Transactional
    public void compareSingleInsertSelectDeletePerformance() {
        // 1건 삽입
        long insertStart = System.currentTimeMillis();
        Comment newComment = new Comment();
        newComment.setPost(post);
        newComment.setCommenter("test_user");
        newComment.setContent("단일 삽입 테스트");
        commentRepository.save(newComment);
        commentRepository.flush();
        long insertEnd = System.currentTimeMillis();

        // 1건 조회
        long selectStart = System.currentTimeMillis();
        Optional<Comment> comments = commentRepository.findById(COMMENT_COUNT - 1);
        long selectEnd = System.currentTimeMillis();

        // 1건 삭제
        long deleteStart = System.currentTimeMillis();
        commentRepository.delete(newComment);
        commentRepository.flush();
        long deleteEnd = System.currentTimeMillis();

        // 결과 출력
        System.out.printf("삽입 시간: %d ms%n", (insertEnd - insertStart));
        System.out.printf("조회 시간: %d ms%n", (selectEnd - selectStart));
        System.out.printf("삭제 시간: %d ms%n", (deleteEnd - deleteStart));
    }
}
