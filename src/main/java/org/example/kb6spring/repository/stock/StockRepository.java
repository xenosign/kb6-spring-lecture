package org.example.kb6spring.repository.stock;

import org.example.kb6spring.domain.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    // 비관적 락 - 읽기 락 (다른 트랜잭션에서 읽기는 가능, 쓰기는 불가능)
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT s FROM Stock s WHERE s.id = :id")
    Optional<Stock> findByIdWithPessimisticReadLock(@Param("id") Long id);

    // 비관적 락 - 쓰기 락 (다른 트랜잭션에서 읽기/쓰기 모두 불가능)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.id = :id")
    Optional<Stock> findByIdWithPessimisticWriteLock(@Param("id") Long id);

    // 비관적 락 - 강제 버전 증가
    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    @Query("SELECT s FROM Stock s WHERE s.id = :id")
    Optional<Stock> findByIdWithPessimisticForceIncrement(@Param("id") Long id);

    // 낙관적 락 - 일반 조회 (버전 체크)
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT s FROM Stock s WHERE s.id = :id")
    Optional<Stock> findByIdWithOptimisticLock(@Param("id") Long id);

    // 낙관적 락 - 강제 버전 증가
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT s FROM Stock s WHERE s.id = :id")
    Optional<Stock> findByIdWithOptimisticForceIncrement(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Stock s SET s.quantity = s.quantity - :quantity " +
            "WHERE s.id = :stockId AND s.quantity >= :quantity")
    int decreaseStockAtomic(@Param("stockId") Long stockId,
                            @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Stock s SET s.quantity = :quantity, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
    int updateStockQuantityAtomic(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Stock s SET s.quantity = s.quantity - :amount, s.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE s.id = :id AND s.quantity >= :amount")
    int decreaseStockByAmount(@Param("id") Long id, @Param("amount") Integer amount);

    // 상품명으로 조회
    Optional<Stock> findByProductName(String productName);
}