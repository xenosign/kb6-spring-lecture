package org.example.kb6spring.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.stock.Stock;
import org.example.kb6spring.repository.stock.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Lazy
    @Autowired
    private StockService self;

    // Redis 분산락 관련 상수
    private static final String LOCK_PREFIX = "stock:lock:";
    private static final String STOCK_PREFIX = "stock:quantity:";
    private static final int DEFAULT_LOCK_TIMEOUT = 10; // 10초
    private static final int DEFAULT_WAIT_TIME = 100; // 100ms

    // Lua 스크립트 - 락 해제 시 원자성 보장
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";

    // Lua 스크립트 - 재고 감소 원자성 보장
    private static final String DECREASE_STOCK_SCRIPT =
            "local current = tonumber(redis.call('get', KEYS[1])) " +
                    "if current == nil then " +
                    "    return -1 " +
                    "end " +
                    "if current >= tonumber(ARGV[1]) then " +
                    "    local newValue = redis.call('decrby', KEYS[1], ARGV[1]) " +
                    "    return newValue " +
                    "else " +
                    "    return -2 " +
                    "end";


    // 낙관락
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


    // 비관락
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

    // 낙관락 + 재시도
    public void decreaseStockWithOptimisticLockAndRetry(Long stockId, Integer quantity, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                // self를 통해 호출하여 AOP 프록시 적용
                self.decreaseStockWithOptimisticLockSingleAttempt(stockId, quantity);
                log.info("낙관적 락 재시도 성공 - ID: {}, 시도: {}", stockId, attempts + 1);
                return;

            } catch (Exception e) {
                attempts++;
                log.warn("낙관적 락 충돌 발생. 재시도 {}/{} - ID: {}, 에러: {}",
                        attempts, maxRetries, stockId, e.getMessage());

                if (attempts >= maxRetries) {
                    log.error("재시도 후에도 실패: {}", e.getMessage());
                    throw new RuntimeException("최대 재시도 횟수 초과", e);
                }

                try {
                    long waitTime = 10 + (attempts * 5);
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }
            }
        }
    }

    @Transactional // 개별 시도에만 트랜잭션 적용
    public void decreaseStockWithOptimisticLockSingleAttempt(Long stockId, Integer quantity) {
        Stock stock = stockRepository.findByIdWithOptimisticLock(stockId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + stockId));

        stock.decrease(quantity);
        stockRepository.save(stock);
    }

    // 락 없음
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

    // 원자적
    @Transactional
    public boolean decreaseStockAtomic(Long stockId, Integer quantity) {
        int updatedRows = stockRepository.decreaseStockAtomic(stockId, quantity);

        if (updatedRows > 0) {
            log.info("재고 감소 성공: stockId={}, quantity={}", stockId, quantity);
            return true;
        } else {
            log.warn("재고 감소 실패: stockId={}, quantity={} (재고 부족 또는 존재하지 않음)", stockId, quantity);
            return false;
        }
    }

    // 재고 생성
    @Transactional
    public Stock createStock(String productName, Integer quantity) {
        Stock stock = Stock.builder()
                .productName(productName)
                .quantity(quantity)
                .updatedAt(LocalDateTime.now())
                .build();

        return stockRepository.save(stock);
    }

    // 재고 조회
    @Transactional(readOnly = true)
    public Stock getStock(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));
    }

    // 재고 초기화
    @Transactional
    public void resetStock(Long stockId, Integer quantity) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));

        stock.setQuantity(quantity);
        stockRepository.save(stock);
    }

    // Redis 분산락을 사용한 재고 감소
    public void decreaseStockWithRedisDistributedLock(Long stockId, Integer quantity) {
        String lockKey = LOCK_PREFIX + stockId;
        String lockValue = UUID.randomUUID().toString();

        long startTime = System.currentTimeMillis();

        try {
            // 락 획득 시도
            if (acquireLock(lockKey, lockValue, DEFAULT_LOCK_TIMEOUT)) {
                try {
                    // 비즈니스 로직 실행
                    self.decreaseStockWithTransaction(stockId, quantity);

                    log.info("Redis 분산락으로 재고 감소 완료 - ID: {}, 감소량: {}, 소요시간: {}ms",
                            stockId, quantity, System.currentTimeMillis() - startTime);

                } finally {
                    // 락 해제
                    releaseLock(lockKey, lockValue);
                }
            } else {
                throw new RuntimeException("락 획득 실패 - stockId: " + stockId);
            }
        } catch (Exception e) {
            log.error("Redis 분산락 재고 감소 실패 - ID: {}, 에러: {}", stockId, e.getMessage());
            throw e;
        }
    }

    // Redis 분산락 + 재시도 메커니즘
    public void decreaseStockWithRedisDistributedLockAndRetry(Long stockId, Integer quantity, int maxRetries) {
        String lockKey = LOCK_PREFIX + stockId;
        long startTime = System.currentTimeMillis();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String lockValue = UUID.randomUUID().toString();

            try {
                if (acquireLock(lockKey, lockValue, DEFAULT_LOCK_TIMEOUT)) {
                    try {
                        self.decreaseStockWithTransaction(stockId, quantity);

                        log.info("Redis 분산락 재시도 성공 - ID: {}, 시도: {}/{}, 소요시간: {}ms",
                                stockId, attempt, maxRetries, System.currentTimeMillis() - startTime);
                        return;

                    } finally {
                        releaseLock(lockKey, lockValue);
                    }
                }

                // 락 획득 실패 시 잠시 대기
                if (attempt < maxRetries) {
                    Thread.sleep(DEFAULT_WAIT_TIME);
                    log.warn("Redis 분산락 획득 실패, 재시도 {}/{} - ID: {}",
                            attempt, maxRetries, stockId);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("재시도 중 인터럽트 발생", e);
            } catch (Exception e) {
                log.error("Redis 분산락 처리 중 오류 발생 - ID: {}, 시도: {}, 에러: {}",
                        stockId, attempt, e.getMessage());
                if (attempt == maxRetries) {
                    throw e;
                }
            }
        }

        throw new RuntimeException("최대 재시도 횟수 초과 - stockId: " + stockId);
    }

    // 개선된 Redis 직접 관리 메서드
    public void decreaseStockWithRedisDirectManagement(Long stockId, Integer quantity) {
        String stockKey = STOCK_PREFIX + stockId;
        long startTime = System.currentTimeMillis();

        try {
            Long result = null;
            int maxRetries = 2; // 재시도 횟수 제한

            for (int retry = 0; retry < maxRetries; retry++) {
                // Redis에서 원자적 재고 감소
                DefaultRedisScript<Long> script = new DefaultRedisScript<>(DECREASE_STOCK_SCRIPT, Long.class);
                result = redisTemplate.execute(script, Collections.singletonList(stockKey), quantity.toString());

                if (result == null || result == -1) {
                    if (retry == 0) {
                        // 첫 번째 시도에서만 동기화 수행
                        synchronizeStockToRedis(stockId);
                        continue; // 재시도
                    } else {
                        throw new EntityNotFoundException("Stock synchronization failed: " + stockId);
                    }
                }

                if (result == -2) {
                    throw new IllegalArgumentException("재고가 부족합니다. stockId: " + stockId);
                }

                // 성공적으로 처리됨
                break;
            }

            // DB 동기화 (상대적 감소)
            try {
                self.decreaseStockInDatabaseByAmount(stockId, quantity);
            } catch (Exception e) {
                log.warn("DB 업데이트 실패, Redis 보상 처리 - ID: {}", stockId);
                redisTemplate.opsForValue().increment(stockKey, quantity);
                throw e;
            }

            log.info("Redis 직접 관리로 재고 감소 완료 - ID: {}, 감소량: {}, 남은 재고: {}, 소요시간: {}ms",
                    stockId, quantity, result, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Redis 직접 관리 재고 감소 실패 - ID: {}, 에러: {}", stockId, e.getMessage());
            throw e;
        }
    }

    // 새로운 메서드: 상대적 감소
    @Transactional
    public void decreaseStockInDatabaseByAmount(Long stockId, Integer quantity) {
        try {
            int updatedRows = stockRepository.decreaseStockByAmount(stockId, quantity);
            if (updatedRows == 0) {
                throw new EntityNotFoundException("Stock not found or insufficient quantity: " + stockId);
            }
            log.debug("DB 재고 감소 완료 - ID: {}, 감소량: {}", stockId, quantity);
        } catch (Exception e) {
            log.error("DB 재고 감소 실패 - ID: {}, 에러: {}", stockId, e.getMessage());
            throw e;
        }
    }

    // Redis 분산락 획득
    private boolean acquireLock(String lockKey, String lockValue, int timeoutSeconds) {
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, timeoutSeconds, TimeUnit.SECONDS);
            return result != null && result;
        } catch (Exception e) {
            log.error("락 획득 실패: {}", e.getMessage());
            return false;
        }
    }

    // Redis 분산락 해제
    private void releaseLock(String lockKey, String lockValue) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
        } catch (Exception e) {
            log.error("락 해제 실패: {}", e.getMessage());
        }
    }

    // 트랜잭션 내에서 재고 감소
    @Transactional
    public void decreaseStockWithTransaction(Long stockId, Integer quantity) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));

        if (stock.getQuantity() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + stock.getQuantity());
        }

        stock.setQuantity(stock.getQuantity() - quantity);
        stockRepository.save(stock);
    }

    // 동기화 락을 사용한 안전한 Redis 동기화
    private void synchronizeStockToRedis(Long stockId) {
        String syncLockKey = "sync:lock:" + stockId;
        String syncLockValue = UUID.randomUUID().toString();

        try {
            // 동기화 락 획득 시도 (5초 타임아웃)
            Boolean lockAcquired = redisTemplate.opsForValue()
                    .setIfAbsent(syncLockKey, syncLockValue, 5, TimeUnit.SECONDS);

            if (lockAcquired != null && lockAcquired) {
                try {
                    // 락을 획득한 스레드만 동기화 수행
                    Stock stock = stockRepository.findById(stockId)
                            .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));

                    String stockKey = STOCK_PREFIX + stockId;
                    redisTemplate.opsForValue().set(stockKey, stock.getQuantity().toString());

                    log.info("재고 정보 Redis 동기화 완료 - ID: {}, 재고: {}", stockId, stock.getQuantity());
                } finally {
                    // 동기화 락 해제
                    DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
                    redisTemplate.execute(script, Collections.singletonList(syncLockKey), syncLockValue);
                }
            } else {
                // 다른 스레드가 이미 동기화 중이므로 잠시 대기 후 재시도
                Thread.sleep(100);

                // Redis에 값이 설정되었는지 확인
                String stockKey = STOCK_PREFIX + stockId;
                String cachedValue = redisTemplate.opsForValue().get(stockKey);

                if (cachedValue == null) {
                    // 여전히 없으면 재귀 호출로 재시도
                    synchronizeStockToRedis(stockId);
                } else {
                    log.debug("다른 스레드가 이미 동기화 완료 - ID: {}, 재고: {}", stockId, cachedValue);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("동기화 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("재고 Redis 동기화 실패 - ID: {}, 에러: {}", stockId, e.getMessage());
            throw e;
        }
    }

    // DB 재고 업데이트 (비동기 처리를 위한 메서드)
    @Transactional
    public void updateStockInDatabase(Long stockId, Integer newQuantity) {
        try {
            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + stockId));

            stock.setQuantity(newQuantity);
            stock.setUpdatedAt(LocalDateTime.now());
            stockRepository.save(stock);

            log.debug("DB 재고 업데이트 완료 - ID: {}, 재고: {}", stockId, newQuantity);
        } catch (Exception e) {
            log.error("DB 재고 업데이트 실패 - ID: {}, 에러: {}", stockId, e.getMessage());
            // 실패 시 보상 트랜잭션 또는 알림 처리
        }
    }

    // 성능 측정을 위한 메서드
    public void performanceTest(Long stockId, Integer quantity, String lockType, int threadCount) {
        long startTime = System.currentTimeMillis();

        try {
            switch (lockType.toLowerCase()) {
                case "redis":
                    decreaseStockWithRedisDistributedLock(stockId, quantity);
                    break;
                case "redis_direct":
                    decreaseStockWithRedisDirectManagement(stockId, quantity);
                    break;
                case "optimistic":
                    decreaseStockWithOptimisticLock(stockId, quantity);
                    break;
                case "pessimistic":
                    decreaseStockWithPessimisticWriteLock(stockId, quantity);
                    break;
                case "none":
                    decreaseStockWithoutLock(stockId, quantity);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown lock type: " + lockType);
            }

            long endTime = System.currentTimeMillis();
            log.info("성능 테스트 완료 - 락 타입: {}, 스레드: {}, 소요시간: {}ms",
                    lockType, threadCount, endTime - startTime);

        } catch (Exception e) {
            log.error("성능 테스트 실패 - 락 타입: {}, 에러: {}", lockType, e.getMessage());
            throw e;
        }
    }

    // Redis 재고 정보 조회
    public Integer getStockFromRedis(Long stockId) {
        String stockKey = STOCK_PREFIX + stockId;
        String quantity = redisTemplate.opsForValue().get(stockKey);
        return quantity != null ? Integer.parseInt(quantity) : null;
    }

    // 모든 Redis 재고 캐시 삭제
    public void clearRedisStockCache() {
        try {
            redisTemplate.delete(redisTemplate.keys(STOCK_PREFIX + "*"));
            log.info("Redis 재고 캐시 초기화 완료");
        } catch (Exception e) {
            log.error("Redis 재고 캐시 초기화 실패: {}", e.getMessage());
        }
    }
}
