package org.example.kb6spring.service.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.index.Nightmare;
import org.example.kb6spring.repository.index.NightmareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class NightmareService {
    private final NightmareRepository nightmareRepository;
    private final Random random = new Random();

    @Transactional
    public long generateTestData(int count) {
        log.info("Nightmare 더미 데이터 배치 생성 시작 - {} 개", count);
        long startTime = System.currentTimeMillis();
        try {
            List<Nightmare> nightmares = new ArrayList<>();

            for (int i = 1; i <= count; i++) {
                Nightmare nightmare = createRandomNightmare(i);
                nightmares.add(nightmare);

                // 1000개씩 배치로 저장 (메모리 효율성)
                if (i % 1000 == 0 || i == count) {
                    nightmareRepository.saveAll(nightmares);
                    nightmares.clear();

                    if (i % 10000 == 0) {
                        log.info("진행상황: {}/{} 완료 ({}%)", i, count, (i * 100 / count));
                    }
                }
            }
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            log.info("Nightmare 더미 데이터 배치 생성 완료 - {} 개, 실행 시간 - {} ms", count, executionTime);

            return executionTime;
        } catch (Exception e) {
            log.error("더미 데이터 배치 생성 실패", e);
            throw new RuntimeException("더미 데이터 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }


    @Transactional
    public Long insertSampleData() {
        log.info("샘플 Nightmare 데이터 INSERT 시작");
        long startTime = System.currentTimeMillis();

        Nightmare nightmare = new Nightmare();
        nightmare.setUserId(101); // 샘플 사용자 ID
        nightmare.setProductId(201); // 샘플 상품 ID
        nightmare.setOrderDate(LocalDate.now());
        nightmare.setOrderDatetime(LocalDateTime.now());
        nightmare.setAmount(new BigDecimal("150.00")); // 샘플 금액
        nightmare.setQuantity(2); // 샘플 수량
        nightmare.setStatus(Nightmare.OrderStatus.PENDING); // 샘플 상태
        nightmare.setPaymentMethod(Nightmare.PaymentMethod.CREDIT_CARD); // 샘플 결제방법
        nightmare.setShippingAddress("서울시 강남구 테헤란로 123"); // 샘플 주소
        BigDecimal discountRate = new BigDecimal("0.05"); // 5% 할인
        BigDecimal taxAmount = nightmare.getAmount().multiply(new BigDecimal("0.1")); // 10% 세금
        BigDecimal totalAmount = nightmare.getAmount()
                .subtract(nightmare.getAmount().multiply(discountRate))
                .add(taxAmount);
        nightmare.setDiscountRate(discountRate);
        nightmare.setTaxAmount(taxAmount);
        nightmare.setTotalAmount(totalAmount);

        Nightmare savedNightmare = nightmareRepository.save(nightmare);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        log.info("샘플 Nightmare 데이터 INSERT 완료 실행 시간: {}ms", executionTime);

        return executionTime;
    }

    private Nightmare createRandomNightmare(int orderIndex) {
        Nightmare nightmare = new Nightmare();

        nightmare.setOrderId((long) orderIndex);
        nightmare.setUserId(random.nextInt(10000) + 1); // 1~10000
        nightmare.setProductId(random.nextInt(1000) + 200); // 200~1199
        LocalDate baseDate = LocalDate.now().minusDays(random.nextInt(365));
        nightmare.setOrderDate(baseDate);
        nightmare.setOrderDatetime(baseDate.atTime(
                random.nextInt(24), random.nextInt(60), random.nextInt(60)));
        BigDecimal amount = new BigDecimal(random.nextDouble() * 1000 + 50)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        nightmare.setAmount(amount);
        nightmare.setQuantity(random.nextInt(5) + 1);
        Nightmare.OrderStatus[] statuses = Nightmare.OrderStatus.values();
        nightmare.setStatus(statuses[random.nextInt(statuses.length)]);
        Nightmare.PaymentMethod[] methods = Nightmare.PaymentMethod.values();
        nightmare.setPaymentMethod(methods[random.nextInt(methods.length)]);
        String[] cities = {"서울시", "부산시", "대구시", "인천시", "광주시", "대전시", "울산시"};
        String[] districts = {"강남구", "서초구", "송파구", "강동구", "마포구", "영등포구", "중구"};
        nightmare.setShippingAddress(
                cities[random.nextInt(cities.length)] + " " +
                        districts[random.nextInt(districts.length)] + " " +
                        (random.nextInt(999) + 1) + "번길");
        BigDecimal discountRate = new BigDecimal(random.nextDouble() * 0.2) // 0~20%
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal taxAmount = amount.multiply(new BigDecimal("0.1")); // 10% 세금
        BigDecimal totalAmount = amount
                .subtract(amount.multiply(discountRate))
                .add(taxAmount);

        nightmare.setDiscountRate(discountRate);
        nightmare.setTaxAmount(taxAmount);
        nightmare.setTotalAmount(totalAmount);

        return nightmare;
    }
}
