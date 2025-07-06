package org.example.kb6spring.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.stock.Stock;
import org.example.kb6spring.repository.stock.StockRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;

    /**
     * 낙관적 락을 사용한 재고 감소
     */
    @Transactional
    public void decreaseStockWithOptimisticLock(Long stockId, Integer quantity) {
        Stock stock = stockRepository.findByIdWithOptimisticLock(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));

        if (stock.getQuantity() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + stock.getQuantity());
        }

        stock.setQuantity(stock.getQuantity() - quantity);
        stockRepository.save(stock);

        log.info("낙관적 락으로 재고 감소 완료 - ID: {}, 감소량: {}, 남은 재고: {}",
                stockId, quantity, stock.getQuantity());
    }

    /**
     * 비관적 락(읽기)을 사용한 재고 감소
     */
    @Transactional
    public void decreaseStockWithPessimisticReadLock(Long stockId, Integer quantity) {
        Stock stock = stockRepository.findByIdWithPessimisticReadLock(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));

        if (stock.getQuantity() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + stock.getQuantity());
        }

        stock.setQuantity(stock.getQuantity() - quantity);
        stockRepository.save(stock);

        log.info("비관적 락(읽기)으로 재고 감소 완료 - ID: {}, 감소량: {}, 남은 재고: {}",
                stockId, quantity, stock.getQuantity());
    }

    /**
     * 비관적 락(쓰기)을 사용한 재고 감소
     */
    @Transactional
    public void decreaseStockWithPessimisticWriteLock(Long stockId, Integer quantity) {
        Stock stock = stockRepository.findByIdWithPessimisticWriteLock(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));

        if (stock.getQuantity() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + stock.getQuantity());
        }

        stock.setQuantity(stock.getQuantity() - quantity);
        stockRepository.save(stock);

        log.info("비관적 락(쓰기)으로 재고 감소 완료 - ID: {}, 감소량: {}, 남은 재고: {}",
                stockId, quantity, stock.getQuantity());
    }

    /**
     * 재시도 로직이 포함된 낙관적 락 재고 감소
     */
    @Transactional
    public void decreaseStockWithOptimisticLockAndRetry(Long stockId, Integer quantity, int maxRetries) {
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                decreaseStockWithOptimisticLock(stockId, quantity);
                return; // 성공하면 종료
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.warn("낙관적 락 충돌 발생, 재시도 {}/{} - Stock ID: {}", retryCount, maxRetries, stockId);

                if (retryCount >= maxRetries) {
                    throw new RuntimeException("최대 재시도 횟수 초과: " + maxRetries, e);
                }

                // 짧은 대기 후 재시도
                try {
                    Thread.sleep(50 + (retryCount * 10)); // 백오프 전략
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }
            }
        }
    }

    /**
     * 락 없이 재고 감소 (동시성 문제 발생 가능)
     */
    @Transactional
    public void decreaseStockWithoutLock(Long stockId, Integer quantity) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));

        if (stock.getQuantity() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + stock.getQuantity());
        }

        stock.setQuantity(stock.getQuantity() - quantity);
        stockRepository.save(stock);

        log.info("락 없이 재고 감소 완료 - ID: {}, 감소량: {}, 남은 재고: {}",
                stockId, quantity, stock.getQuantity());
    }

    /**
     * 재고 생성
     */
    @Transactional
    public Stock createStock(String productName, Integer quantity) {
        Stock stock = Stock.builder()
                .productName(productName)
                .quantity(quantity)
                .updatedAt(LocalDateTime.now())
                .build();

        return stockRepository.save(stock);
    }

    /**
     * 재고 조회
     */
    @Transactional(readOnly = true)
    public Stock getStock(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));
    }

    /**
     * 재고 초기화
     */
    @Transactional
    public void resetStock(Long stockId, Integer quantity) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));

        stock.setQuantity(quantity);
        stockRepository.save(stock);
    }
}
