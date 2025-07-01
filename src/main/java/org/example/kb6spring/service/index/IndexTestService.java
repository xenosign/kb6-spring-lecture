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

    // 테스트 데이터 생성
    public void generateTestData(int count) {
        log.info("{}개의 테스트 데이터 생성 시작", count);

        List<IndexTest> testDataList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            IndexTest indexTest = new IndexTest();
            indexTest.setUsername(i + "user");
            indexTest.setEmail(i + "email");
            indexTest.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));

            testDataList.add(indexTest);

            if (testDataList.size() == 1000) {
                indexTestRepository.saveAll(testDataList);
                testDataList.clear();
                log.info("{}개 데이터 저장 완료", (i + 1));
            }
        }

        if (!testDataList.isEmpty()) {
            indexTestRepository.saveAll(testDataList);
        }

        log.info("총 {}개의 테스트 데이터 생성 완료", count);
    }

    // email 조회 (인덱스 적용)
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
                .testName("이메일 조회 (인덱스 적용)")
                .iterations(iterations)
                .totalTime(executionTime)
                .averageTime((double) executionTime / iterations)
                .build();
    }

    // username 조회 (인덱스 미적용)
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
                .testName("사용자명 조회 (인덱스 미적용)")
                .iterations(iterations)
                .totalTime(executionTime)
                .averageTime((double) executionTime / iterations)
                .build();
    }

    // LIKE 검색
    @Transactional(readOnly = true)
    public List<PerformanceTestResult> likeSearchPerformanceTest(String keyword, int iterations, String option) {
        List<PerformanceTestResult> results = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        if (option == null) {
            for (int i = 0; i < iterations; i++) {
                indexTestRepository.findByEmailContaining(keyword);
            }
        } else {
            for (int i = 0; i < iterations; i++) {
                indexTestRepository.findByEmailStartingWith(keyword);
            }
        }

        long endTime = System.currentTimeMillis();

        if (option == null) {
            results.add(PerformanceTestResult.builder()
                    .testName("이메일 LIKE %email% 검색 (인덱스 적용)")
                    .iterations(iterations)
                    .totalTime(endTime - startTime)
                    .averageTime((double) (endTime - startTime) / iterations)
                    .build());
        } else {
            results.add(PerformanceTestResult.builder()
                    .testName("이메일 LIKE email% 검색 (인덱스 적용)")
                    .iterations(iterations)
                    .totalTime(endTime - startTime)
                    .averageTime((double) (endTime - startTime) / iterations)
                    .build());
        }

        startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            indexTestRepository.findByUsernameContaining(keyword);
        }
        endTime = System.currentTimeMillis();

        results.add(PerformanceTestResult.builder()
                .testName("사용자명 LIKE 검색 (인덱스 미적용)")
                .iterations(iterations)
                .totalTime(endTime - startTime)
                .averageTime((double) (endTime - startTime) / iterations)
                .build());

        return results;
    }

    // 복합(username, email) 검색
    @Transactional(readOnly = true)
    public PerformanceTestResult complexSearchPerformanceTest(String email, String username, int iterations) {
        if (email == null && username == null) {
            throw new IllegalArgumentException("email 또는 username 중 하나는 필수입니다");
        }

        String testName;
        long startTime = System.currentTimeMillis();

        if (email != null && username != null) {
            testName = "복합 조건 검색 (이메일 + 사용자명)";

            for (int i = 0; i < iterations; i++) {
                indexTestRepository.findByEmailAndUsername(email, username);
            }

        } else if (email != null) {
            testName = "이메일 단일 조건 검색 (인덱스 적용)";

            for (int i = 0; i < iterations; i++) {
                indexTestRepository.findByEmail(email);
            }
        } else {
            testName = "사용자명 단일 조건 검색 (인덱스 미적용)";

            for (int i = 0; i < iterations; i++) {
                indexTestRepository.findByUsername(username);
            }
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        double averageTime = (double) executionTime / iterations;

        return PerformanceTestResult.builder()
                .testName(testName)
                .iterations(iterations)
                .totalTime(executionTime)
                .averageTime(averageTime)
                .build();
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