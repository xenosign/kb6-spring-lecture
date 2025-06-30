package org.example.kb6spring.controller.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.service.index.IndexTestService2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/index-test2")
@RequiredArgsConstructor
@Slf4j
public class IndexTestController2 {
    private static final String COMMENT_COUNTS = "10000";
    private static final String TEST_COMMENT_ID = "9999";
    private final IndexTestService2 indexTestService2;

    // GET
    // http://localhost:8080/index-test2/generate-data
    @GetMapping("/generate-data")
    public ResponseEntity<String> generateTestData(@RequestParam(defaultValue = COMMENT_COUNTS) int count) {
        try {
            indexTestService2.generatePostAndComments(count);
            return ResponseEntity.ok("1개의 Post, " + count + "개의 Comment 테스트 데이터 생성 완료");
        } catch (Exception e) {
            log.error("테스트 데이터 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("테스트 데이터 생성 실패: " + e.getMessage());
        }
    }

    // GET
    // http://localhost:8080/index-test2/measure-performance
    @GetMapping("/measure-performance")
    public ResponseEntity<List<String>> measureCommentPerformance(@RequestParam(defaultValue = TEST_COMMENT_ID) int commentId) {
        try {
            List<String> result = indexTestService2.measureCommentQueryTimes(commentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("쿼리 성능 측정 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Arrays.asList("쿼리 성능 측정 실패: " + e.getMessage()));
        }
    }
}