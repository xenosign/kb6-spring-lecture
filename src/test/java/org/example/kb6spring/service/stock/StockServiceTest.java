package org.example.kb6spring.service.stock;

import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.config.TestConfig;
import org.example.kb6spring.domain.stock.Stock;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource("classpath:application.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class StockServiceTest {

    @Autowired
    private StockService stockService;

    private Stock testStock;
    private static final int THREAD_COUNT = 10;
    private static final int INITIAL_QUANTITY = 100;
    private static final int DECREASE_QUANTITY = 1;

    @BeforeAll
    static void disableHibernateSqlPrint() {
        System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
            public void write(int b) {
                // 무시
            }
        }));
    }

    @BeforeEach
    @Transactional
    void setUp() {
        testStock = stockService.createStock("Test Product", INITIAL_QUANTITY);
    }

    @Test
    @Order(1)
    @DisplayName("락 없이 테스트 - 10 개 쓰레드 쓰기")
    void testConcurrencyWithoutLock() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    stockService.decreaseStockWithoutLock(testStock.getId(), DECREASE_QUANTITY);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("재고 감소 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        Stock finalStock = stockService.getStock(testStock.getId());
        log.info("최종 재고: {}, 성공: {}, 실패: {}",
                finalStock.getQuantity(), successCount.get(), failureCount.get());

        // 락이 없으면 동시성 문제로 인해 정확한 결과가 나오지 않을 수 있음
        assertTrue(finalStock.getQuantity() >= INITIAL_QUANTITY - THREAD_COUNT,
                "락 없이는 동시성 문제로 결과 값이 테스트마다 달라질 수 있습니다");
    }

    @Test
    @Order(2)
    @DisplayName("낙관적 락 테스트 - 10개 쓰레드 쓰기")
    void testConcurrencyWithOptimisticLock() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    stockService.decreaseStockWithOptimisticLock(testStock.getId(), DECREASE_QUANTITY);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    failureCount.incrementAndGet();
                    log.warn("낙관적 락 충돌 발생: {}", e.getMessage());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("기타 오류 발생: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        Stock finalStock = stockService.getStock(testStock.getId());
        log.info("최종 재고: {}, 성공: {}, 실패: {}",
                finalStock.getQuantity(), successCount.get(), failureCount.get());

        // 성공한 만큼만 재고가 감소되어야 함
        assertEquals(INITIAL_QUANTITY - (successCount.get() * DECREASE_QUANTITY),
                finalStock.getQuantity().intValue());

        // 일부 요청은 실패해야 함 (낙관적 락 충돌)
        assertTrue(failureCount.get() > 0, "낙관적 락 충돌이 발생해야 합니다");
    }

    @Test
    @Order(3)
    @DisplayName("낙관적 락 테스트 - 10개 쓰레드 쓰기 + 재시도 로직")
    void testConcurrencyWithOptimisticLockAndRetry() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    // 🎯 수정: 분리된 트랜잭션 방식 사용
                    stockService.decreaseStockWithOptimisticLockAndRetry(
                            testStock.getId(), DECREASE_QUANTITY, 10);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("재시도 후에도 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then
        Stock finalStock = stockService.getStock(testStock.getId());

        log.info("=== 낙관적 락 + 재시도 테스트 결과 ===");
        log.info("실행 시간: {}ms", executionTime);
        log.info("최종 재고: {}, 성공: {}, 실패: {}",
                finalStock.getQuantity(), successCount.get(), failureCount.get());

        // ✅ 수정된 검증: 재시도로 모든 요청이 성공해야 함
        assertEquals(INITIAL_QUANTITY - THREAD_COUNT, finalStock.getQuantity().intValue());
        assertEquals(THREAD_COUNT, successCount.get());
        assertEquals(0, failureCount.get());
    }

    @Test
    @Order(4)
    @DisplayName("비관적 락 테스트 - 10개 쓰레드 쓰기")
    void testConcurrencyWithPessimisticWriteLock() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    stockService.decreaseStockWithPessimisticWriteLock(
                            testStock.getId(), DECREASE_QUANTITY);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("비관적 락(쓰기) 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        Stock finalStock = stockService.getStock(testStock.getId());
        log.info("최종 재고: {}, 성공: {}, 실패: {}",
                finalStock.getQuantity(), successCount.get(), failureCount.get());

        // 비관적 락으로 인해 정확한 결과가 나와야 함
        assertEquals(INITIAL_QUANTITY - THREAD_COUNT, finalStock.getQuantity().intValue());
        assertEquals(THREAD_COUNT, successCount.get());
        assertEquals(0, failureCount.get());
    }

    @Test
    @Order(5)
    @DisplayName("비관적 락 테스트 - 대용량 100개 쓰레드 쓰기")
    void testHighVolumeConcurrency() throws InterruptedException {
        // Given
        int highThreadCount = 100;
        Stock highVolumeStock = stockService.createStock("High Volume Product", 1000);

        CountDownLatch latch = new CountDownLatch(highThreadCount);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < highThreadCount; i++) {
            executor.submit(() -> {
                try {
                    stockService.decreaseStockWithPessimisticWriteLock(
                            highVolumeStock.getId(), 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("대용량 테스트 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        Stock finalStock = stockService.getStock(highVolumeStock.getId());
        log.info("대용량 테스트 결과 - 최종 재고: {}, 성공: {}, 실패: {}",
                finalStock.getQuantity(), successCount.get(), failureCount.get());

        assertEquals(1000 - highThreadCount, finalStock.getQuantity().intValue());
    }

    @Test
    @Order(6)
    @DisplayName("낙관적 락 테스트 - 대용량 100개 쓰레드 쓰기 (충돌 관찰용)")
    void testHighVolumeConcurrencyWithOptimisticLock() throws InterruptedException {
        // Given
        int highThreadCount = 100;
        Stock highVolumeStock = stockService.createStock("High Volume Optimistic Test", 1000);

        CountDownLatch latch = new CountDownLatch(highThreadCount);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger optimisticLockFailureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < highThreadCount; i++) {
            executor.submit(() -> {
                try {
                    stockService.decreaseStockWithOptimisticLock(highVolumeStock.getId(), 1);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    optimisticLockFailureCount.incrementAndGet();
                    failureCount.incrementAndGet();
                    log.warn("낙관적 락 충돌 발생 (대용량): {}", e.getMessage());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("기타 오류 발생 (대용량): {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then
        Stock finalStock = stockService.getStock(highVolumeStock.getId());

        log.info("=== 낙관적 락 대용량 테스트 결과 ===");
        log.info("실행 시간: {}ms", executionTime);
        log.info("최종 재고: {}", finalStock.getQuantity());
        log.info("성공: {} ({}%)", successCount.get(), (successCount.get() * 100.0 / highThreadCount));
        log.info("실패: {} ({}%)", failureCount.get(), (failureCount.get() * 100.0 / highThreadCount));
        log.info("낙관적 락 충돌: {} ({}%)", optimisticLockFailureCount.get(), (optimisticLockFailureCount.get() * 100.0 / highThreadCount));

        // 성공한 만큼만 재고가 감소되어야 함
        assertEquals(1000 - successCount.get(), finalStock.getQuantity().intValue());

        // 대용량에서는 충돌률이 매우 높아야 함
        assertTrue(failureCount.get() > successCount.get(),
                "대용량 환경에서는 낙관적 락 충돌이 성공보다 많아야 합니다");

        // 성공률이 낮을 것으로 예상
        assertTrue(successCount.get() < highThreadCount * 0.5,
                "낙관적 락은 대용량 동시성에서 성공률이 낮아집니다");
    }

    @Test
    @Order(7)
    @DisplayName("낙관적 락 대용량 테스트 - 100개 쓰레드 + 재시도 로직")
    void testHighVolumeConcurrencyWithOptimisticLockAndRetry() throws InterruptedException {
        // Given
        int highThreadCount = 100;
        Stock highVolumeStock = stockService.createStock("High Volume Optimistic Retry Test", 1000);

        CountDownLatch latch = new CountDownLatch(highThreadCount);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger totalRetryCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < highThreadCount; i++) {
            executor.submit(() -> {
                try {
                    // 재시도 횟수를 늘려서 테스트
                    stockService.decreaseStockWithOptimisticLockAndRetry(
                            highVolumeStock.getId(), 1, 10);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("재시도 후에도 실패 (대용량): {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then
        Stock finalStock = stockService.getStock(highVolumeStock.getId());

        log.info("=== 낙관적 락 + 재시도 대용량 테스트 결과 ===");
        log.info("실행 시간: {}ms", executionTime);
        log.info("최종 재고: {}", finalStock.getQuantity());
        log.info("성공: {} ({}%)", successCount.get(), (successCount.get() * 100.0 / highThreadCount));
        log.info("실패: {} ({}%)", failureCount.get(), (failureCount.get() * 100.0 / highThreadCount));

        // 성공한 만큼만 재고가 감소되어야 함
        assertEquals(1000 - successCount.get(), finalStock.getQuantity().intValue());

        // 교육 목적: 재시도 로직의 효과 관찰
        // 재시도 로직이 있어도 대용량에서는 한계가 있음을 보여줌
        log.info("재시도 로직으로 인한 성공률 개선 효과를 관찰하세요");

        // 재시도 없는 버전과 비교를 위한 정보 제공
        if (successCount.get() > highThreadCount * 0.7) {
            log.info("재시도 로직이 효과적으로 작동했습니다");
        } else {
            log.warn("대용량 환경에서는 재시도 로직도 한계가 있습니다");
        }
    }

    @Test
    @Order(8)
    @DisplayName("낙관적 락 vs 비관적 락 성능 비교 - 100개 쓰레드")
    void testOptimisticVsPessimisticLockPerformance() throws InterruptedException {
        // Given
        int threadCount = 100;
        Stock optimisticStock = stockService.createStock("Optimistic Performance Test", 1000);
        Stock pessimisticStock = stockService.createStock("Pessimistic Performance Test", 1000);

        // 낙관적 락 테스트
        long optimisticStartTime = System.currentTimeMillis();
        CountDownLatch optimisticLatch = new CountDownLatch(threadCount);
        ExecutorService optimisticExecutor = Executors.newFixedThreadPool(20);
        AtomicInteger optimisticSuccess = new AtomicInteger(0);
        AtomicInteger optimisticFailure = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            optimisticExecutor.submit(() -> {
                try {
                    stockService.decreaseStockWithOptimisticLockAndRetry(
                            optimisticStock.getId(), 1, 5);
                    optimisticSuccess.incrementAndGet();
                } catch (Exception e) {
                    optimisticFailure.incrementAndGet();
                } finally {
                    optimisticLatch.countDown();
                }
            });
        }

        optimisticLatch.await();
        optimisticExecutor.shutdown();
        long optimisticEndTime = System.currentTimeMillis();

        // 비관적 락 테스트
        long pessimisticStartTime = System.currentTimeMillis();
        CountDownLatch pessimisticLatch = new CountDownLatch(threadCount);
        ExecutorService pessimisticExecutor = Executors.newFixedThreadPool(20);
        AtomicInteger pessimisticSuccess = new AtomicInteger(0);
        AtomicInteger pessimisticFailure = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pessimisticExecutor.submit(() -> {
                try {
                    stockService.decreaseStockWithPessimisticWriteLock(
                            pessimisticStock.getId(), 1);
                    pessimisticSuccess.incrementAndGet();
                } catch (Exception e) {
                    pessimisticFailure.incrementAndGet();
                } finally {
                    pessimisticLatch.countDown();
                }
            });
        }

        pessimisticLatch.await();
        pessimisticExecutor.shutdown();
        long pessimisticEndTime = System.currentTimeMillis();

        // 결과 비교
        long optimisticTime = optimisticEndTime - optimisticStartTime;
        long pessimisticTime = pessimisticEndTime - pessimisticStartTime;

        log.info("=== 낙관적 락 vs 비관적 락 성능 비교 ===");
        log.info("낙관적 락 - 실행시간: {}ms, 성공: {}, 실패: {}",
                optimisticTime, optimisticSuccess.get(), optimisticFailure.get());
        log.info("비관적 락 - 실행시간: {}ms, 성공: {}, 실패: {}",
                pessimisticTime, pessimisticSuccess.get(), pessimisticFailure.get());

        if (optimisticTime > pessimisticTime) {
            log.info("대용량 환경에서는 비관적 락이 더 효율적입니다");
        } else {
            log.info("낙관적 락이 더 빠르게 처리되었습니다");
        }

        // 정확성 검증
        Stock finalOptimisticStock = stockService.getStock(optimisticStock.getId());
        Stock finalPessimisticStock = stockService.getStock(pessimisticStock.getId());

        assertEquals(1000 - optimisticSuccess.get(), finalOptimisticStock.getQuantity().intValue());
        assertEquals(1000 - threadCount, finalPessimisticStock.getQuantity().intValue());
    }

    @Test
    @Order(9)
    @DisplayName("원자적 업데이트 테스트 - 100개 쓰레드 쓰기")
    void testAtomicUpdateConcurrency() throws InterruptedException {
        // Given
        int threadCount = 100;
        Stock atomicStock = stockService.createStock("Atomic Test Product", 1000);

        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When - 100개 쓰레드가 동시에 원자적 업데이트
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = stockService.decreaseStockAtomic(atomicStock.getId(), 1);
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("원자적 업데이트 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then
        Stock finalStock = stockService.getStock(atomicStock.getId());

        log.info("=== 원자적 업데이트 테스트 결과 ===");
        log.info("실행 시간: {}ms", executionTime);
        log.info("최종 재고: {}", finalStock.getQuantity());
        log.info("성공: {} ({}%)", successCount.get(), (successCount.get() * 100.0 / threadCount));
        log.info("실패: {} ({}%)", failureCount.get(), (failureCount.get() * 100.0 / threadCount));

        // 검증
        assertEquals(1000 - threadCount, finalStock.getQuantity().intValue());
        assertEquals(threadCount, successCount.get());
        assertEquals(0, failureCount.get());
    }

    @Test
    @Order(10)
    @DisplayName("🔄 원자적 vs 락 성능 비교 테스트")
    void testAtomicVsLockPerformanceComparison() throws InterruptedException {
        // Given
        int threadCount = 50;
        Stock atomicStock = stockService.createStock("Atomic Performance Test", 500);
        Stock lockStock = stockService.createStock("Lock Performance Test", 500);

        // 원자적 업데이트 성능 측정
        long atomicStartTime = System.currentTimeMillis();
        CountDownLatch atomicLatch = new CountDownLatch(threadCount);
        ExecutorService atomicExecutor = Executors.newFixedThreadPool(20);
        AtomicInteger atomicSuccess = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            atomicExecutor.submit(() -> {
                try {
                    if (stockService.decreaseStockAtomic(atomicStock.getId(), 1)) {
                        atomicSuccess.incrementAndGet();
                    }
                } finally {
                    atomicLatch.countDown();
                }
            });
        }

        atomicLatch.await();
        atomicExecutor.shutdown();
        long atomicEndTime = System.currentTimeMillis();

        // 비관적 락 성능 측정
        long lockStartTime = System.currentTimeMillis();
        CountDownLatch lockLatch = new CountDownLatch(threadCount);
        ExecutorService lockExecutor = Executors.newFixedThreadPool(20);
        AtomicInteger lockSuccess = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            lockExecutor.submit(() -> {
                try {
                    stockService.decreaseStockWithPessimisticWriteLock(lockStock.getId(), 1);
                    lockSuccess.incrementAndGet();
                } catch (Exception e) {
                    // 재고 부족으로 실패
                } finally {
                    lockLatch.countDown();
                }
            });
        }

        lockLatch.await();
        lockExecutor.shutdown();
        long lockEndTime = System.currentTimeMillis();

        // 결과 비교
        long atomicTime = atomicEndTime - atomicStartTime;
        long lockTime = lockEndTime - lockStartTime;

        log.info("=== 성능 비교 결과 ===");
        log.info("원자적 업데이트 - 실행시간: {}ms, 성공: {}", atomicTime, atomicSuccess.get());
        log.info("비관적 락 - 실행시간: {}ms, 성공: {}", lockTime, lockSuccess.get());
        log.info("성능 개선: {}% 빠름", ((lockTime - atomicTime) * 100.0 / lockTime));

        // 정확성 검증
        Stock finalAtomicStock = stockService.getStock(atomicStock.getId());
        Stock finalLockStock = stockService.getStock(lockStock.getId());

        assertEquals(500 - threadCount, finalAtomicStock.getQuantity().intValue());
        assertEquals(500 - threadCount, finalLockStock.getQuantity().intValue());

        // 일반적으로 원자적 업데이트가 더 빨라야 함
        assertTrue(atomicTime <= lockTime * 1.5, "원자적 업데이트가 현저히 느리면 안됨");
    }

    @AfterAll
    static void restoreSystemOut() {
        System.setOut(System.out);
    }

}