package org.example.kb6spring.domain.index;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

//@Entity
//@Table(name = "nightmare", indexes = {
//        // 1. 기본 단일 컬럼 인덱스들
//        @Index(name = "idx_user_id", columnList = "user_id"),
//        @Index(name = "idx_product_id", columnList = "product_id"),
//        @Index(name = "idx_order_date", columnList = "order_date"),
//        @Index(name = "idx_order_datetime", columnList = "order_datetime"),
//        @Index(name = "idx_amount", columnList = "amount"),
//        @Index(name = "idx_quantity", columnList = "quantity"),
//        @Index(name = "idx_status", columnList = "status"),
//        @Index(name = "idx_payment_method", columnList = "payment_method"),
//        @Index(name = "idx_created_at", columnList = "created_at"),
//        @Index(name = "idx_updated_at", columnList = "updated_at"),
//        @Index(name = "idx_discount_rate", columnList = "discount_rate"),
//        @Index(name = "idx_tax_amount", columnList = "tax_amount"),
//        @Index(name = "idx_total_amount", columnList = "total_amount"),
//
//        // 2. 중복되고 비효율적인 복합 인덱스들
//        @Index(name = "idx_user_date", columnList = "user_id, order_date"),
//        @Index(name = "idx_user_datetime", columnList = "user_id, order_datetime"),
//        @Index(name = "idx_user_product", columnList = "user_id, product_id"),
//        @Index(name = "idx_user_status", columnList = "user_id, status"),
//        @Index(name = "idx_user_amount", columnList = "user_id, amount"),
//        @Index(name = "idx_product_date", columnList = "product_id, order_date"),
//        @Index(name = "idx_product_status", columnList = "product_id, status"),
//        @Index(name = "idx_date_status", columnList = "order_date, status"),
//        @Index(name = "idx_amount_status", columnList = "amount, status"),
//
//        // 3. 의미없는 3개 이상의 복합 인덱스들
//        @Index(name = "idx_user_product_date", columnList = "user_id, product_id, order_date"),
//        @Index(name = "idx_user_date_status", columnList = "user_id, order_date, status"),
//        @Index(name = "idx_product_date_status", columnList = "product_id, order_date, status"),
//        @Index(name = "idx_user_amount_quantity", columnList = "user_id, amount, quantity"),
//
//        // 4. 거의 사용되지 않는 복잡한 인덱스들
//        @Index(name = "idx_complex_1", columnList = "status, payment_method, discount_rate"),
//        @Index(name = "idx_complex_2", columnList = "quantity, tax_amount, total_amount"),
//        @Index(name = "idx_complex_3", columnList = "order_date, amount, quantity, status"),
//
//        // 5. 선택도가 낮은 컬럼들에 대한 불필요한 인덱스
//        @Index(name = "idx_low_selectivity_1", columnList = "status, payment_method"),
//        @Index(name = "idx_low_selectivity_2", columnList = "discount_rate, tax_amount")
//})
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class Nightmare {
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "order_id")
//    private Long orderId;
//
//    @Column(name = "user_id", nullable = false)
//    private Integer userId;
//
//    @Column(name = "product_id", nullable = false)
//    private Integer productId;
//
//    @Column(name = "order_date", nullable = false)
//    private LocalDate orderDate;
//
//    @Column(name = "order_datetime", nullable = false)
//    private LocalDateTime orderDatetime;
//
//    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
//    private BigDecimal amount;
//
//    @Column(name = "quantity", nullable = false)
//    private Integer quantity;
//
//    @Column(name = "status", nullable = false, length = 20)
//    @Enumerated(EnumType.STRING)
//    private OrderStatus status;
//
//    @Column(name = "payment_method", length = 20)
//    @Enumerated(EnumType.STRING)
//    private PaymentMethod paymentMethod;
//
//    @Column(name = "shipping_address", columnDefinition = "TEXT")
//    private String shippingAddress;
//
//    @CreationTimestamp
//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    @Builder.Default
//    @Column(name = "discount_rate", precision = 5, scale = 2)
//    private BigDecimal discountRate = BigDecimal.ZERO;
//
//    @Builder.Default
//    @Column(name = "tax_amount", precision = 10, scale = 2)
//    private BigDecimal taxAmount = BigDecimal.ZERO;
//
//    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
//    private BigDecimal totalAmount;
//
//    // Enum 정의
//    public enum OrderStatus {
//        PENDING("pending"),
//        PROCESSING("processing"),
//        COMPLETED("completed"),
//        CANCELLED("cancelled"),
//        SHIPPED("shipped");
//
//        private final String value;
//
//        OrderStatus(String value) {
//            this.value = value;
//        }
//
//        public String getValue() {
//            return value;
//        }
//    }
//
//    public enum PaymentMethod {
//        CREDIT_CARD("credit_card"),
//        PAYPAL("paypal"),
//        BANK_TRANSFER("bank_transfer"),
//        CASH("cash");
//
//        private final String value;
//
//        PaymentMethod(String value) {
//            this.value = value;
//        }
//
//        public String getValue() {
//            return value;
//        }
//    }
//}

@Entity
@Table(name = "nightmare")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nightmare {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "order_datetime", nullable = false)
    private LocalDateTime orderDatetime;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "payment_method", length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Enum 정의
    public enum OrderStatus {
        PENDING("pending"),
        PROCESSING("processing"),
        COMPLETED("completed"),
        CANCELLED("cancelled"),
        SHIPPED("shipped");

        private final String value;

        OrderStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum PaymentMethod {
        CREDIT_CARD("credit_card"),
        PAYPAL("paypal"),
        BANK_TRANSFER("bank_transfer"),
        CASH("cash");

        private final String value;

        PaymentMethod(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
