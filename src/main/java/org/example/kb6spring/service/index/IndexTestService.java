package org.example.kb6spring.service.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.index.IndexTest;
import org.example.kb6spring.dto.index.IndexTestDto;
import org.example.kb6spring.dto.index.PerformanceTestResult;
import org.example.kb6spring.repository.index.IndexTestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IndexTestService {

    private final IndexTestRepository indexTestRepository;
    private final Random random = new Random();

    // 대량 데이터 생성
    public void generateTestData(int count) {
        log.info("{}개의 테스트 데이터 생성 시작", count);

        List<IndexTest> testDataList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            IndexTest indexTest = new IndexTest();
            indexTest.setUsername("user" + i);
            indexTest.setEmail("test" + i + "@example.com");
            indexTest.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));

            testDataList.add(indexTest);

            // 배치 처리 (1000개씩)
            if (testDataList.size() == 1000) {
                indexTestRepository.saveAll(testDataList);
                testDataList.clear();
                log.info("{}개 데이터 저장 완료", (i + 1));
            }
        }

        // 남은 데이터 저장
        if (!testDataList.isEmpty()) {
            indexTestRepository.saveAll(testDataList);
        }

        log.info("총 {}개의 테스트 데이터 생성 완료", count);
    }

    // 이메일로 조회 (인덱스 사용)
    @Transactional(readOnly = true)
    public PerformanceTestResult findByEmailPerformanceTest(String email, int iterations) {
        log.info("이메일 조회 성능 테스트 시작 - 반복횟수: {}", iterations);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            indexTestRepository.findByEmail(email);
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        return PerformanceTestResult.builder()
                .testName("이메일 조회 (인덱스 사용)")
                .iterations(iterations)
                .totalTime(executionTime)
                .averageTime((double) executionTime / iterations)
                .build();
    }

    // 사용자명으로 조회 (인덱스 미사용)
    @Transactional(readOnly = true)
    public PerformanceTestResult findByUsernamePerformanceTest(String username, int iterations) {
        log.info("사용자명 조회 성능 테스트 시작 - 반복횟수: {}", iterations);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            indexTestRepository.findByUsername(username);
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        return PerformanceTestResult.builder()
                .testName("사용자명 조회 (인덱스 미사용)")
                .iterations(iterations)
                .totalTime(executionTime)
                .averageTime((double) executionTime / iterations)
                .build();
    }

    // LIKE 검색 성능 비교
    @Transactional(readOnly = true)
    public List<PerformanceTestResult> likeSearchPerformanceTest(String keyword, int iterations) {
        List<PerformanceTestResult> results = new ArrayList<>();

        // 이메일 LIKE 검색 (인덱스 부분 사용)
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            indexTestRepository.findByEmailContaining(keyword);
        }
        long endTime = System.currentTimeMillis();

        results.add(PerformanceTestResult.builder()
                .testName("이메일 LIKE 검색 (인덱스 부분 사용)")
                .iterations(iterations)
                .totalTime(endTime - startTime)
                .averageTime((double) (endTime - startTime) / iterations)
                .build());

        // 사용자명 LIKE 검색 (인덱스 미사용)
        startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            indexTestRepository.findByUsernameContaining(keyword);
        }
        endTime = System.currentTimeMillis();

        results.add(PerformanceTestResult.builder()
                .testName("사용자명 LIKE 검색 (인덱스 미사용)")
                .iterations(iterations)
                .totalTime(endTime - startTime)
                .averageTime((double) (endTime - startTime) / iterations)
                .build());

        return results;
    }

    // 페이징 성능 테스트
    @Transactional(readOnly = true)
    public PerformanceTestResult pagingPerformanceTest(String emailPrefix, int pageSize, int pageCount) {
        log.info("페이징 성능 테스트 시작 - 페이지 크기: {}, 페이지 수: {}", pageSize, pageCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < pageCount; i++) {
            Pageable pageable = PageRequest.of(i, pageSize);
            indexTestRepository.findByEmailStartingWith(emailPrefix, pageable);
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        return PerformanceTestResult.builder()
                .testName("페이징 조회 성능")
                .iterations(pageCount)
                .totalTime(executionTime)
                .averageTime((double) executionTime / pageCount)
                .build();
    }

    // 복합 조건 검색 성능 테스트
    @Transactional(readOnly = true)
    public PerformanceTestResult complexSearchPerformanceTest(String email, String username, int iterations) {
        log.info("복합 조건 검색 성능 테스트 시작");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            indexTestRepository.findByEmailAndUsername(email, username);
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        return PerformanceTestResult.builder()
                .testName("복합 조건 검색 (이메일 + 사용자명)")
                .iterations(iterations)
                .totalTime(executionTime)
                .averageTime((double) executionTime / iterations)
                .build();
    }

    // 단일 조회
    @Transactional(readOnly = true)
    public Optional<IndexTestDto> findByEmail(String email) {
        return indexTestRepository.findByEmail(email)
                .map(this::convertToDto);
    }

    // 전체 데이터 조회
    @Transactional(readOnly = true)
    public List<IndexTestDto> findAll() {
        return indexTestRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 데이터 개수 조회
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return indexTestRepository.getTotalCount();
    }

    // 모든 데이터 삭제
    public void deleteAllData() {
        log.info("모든 테스트 데이터 삭제");
        indexTestRepository.deleteAll();
    }

    // Entity to DTO 변환
    private IndexTestDto convertToDto(IndexTest entity) {
        return IndexTestDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}