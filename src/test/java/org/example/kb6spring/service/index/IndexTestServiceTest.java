package org.example.kb6spring.service.index;

import org.example.kb6spring.config.RootConfig; // 실제 설정 클래스명으로 변경
import org.example.kb6spring.dto.index.PerformanceTestResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RootConfig.class })
@TestPropertySource(properties = "spring.profiles.active=test")
class IndexTestServiceTest {

    @Autowired
    private IndexTestService indexTestService;

    private static final int ITERATIONS = 1000;
    private static final String TEST_EMAIL = "9999email";
    private static final String TEST_USERNAME = "9999user";


    @Test
    void findByEmailPerformanceTest() {


        PerformanceTestResult result = indexTestService.findByEmailPerformanceTest(TEST_EMAIL, ITERATIONS);

        assertNotNull(result);
        assertTrue(result.getTotalTime() > 0, "총 실행 시간은 0보다 커야 합니다");
        assertTrue(result.getAverageTime() > 0, "평균 시간은 0보다 커야 합니다");

        System.out.println(result);
    }

    @Test
    void findByUsernamePerformanceTest() {
        PerformanceTestResult result = indexTestService.findByUsernamePerformanceTest(TEST_USERNAME, ITERATIONS);

        assertNotNull(result);
        assertTrue(result.getTotalTime() > 0, "총 실행 시간은 0보다 커야 합니다");
        assertTrue(result.getAverageTime() > 0, "평균 시간은 0보다 커야 합니다");

        System.out.println(result);
    }
}
