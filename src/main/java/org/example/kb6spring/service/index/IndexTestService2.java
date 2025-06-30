package org.example.kb6spring.service.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.post.Comment;
import org.example.kb6spring.domain.post.Post;
import org.example.kb6spring.repository.post.jpa.CommentJpaRepository;
import org.example.kb6spring.repository.post.jpa.PostJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IndexTestService2 {
    private final PostJpaRepository postRepository;
    private final CommentJpaRepository commentRepository;

    public void generatePostAndComments(int commentCount) {
        long startTime = System.currentTimeMillis();

        Post post = new Post();
        post.setTitle("성능 측정용 게시글");
        postRepository.save(post);

        List<Comment> commentList = new ArrayList<>(commentCount);
        for (int i = 0; i < commentCount; i++) {
            Comment comment = new Comment();
            comment.setPost(post);
            comment.setCommenter("user" + i);
            comment.setContent("테스트 댓글 내용 " + i);
            commentList.add(comment);
        }

        commentRepository.saveAll(commentList);
        commentRepository.flush();

        long endTime = System.currentTimeMillis();
        log.info("더미 데이터 생성 완료 - 댓글 수: {}, 걸린 시간: {} ms", commentCount, (endTime - startTime));
    }

    public List<String> measureCommentQueryTimes(long commentId) {
        List<String> results = new ArrayList<>();

        // 조회
        long selectStart = System.currentTimeMillis();
        Optional<Comment> optionalComment = commentRepository.findById(commentId);
        long selectEnd = System.currentTimeMillis();
        results.add("조회 시간 (id = " + commentId + "): " + (selectEnd - selectStart) + " ms");

        // 삽입
        Comment newComment = new Comment();
        newComment.setPost(postRepository.findById(1L).orElseThrow(() -> new RuntimeException("Post ID 1 없음")));
        newComment.setCommenter("test_user_insert");
        newComment.setContent("성능 테스트용 댓글");

        long insertStart = System.currentTimeMillis();
        commentRepository.save(newComment);
        commentRepository.flush();
        long insertEnd = System.currentTimeMillis();
        results.add("삽입 시간: " + (insertEnd - insertStart) + " ms");

        // 삭제
        long deleteStart = System.currentTimeMillis();
        commentRepository.delete(newComment);
        commentRepository.flush();
        long deleteEnd = System.currentTimeMillis();
        results.add("삭제 시간: " + (deleteEnd - deleteStart) + " ms");

        return results;
    }
}
