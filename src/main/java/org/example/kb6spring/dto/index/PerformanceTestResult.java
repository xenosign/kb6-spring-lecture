package org.example.kb6spring.dto.index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceTestResult {
    private String testName;        // 테스트 이름
    private int iterations;         // 반복 횟수
    private long totalTime;         // 총 실행 시간 (ms)
    private double averageTime;     // 평균 실행 시간 (ms)

    // 성능 결과를 문자열로 출력
    @Override
    public String toString() {
        return String.format(
                "테스트: %s, 반복횟수: %d, 총 시간: %dms, 평균 시간: %.2fms",
                testName, iterations, totalTime, averageTime
        );
    }
}
