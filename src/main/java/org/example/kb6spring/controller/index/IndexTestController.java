package org.example.kb6spring.controller.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.dto.index.IndexTestDto;
import org.example.kb6spring.dto.index.PerformanceTestResult;
import org.example.kb6spring.service.index.IndexTestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/index-test")
@RequiredArgsConstructor
@Slf4j
public class IndexTestController {

    private final IndexTestService indexTestService;

    // POST
    // http://localhost:8080/index-test/generate-data
    @PostMapping("/generate-data")
    public ResponseEntity<String> generateTestData(@RequestParam(defaultValue = "10000") int count) {
        try {
            indexTestService.generateTestData(count);
            return ResponseEntity.ok(count + "개의 테스트 데이터 생성 완료");
        } catch (Exception e) {
            log.error("테스트 데이터 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("테스트 데이터 생성 실패: " + e.getMessage());
        }
    }

    // 인덱스 적용
    // GET
    // http://localhost:8080/index-test/performance/email
    @GetMapping("/performance/email")
    public ResponseEntity<PerformanceTestResult> testEmailPerformance(
            @RequestParam(defaultValue = "email9999") String email,
            @RequestParam(defaultValue = "1") int iterations) {

        PerformanceTestResult result = indexTestService.findByEmailPerformanceTest(email, iterations);
        return ResponseEntity.ok(result);
    }
    
    // 인덱스 미적용
    // GET
    // http://localhost:8080/index-test/performance/username
    @GetMapping("/performance/username")
    public ResponseEntity<PerformanceTestResult> testUsernamePerformance(
            @RequestParam(defaultValue = "user9999") String username,
            @RequestParam(defaultValue = "1") int iterations) {

        PerformanceTestResult result = indexTestService.findByUsernamePerformanceTest(username, iterations);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/performance/like-search")
    public ResponseEntity<List<PerformanceTestResult>> testLikeSearchPerformance(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "500") int iterations) {

        List<PerformanceTestResult> results = indexTestService.likeSearchPerformanceTest(keyword, iterations);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/performance/paging")
    public ResponseEntity<PerformanceTestResult> testPagingPerformance(
            @RequestParam(defaultValue = "test") String emailPrefix,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "50") int pageCount) {

        PerformanceTestResult result = indexTestService.pagingPerformanceTest(emailPrefix, pageSize, pageCount);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/performance/complex")
    public ResponseEntity<PerformanceTestResult> testComplexSearchPerformance(
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam(defaultValue = "1000") int iterations) {

        PerformanceTestResult result = indexTestService.complexSearchPerformanceTest(email, username, iterations);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/performance/full-test")
    public ResponseEntity<List<PerformanceTestResult>> runFullPerformanceTest() {
        List<PerformanceTestResult> results = Arrays.asList(
                indexTestService.findByEmailPerformanceTest("test100@example.com", 1000),
                indexTestService.findByUsernamePerformanceTest("user100", 1000),
                indexTestService.complexSearchPerformanceTest("test100@example.com", "user100", 1000),
                indexTestService.pagingPerformanceTest("test", 20, 50)
        );

        return ResponseEntity.ok(results);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<IndexTestDto> findByEmail(@PathVariable String email) {
        Optional<IndexTestDto> result = indexTestService.findByEmail(email);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<IndexTestDto>> findAll() {
        List<IndexTestDto> results = indexTestService.findAll();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCount() {
        long count = indexTestService.getTotalCount();
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/clear-data")
    public ResponseEntity<String> clearAllData() {
        try {
            indexTestService.deleteAllData();
            return ResponseEntity.ok("모든 테스트 데이터 삭제 완료");
        } catch (Exception e) {
            log.error("데이터 삭제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("데이터 삭제 실패: " + e.getMessage());
        }
    }

    @GetMapping("/performance/comparison")
    public ResponseEntity<List<PerformanceTestResult>> performanceComparison(
            @RequestParam(defaultValue = "1000") int iterations) {

        List<PerformanceTestResult> results = Arrays.asList(
                indexTestService.findByEmailPerformanceTest("test500@example.com", iterations),
                indexTestService.findByUsernamePerformanceTest("user500", iterations)
        );

        PerformanceTestResult emailResult = results.get(0);
        PerformanceTestResult usernameResult = results.get(1);

        log.info("=== 성능 비교 결과 ===");
        log.info("이메일 조회 (인덱스): {}ms", emailResult.getAverageTime());
        log.info("사용자명 조회 (비인덱스): {}ms", usernameResult.getAverageTime());
        log.info("성능 차이: {}배", usernameResult.getAverageTime() / emailResult.getAverageTime());

        return ResponseEntity.ok(results);
    }
}