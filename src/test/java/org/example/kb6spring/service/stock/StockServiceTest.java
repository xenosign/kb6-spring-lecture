package org.example.kb6spring.service.stock;

import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.config.RootConfig;
import org.example.kb6spring.domain.stock.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@ContextConfiguration(classes = RootConfig.class)
@TestPropertySource("classpath:application.properties")
@Slf4j
class StockServiceTest {

    @Autowired
    private StockService stockService;

    private Stock testStock;
    private static final int THREAD_COUNT = 10;
    private static final int INITIAL_QUANTITY = 100;
    private static final int DECREASE_QUANTITY = 1;

    @BeforeEach
    @Transactional
    void setUp() {
        testStock = stockService.createStock("Test Product", INITIAL_QUANTITY);
    }

    @Test
    @DisplayName("락 없이 동시성 테스트 - 데이터 정합성 문제 발생")
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
                "락 없이는 동시성 문제가 발생할 수 있습니다");
    }

    @Test
    @DisplayName("낙관적 락 동시성 테스트 - 충돌 발생 및 실패")
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
    @DisplayName("낙관적 락 + 재시도 로직 동시성 테스트")
    void testConcurrencyWithOptimisticLockAndRetry() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    stockService.decreaseStockWithOptimisticLockAndRetry(
                            testStock.getId(), DECREASE_QUANTITY, 5);
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

        // Then
        Stock finalStock = stockService.getStock(testStock.getId());
        log.info("최종 재고: {}, 성공: {}, 실패: {}",
                finalStock.getQuantity(), successCount.get(), failureCount.get());

        // 재시도 로직이 있으므로 성공률이 높아야 함
        assertTrue(successCount.get() > failureCount.get(),
                "재시도 로직으로 인해 성공률이 높아야 합니다");
    }

    @Test
    @DisplayName("비관적 락(읽기) 동시성 테스트")
    void testConcurrencyWithPessimisticReadLock() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    stockService.decreaseStockWithPessimisticReadLock(
                            testStock.getId(), DECREASE_QUANTITY);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("비관적 락(읽기) 실패: {}", e.getMessage());
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
    @DisplayName("비관적 락(쓰기) 동시성 테스트")
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
    @DisplayName("재고 부족 시 예외 처리 테스트")
    void testInsufficientStock() {
        // Given
        Stock stock = stockService.createStock("Limited Product", 5);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            stockService.decreaseStockWithOptimisticLock(stock.getId(), 10);
        });
    }

    @Test
    @DisplayName("대용량 동시성 테스트")
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
}