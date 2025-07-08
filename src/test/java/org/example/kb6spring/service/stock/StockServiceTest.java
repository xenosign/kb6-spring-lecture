package org.example.kb6spring.service.stock;

import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.config.TestConfig;
import org.example.kb6spring.domain.stock.Stock;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // Redis 직접 제어용

    private Stock testStock;

    // 쓰레드 수
    private static final int THREAD_COUNT = 10;
    // 초기 재고 수
    private static final int INITIAL_QUANTITY = 100;
    // 시도 당 감소 재고 수
    private static final int DECREASE_QUANTITY = 1;

    // 낙관락 재시도 수
    private static final int OPTIMISTIC_LOCK_RETRY_COUNT = 10;
    // Redis 분산락 재시도 수
    private static final int REDIS_RETRY_COUNT = 10;

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
        cleanupRedisCache();
        initializeRedisForTest(testStock.getId(), INITIAL_QUANTITY);
        log.debug("테스트 셋업 완료 - Stock ID: {}, 초기 재고: {}", testStock.getId(), INITIAL_QUANTITY);
    }

    @AfterEach
    void tearDown() {
        cleanupRedisCache();
        log.debug("테스트 정리 완료");
    }

    private void cleanupRedisCache() {
        try {
            stockService.clearRedisStockCache();
            Set<String> lockKeys = redisTemplate.keys("stock:lock:*");

            if (lockKeys != null && !lockKeys.isEmpty()) {
                redisTemplate.delete(lockKeys);
            }

            Set<String> initKeys = redisTemplate.keys("init:lock:*");
            if (initKeys != null && !initKeys.isEmpty()) {
                redisTemplate.delete(initKeys);
            }

            Set<String> syncKeys = redisTemplate.keys("sync:lock:*");
            if (syncKeys != null && !syncKeys.isEmpty()) {
                redisTemplate.delete(syncKeys);
            }

            log.debug("Redis 캐시 완전 초기화 완료");
        } catch (Exception e) {
            log.warn("Redis 캐시 초기화 중 오류: {}", e.getMessage());
        }
    }

    private void initializeRedisForTest(Long stockId, Integer quantity) {
        try {
            String stockKey = "stock:quantity:" + stockId;
            redisTemplate.opsForValue().set(stockKey, quantity.toString());
            log.debug("Redis 테스트 초기화 - Stock ID: {}, 재고: {}", stockId, quantity);
        } catch (Exception e) {
            log.warn("Redis 테스트 초기화 실패: {}", e.getMessage());
        }
    }


    @Test
    @Order(1)
    @DisplayName("1. 락 없이 테스트 - 10 개 쓰레드 쓰기")
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
    @DisplayName("2. 낙관적 락 - 10개 쓰레드 쓰기")
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
    @DisplayName("3. 낙관적 락(재시도 O) - 10개 쓰레드")
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
                            testStock.getId(), DECREASE_QUANTITY, OPTIMISTIC_LOCK_RETRY_COUNT);
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
        log.info("낙관락 재시도 수: {}", OPTIMISTIC_LOCK_RETRY_COUNT);
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
    @DisplayName("4. 비관적 락 테스트 - 10개 쓰레드")
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
    @DisplayName("5. 낙관적 락(재시도 X) - 대용량 100개 쓰레드, 충돌 관찰용")
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
    @Order(6)
    @DisplayName("6. 낙관적 락(재시도 O) - 대용량 100개 쓰레드")
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
                            highVolumeStock.getId(), 1, OPTIMISTIC_LOCK_RETRY_COUNT);
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

        log.info("재시도 로직으로 인한 성공률 개선 효과를 관찰하세요");

        // 재시도 없는 버전과 비교를 위한 정보 제공
        if (successCount.get() > highThreadCount * 0.7) {
            log.info("재시도 로직이 효과적으로 작동했습니다");
        } else {
            log.warn("대용량 환경에서는 재시도 로직도 한계가 있습니다");
        }
    }

    @Test
    @Order(7)
    @DisplayName("7. 비관적 락 테스트 - 대용량 100개 쓰레드")
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
    @Order(8)
    @DisplayName("8. 낙관적 락(재시도 X) vs 비관적 락 성능 비교 - 100개 쓰레드")
    void testOptimisticVsPessimisticLockPerformanceWithoutRetry() throws InterruptedException {
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
                    stockService.decreaseStockWithOptimisticLock(
                            optimisticStock.getId(), 1);
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
    @DisplayName("9. 낙관적 락(재시도 O) vs 비관적 락 성능 비교 - 100개 쓰레드")
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
        log.info("낙관락 재시도 수: {}", OPTIMISTIC_LOCK_RETRY_COUNT);
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
    @Order(10)
    @DisplayName("10. 원자적 업데이트 테스트 - 100개 쓰레드 쓰기")
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
    @Order(11)
    @DisplayName("11. 원자적 vs 락 성능 비교 테스트")
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

    @Test
    @Order(12)
    @DisplayName("12. Redis 분산락 테스트 - 10개 쓰레드 기본 동작 확인")
    void testRedisDistributedLockBasic() throws InterruptedException {
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
                    stockService.decreaseStockWithRedisDistributedLock(testStock.getId(), DECREASE_QUANTITY);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Redis 분산락 실패: {}", e.getMessage());
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
        log.info("=== Redis 분산락 기본 테스트 결과 ===");
        log.info("실행 시간: {}ms", executionTime);
        log.info("최종 재고: {}, 성공: {}, 실패: {}",
                finalStock.getQuantity(), successCount.get(), failureCount.get());

        // 성공한 만큼만 재고가 감소되어야 함 (재시도 없는 버전의 특성)
        assertEquals(INITIAL_QUANTITY - (successCount.get() * DECREASE_QUANTITY),
                finalStock.getQuantity().intValue());

        // 일부 요청은 실패해야 함 (락 경합으로 인해)
        assertTrue(failureCount.get() > 0, "락 경합으로 일부 요청이 실패해야 합니다");
        assertTrue(successCount.get() > 0, "최소 하나의 요청은 성공해야 합니다");
    }

    @Test
    @Order(13)
    @DisplayName("13. Redis 분산락 + 재시도 테스트 - 10개 쓰레드")
    void testRedisDistributedLockWithRetry() throws InterruptedException {
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
                    stockService.decreaseStockWithRedisDistributedLockAndRetry(
                            testStock.getId(), DECREASE_QUANTITY, REDIS_RETRY_COUNT);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Redis 분산락 재시도 실패: {}", e.getMessage());
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
        log.info("=== Redis 분산락 + 재시도 테스트 결과 ===");
        log.info("Redis 분산락 재시도 수: {}", REDIS_RETRY_COUNT);
        log.info("실행 시간: {}ms", executionTime);
        log.info("최종 재고: {}, 성공: {}, 실패: {}",
                finalStock.getQuantity(), successCount.get(), failureCount.get());

        // 재시도로 모든 요청이 성공해야 함
        assertEquals(INITIAL_QUANTITY - THREAD_COUNT, finalStock.getQuantity().intValue());
        assertEquals(THREAD_COUNT, successCount.get());
        assertEquals(0, failureCount.get());
    }

    @Test
    @Order(14)
    @DisplayName("14. Redis 분산락 대용량 테스트 - 100개 쓰레드")
    void testRedisDistributedLockHighVolume() throws InterruptedException {
        // Given
        int highThreadCount = 100;
        Stock redisHighVolumeStock = stockService.createStock("Redis High Volume Test", 1000);

        CountDownLatch latch = new CountDownLatch(highThreadCount);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < highThreadCount; i++) {
            executor.submit(() -> {
                try {
                    stockService.decreaseStockWithRedisDistributedLockAndRetry(
                            redisHighVolumeStock.getId(), 1, REDIS_RETRY_COUNT);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Redis 분산락 대용량 실패: {}", e.getMessage());
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
        Stock finalStock = stockService.getStock(redisHighVolumeStock.getId());

        log.info("=== Redis 분산락 대용량 테스트 결과 ===");
        log.info("Redis 분산락 재시도 수: {}", REDIS_RETRY_COUNT);
        log.info("실행 시간: {}ms", executionTime);
        log.info("최종 재고: {}", finalStock.getQuantity());
        log.info("성공: {} ({}%)", successCount.get(), (successCount.get() * 100.0 / highThreadCount));
        log.info("실패: {} ({}%)", failureCount.get(), (failureCount.get() * 100.0 / highThreadCount));

        // Redis 분산락의 안정성 확인
        assertEquals(1000 - successCount.get(), finalStock.getQuantity().intValue());

        // 대용량에서도 높은 성공률을 기대
        assertTrue(successCount.get() > highThreadCount * 0.8,
                "Redis 분산락은 대용량에서도 높은 성공률을 보여야 합니다");
    }

    @Test
    @Order(15)
    @DisplayName("15. Redis 직접 관리 테스트 - 10개 쓰레드")
    void testRedisDirectManagement() throws InterruptedException {
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
                    stockService.decreaseStockWithRedisDirectManagement(testStock.getId(), DECREASE_QUANTITY);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Redis 직접 관리 실패: {}", e.getMessage());
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
        Integer redisStock = stockService.getStockFromRedis(testStock.getId());

        log.info("=== Redis 직접 관리 테스트 결과 ===");
        log.info("실행 시간: {}ms", executionTime);
        log.info("DB 최종 재고: {}, Redis 최종 재고: {}", finalStock.getQuantity(), redisStock);
        log.info("성공: {}, 실패: {}", successCount.get(), failureCount.get());

        // Redis 직접 관리로 정확한 결과가 나와야 함
        assertEquals(THREAD_COUNT, successCount.get());
        assertEquals(0, failureCount.get());

        // Redis와 DB 동기화 확인 (비동기 업데이트를 고려해 약간의 대기 시간 필요할 수 있음)
        assertNotNull(redisStock);
        assertEquals(INITIAL_QUANTITY - THREAD_COUNT, redisStock.intValue());
    }



    @Test
    @Order(16)
    @DisplayName("16. Redis 직접 관리 대용량 테스트 - 100개 쓰레드")
    void testRedisDirectManagementHighVolume() throws InterruptedException {
        // Given
        int highThreadCount = 100;
        Stock redisDirectStock = stockService.createStock("Redis Direct High Volume Test", 1000);

        CountDownLatch latch = new CountDownLatch(highThreadCount);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < highThreadCount; i++) {
            executor.submit(() -> {
                try {
                    stockService.decreaseStockWithRedisDirectManagement(redisDirectStock.getId(), 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Redis 직접 관리 대용량 실패: {}", e.getMessage());
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
        Integer redisStock = stockService.getStockFromRedis(redisDirectStock.getId());

        log.info("=== Redis 직접 관리 대용량 테스트 결과 ===");
        log.info("실행 시간: {}ms", executionTime);
        log.info("Redis 최종 재고: {}", redisStock);
        log.info("성공: {} ({}%)", successCount.get(), (successCount.get() * 100.0 / highThreadCount));
        log.info("실패: {} ({}%)", failureCount.get(), (failureCount.get() * 100.0 / highThreadCount));

        // Redis 직접 관리의 성능과 정확성 확인
        assertEquals(highThreadCount, successCount.get());
        assertEquals(0, failureCount.get());
        assertNotNull(redisStock);
        assertEquals(1000 - highThreadCount, redisStock.intValue());
    }

    @Test
    @Order(17)
    @DisplayName("17. Redis 직접 관리 vs 분산락 성능 비교 - 100개 쓰레드")
    void testRedisDirectVsDistributedLockPerformance() throws InterruptedException {
        // Given
        int threadCount = 100;
        Stock directStock = stockService.createStock("Redis Direct Performance", 1000);
        Stock distributedLockStock = stockService.createStock("Redis Lock Performance", 1000);

        // Redis 직접 관리 테스트
        long directStartTime = System.currentTimeMillis();
        CountDownLatch directLatch = new CountDownLatch(threadCount);
        ExecutorService directExecutor = Executors.newFixedThreadPool(20);
        AtomicInteger directSuccess = new AtomicInteger(0);
        AtomicInteger directFailure = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            directExecutor.submit(() -> {
                try {
                    stockService.decreaseStockWithRedisDirectManagement(directStock.getId(), 1);
                    directSuccess.incrementAndGet();
                } catch (Exception e) {
                    directFailure.incrementAndGet();
                    log.error("Redis 직접 관리 실패: {}", e.getMessage());
                } finally {
                    directLatch.countDown();
                }
            });
        }

        directLatch.await();
        directExecutor.shutdown();
        long directEndTime = System.currentTimeMillis();

        // Redis 분산락 테스트
        long lockStartTime = System.currentTimeMillis();
        CountDownLatch lockLatch = new CountDownLatch(threadCount);
        ExecutorService lockExecutor = Executors.newFixedThreadPool(20);
        AtomicInteger lockSuccess = new AtomicInteger(0);
        AtomicInteger lockFailure = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            lockExecutor.submit(() -> {
                try {
                    stockService.decreaseStockWithRedisDistributedLockAndRetry(
                            distributedLockStock.getId(), 1, 3);
                    lockSuccess.incrementAndGet();
                } catch (Exception e) {
                    lockFailure.incrementAndGet();
                    log.error("Redis 분산락 실패: {}", e.getMessage());
                } finally {
                    lockLatch.countDown();
                }
            });
        }

        lockLatch.await();
        lockExecutor.shutdown();
        long lockEndTime = System.currentTimeMillis();

        // 결과 비교
        long directTime = directEndTime - directStartTime;
        long lockTime = lockEndTime - lockStartTime;

        Integer redisDirectStock = stockService.getStockFromRedis(directStock.getId());
        Stock finalLockStock = stockService.getStock(distributedLockStock.getId());

        log.info("=== Redis 직접 관리 vs 분산락 성능 비교 ===");
        log.info("Redis 분산락 재시도 수: {}", REDIS_RETRY_COUNT);
        log.info("Redis 직접 관리 - 실행시간: {}ms, 성공: {}, 실패: {}, 최종재고: {}",
                directTime, directSuccess.get(), directFailure.get(), redisDirectStock);
        log.info("Redis 분산락 - 실행시간: {}ms, 성공: {}, 실패: {}, 최종재고: {}",
                lockTime, lockSuccess.get(), lockFailure.get(), finalLockStock.getQuantity());

        double performanceImprovement = ((double)(lockTime - directTime) / lockTime) * 100;
        log.info("Redis 직접 관리가 {}% 더 빠름", String.format("%.2f", performanceImprovement));

        // 정확성 검증
        assertEquals(1000 - threadCount, redisDirectStock.intValue());
        assertEquals(1000 - lockSuccess.get(), finalLockStock.getQuantity().intValue());

        // Redis 직접 관리가 더 빨라야 함
        assertTrue(directTime <= lockTime, "Redis 직접 관리가 분산락보다 빨라야 합니다");

        // 모든 요청이 성공해야 함
        assertEquals(threadCount, directSuccess.get());
        assertEquals(0, directFailure.get());
    }

//    @Test
//    @Order(18)
//    @DisplayName("18. Redis vs DB 락 성능 비교 테스트 - 50개 쓰레드")
//    void testRedisVsDatabaseLockPerformance() throws InterruptedException {
//        // Given
//        int threadCount = 50;
//        Stock redisStock = stockService.createStock("Redis Performance Test", 500);
//        Stock pessimisticStock = stockService.createStock("Pessimistic Performance Test", 500);
//        Stock optimisticStock = stockService.createStock("Optimistic Performance Test", 500);
//
//        // Redis 분산락 테스트
//        long redisStartTime = System.currentTimeMillis();
//        CountDownLatch redisLatch = new CountDownLatch(threadCount);
//        ExecutorService redisExecutor = Executors.newFixedThreadPool(20);
//        AtomicInteger redisSuccess = new AtomicInteger(0);
//
//        for (int i = 0; i < threadCount; i++) {
//            redisExecutor.submit(() -> {
//                try {
//                    stockService.decreaseStockWithRedisDistributedLock(redisStock.getId(), 1);
//                    redisSuccess.incrementAndGet();
//                } catch (Exception e) {
//                    log.error("Redis 분산락 실패: {}", e.getMessage());
//                } finally {
//                    redisLatch.countDown();
//                }
//            });
//        }
//
//        redisLatch.await();
//        redisExecutor.shutdown();
//        long redisEndTime = System.currentTimeMillis();
//
//        // 비관적 락 테스트
//        long pessimisticStartTime = System.currentTimeMillis();
//        CountDownLatch pessimisticLatch = new CountDownLatch(threadCount);
//        ExecutorService pessimisticExecutor = Executors.newFixedThreadPool(20);
//        AtomicInteger pessimisticSuccess = new AtomicInteger(0);
//
//        for (int i = 0; i < threadCount; i++) {
//            pessimisticExecutor.submit(() -> {
//                try {
//                    stockService.decreaseStockWithPessimisticWriteLock(pessimisticStock.getId(), 1);
//                    pessimisticSuccess.incrementAndGet();
//                } catch (Exception e) {
//                    log.error("비관적 락 실패: {}", e.getMessage());
//                } finally {
//                    pessimisticLatch.countDown();
//                }
//            });
//        }
//
//        pessimisticLatch.await();
//        pessimisticExecutor.shutdown();
//        long pessimisticEndTime = System.currentTimeMillis();
//
//        // 낙관적 락 + 재시도 테스트
//        long optimisticStartTime = System.currentTimeMillis();
//        CountDownLatch optimisticLatch = new CountDownLatch(threadCount);
//        ExecutorService optimisticExecutor = Executors.newFixedThreadPool(20);
//        AtomicInteger optimisticSuccess = new AtomicInteger(0);
//
//        for (int i = 0; i < threadCount; i++) {
//            optimisticExecutor.submit(() -> {
//                try {
//                    stockService.decreaseStockWithOptimisticLockAndRetry(optimisticStock.getId(), 1, 5);
//                    optimisticSuccess.incrementAndGet();
//                } catch (Exception e) {
//                    log.error("낙관적 락 실패: {}", e.getMessage());
//                } finally {
//                    optimisticLatch.countDown();
//                }
//            });
//        }
//
//        optimisticLatch.await();
//        optimisticExecutor.shutdown();
//        long optimisticEndTime = System.currentTimeMillis();
//
//        // 결과 비교
//        long redisTime = redisEndTime - redisStartTime;
//        long pessimisticTime = pessimisticEndTime - pessimisticStartTime;
//        long optimisticTime = optimisticEndTime - optimisticStartTime;
//
//        log.info("=== Redis vs DB 락 성능 비교 결과 ===");
//        log.info("Redis 분산락 - 실행시간: {}ms, 성공: {}", redisTime, redisSuccess.get());
//        log.info("비관적 락 - 실행시간: {}ms, 성공: {}", pessimisticTime, pessimisticSuccess.get());
//        log.info("낙관적 락 + 재시도 - 실행시간: {}ms, 성공: {}", optimisticTime, optimisticSuccess.get());
//
//        // 성능 비교 분석
//        long fastestTime = Math.min(Math.min(redisTime, pessimisticTime), optimisticTime);
//
//        if (fastestTime == redisTime) {
//            log.info("Redis 분산락이 가장 빠름");
//        } else if (fastestTime == pessimisticTime) {
//            log.info("비관적 락이 가장 빠름");
//        } else {
//            log.info("낙관적 락이 가장 빠름");
//        }
//
//        // 정확성 검증
//        Stock finalRedisStock = stockService.getStock(redisStock.getId());
//        Stock finalPessimisticStock = stockService.getStock(pessimisticStock.getId());
//        Stock finalOptimisticStock = stockService.getStock(optimisticStock.getId());
//
//        assertEquals(500 - threadCount, finalRedisStock.getQuantity().intValue());
//        assertEquals(500 - threadCount, finalPessimisticStock.getQuantity().intValue());
//        assertEquals(500 - optimisticSuccess.get(), finalOptimisticStock.getQuantity().intValue());
//    }

    @Test
    @Order(18)
    @DisplayName("18. 모든 동시성 제어 방식 종합 성능 비교 - 200개 쓰레드")
    void testComprehensivePerformanceComparison() throws InterruptedException {
        // Given
        int threadCount = 200;
        int initialStock = 2000;

        Stock noLockStock = stockService.createStock("No Lock Comprehensive", initialStock);
        Stock optimisticStock = stockService.createStock("Optimistic Comprehensive", initialStock);
        Stock pessimisticStock = stockService.createStock("Pessimistic Comprehensive", initialStock);
        Stock atomicStock = stockService.createStock("Atomic Comprehensive", initialStock);
        Stock redisLockStock = stockService.createStock("Redis Lock Comprehensive", initialStock);
        Stock redisDirectStock = stockService.createStock("Redis Direct Comprehensive", initialStock);

        Map<String, Long> executionTimes = new HashMap<>();
        Map<String, Integer> successCounts = new HashMap<>();
        Map<String, Integer> failureCounts = new HashMap<>();

        // 1. 락 없음 테스트
        performConcurrentTest("락 없음", threadCount,
                () -> stockService.decreaseStockWithoutLock(noLockStock.getId(), 1),
                executionTimes, successCounts, failureCounts);

        // 2. 낙관적 락 + 재시도 테스트
        performConcurrentTest("낙관락(재시도 O)", threadCount,
                () -> stockService.decreaseStockWithOptimisticLockAndRetry(optimisticStock.getId(), 1, OPTIMISTIC_LOCK_RETRY_COUNT),
                executionTimes, successCounts, failureCounts);

        // 3. 비관적 락 테스트
        performConcurrentTest("비관락", threadCount,
                () -> stockService.decreaseStockWithPessimisticWriteLock(pessimisticStock.getId(), 1),
                executionTimes, successCounts, failureCounts);

        // 4. 원자적 업데이트 테스트
        performConcurrentTest("원자적 업데이트", threadCount,
                () -> stockService.decreaseStockAtomic(atomicStock.getId(), 1),
                executionTimes, successCounts, failureCounts);

        // 5. Redis 분산락 테스트
        performConcurrentTest("Redis 분산락", threadCount,
                () -> stockService.decreaseStockWithRedisDistributedLockAndRetry(redisLockStock.getId(), 1, REDIS_RETRY_COUNT),
                executionTimes, successCounts, failureCounts);

        // 6. Redis 직접 관리 테스트
        performConcurrentTest("Redis 직접 관리", threadCount,
                () -> stockService.decreaseStockWithRedisDirectManagement(redisDirectStock.getId(), 1),
                executionTimes, successCounts, failureCounts);

        // 결과 분석 및 출력
        log.info("=== 종합 성능 비교 결과 ({}개 쓰레드) ===", threadCount);

        String[] methods = {"락 없음", "낙관락(재시도 O)", "비관락",
                "원자적 업데이트", "Redis 분산락", "Redis 직접 관리"};
        Stock[] stocks = {noLockStock, optimisticStock, pessimisticStock,
                atomicStock, redisLockStock, redisDirectStock};

        for (int i = 0; i < methods.length; i++) {
            String method = methods[i];
            Stock stock = stocks[i];

            long time = executionTimes.get(method);
            int success = successCounts.get(method);
            int failure = failureCounts.get(method);
            double successRate = (success * 100.0) / threadCount;

            Stock finalStock = stockService.getStock(stock.getId());
            int expectedStock = initialStock - success;
            boolean stockAccuracy = finalStock.getQuantity().equals(expectedStock);

            // Redis Direct의 경우 Redis에서 재고 확인
            if (method.equals("Redis Direct")) {
                Integer redisStock = stockService.getStockFromRedis(stock.getId());
                stockAccuracy = redisStock != null && redisStock.equals(expectedStock);
            }
        }

        // 성능 순위 출력
        log.info("\n=== 성능 순위 (실행시간 기준) ===");
        executionTimes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> log.info("{}. {} - {}ms",
                        new AtomicInteger(), entry.getKey(), entry.getValue()));

        // 안정성 검증
        assertTrue(successCounts.get("비관락") == threadCount, "비관적 락은 모든 요청이 성공해야 함");
        assertTrue(successCounts.get("원자적 업데이트") == threadCount, "원자적 업데이트는 모든 요청이 성공해야 함");
        assertTrue(successCounts.get("Redis 직접 관리") == threadCount, "Redis 직접 관리는 모든 요청이 성공해야 함");

        // Redis 직접 관리가 가장 빠를 것으로 예상
        assertTrue(executionTimes.get("Redis 직접 관리") <= executionTimes.get("Redis 분산락"),
                "Redis 직접 관리가 Redis 분산락보다 빨라야 함");
    }

//    @Test
//    @Order(20)
//    @DisplayName("20. 비관락 vs Redis 직접 - 극한 테스트 (500개 쓰레드)")
//    void testExtremeBenchmark() throws InterruptedException {
//        // Given
//        int extremeThreadCount = 500;
//        int initialStock = 5000;
//
//        // Redis 직접 관리 vs 비관적 락 극한 비교
//        Stock redisExtremeStock = stockService.createStock("Redis Extreme Benchmark", initialStock);
//        Stock pessimisticExtremeStock = stockService.createStock("Pessimistic Extreme Benchmark", initialStock);
//
//        // Redis 직접 관리 극한 테스트
//        long redisStartTime = System.currentTimeMillis();
//        CountDownLatch redisLatch = new CountDownLatch(extremeThreadCount);
//        ExecutorService redisExecutor = Executors.newFixedThreadPool(100);
//        AtomicInteger redisSuccess = new AtomicInteger(0);
//        AtomicInteger redisFailure = new AtomicInteger(0);
//
//        for (int i = 0; i < extremeThreadCount; i++) {
//            redisExecutor.submit(() -> {
//                try {
//                    stockService.decreaseStockWithRedisDirectManagement(redisExtremeStock.getId(), 1);
//                    redisSuccess.incrementAndGet();
//                } catch (Exception e) {
//                    redisFailure.incrementAndGet();
//                } finally {
//                    redisLatch.countDown();
//                }
//            });
//        }
//
//        redisLatch.await();
//        redisExecutor.shutdown();
//        long redisEndTime = System.currentTimeMillis();
//
//        // 비관적 락 극한 테스트
//        long pessimisticStartTime = System.currentTimeMillis();
//        CountDownLatch pessimisticLatch = new CountDownLatch(extremeThreadCount);
//        ExecutorService pessimisticExecutor = Executors.newFixedThreadPool(100);
//        AtomicInteger pessimisticSuccess = new AtomicInteger(0);
//        AtomicInteger pessimisticFailure = new AtomicInteger(0);
//
//        for (int i = 0; i < extremeThreadCount; i++) {
//            pessimisticExecutor.submit(() -> {
//                try {
//                    stockService.decreaseStockWithPessimisticWriteLock(pessimisticExtremeStock.getId(), 1);
//                    pessimisticSuccess.incrementAndGet();
//                } catch (Exception e) {
//                    pessimisticFailure.incrementAndGet();
//                } finally {
//                    pessimisticLatch.countDown();
//                }
//            });
//        }
//
//        pessimisticLatch.await();
//        pessimisticExecutor.shutdown();
//        long pessimisticEndTime = System.currentTimeMillis();
//
//        // 결과 분석
//        long redisTime = redisEndTime - redisStartTime;
//        long pessimisticTime = pessimisticEndTime - pessimisticStartTime;
//
//        Integer finalRedisStock = stockService.getStockFromRedis(redisExtremeStock.getId());
//        Stock finalPessimisticStock = stockService.getStock(pessimisticExtremeStock.getId());
//
//        log.info("=== 극한 성능 벤치마크 결과 ({}개 쓰레드) ===", extremeThreadCount);
//        log.info("Redis 직접 관리:");
//        log.info("  - 실행시간: {}ms", redisTime);
//        log.info("  - 성공: {} ({}%)", redisSuccess.get(), (redisSuccess.get() * 100.0 / extremeThreadCount));
//        log.info("  - 실패: {} ({}%)", redisFailure.get(), (redisFailure.get() * 100.0 / extremeThreadCount));
//        log.info("  - 최종 재고: {}", finalRedisStock);
//        log.info("  - 처리량: {:.2f} TPS", (extremeThreadCount * 1000.0 / redisTime));
//
//        log.info("비관적 락:");
//        log.info("  - 실행시간: {}ms", pessimisticTime);
//        log.info("  - 성공: {} ({}%)", pessimisticSuccess.get(), (pessimisticSuccess.get() * 100.0 / extremeThreadCount));
//        log.info("  - 실패: {} ({}%)", pessimisticFailure.get(), (pessimisticFailure.get() * 100.0 / extremeThreadCount));
//        log.info("  - 최종 재고: {}", finalPessimisticStock.getQuantity());
//        log.info("  - 처리량: {:.2f} TPS", (extremeThreadCount * 1000.0 / pessimisticTime));
//
//        double performanceImprovement = ((double)(pessimisticTime - redisTime) / pessimisticTime) * 100;
//        log.info("성능 개선률: {:.2f}%", performanceImprovement);
//
//        // 검증
//        assertEquals(extremeThreadCount, redisSuccess.get(), "Redis 직접 관리는 모든 요청이 성공해야 함");
//        assertEquals(0, redisFailure.get(), "Redis 직접 관리는 실패가 없어야 함");
//        assertEquals(extremeThreadCount, pessimisticSuccess.get(), "비관적 락은 모든 요청이 성공해야 함");
//        assertEquals(0, pessimisticFailure.get(), "비관적 락은 실패가 없어야 함");
//
//        assertEquals(initialStock - extremeThreadCount, finalRedisStock.intValue());
//        assertEquals(initialStock - extremeThreadCount, finalPessimisticStock.getQuantity().intValue());
//
//        // Redis가 더 빠르거나 비슷해야 함
//        assertTrue(redisTime <= pessimisticTime * 1.1,
//                "Redis 직접 관리가 비관적 락보다 현저히 느리면 안됨");
//    }

    /**
     * 동시성 테스트 수행 헬퍼 메서드
     */
    private void performConcurrentTest(String testName, int threadCount, Runnable task,
                                       Map<String, Long> executionTimes,
                                       Map<String, Integer> successCounts,
                                       Map<String, Integer> failureCounts) throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    task.run();
                    success.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        long endTime = System.currentTimeMillis();

        executionTimes.put(testName, endTime - startTime);
        successCounts.put(testName, success.get());
        failureCounts.put(testName, failure.get());
    }

    @AfterAll
    static void restoreSystemOut() {
        System.setOut(System.out);
    }
}