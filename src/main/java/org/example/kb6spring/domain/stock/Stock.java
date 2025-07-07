package org.example.kb6spring.domain.stock;

import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productName;
    private Integer quantity;
    @Version
    private Long version;
    private java.time.LocalDateTime updatedAt;
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void decrease(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("감소량은 0보다 커야 합니다: " + amount);
        }

        if (this.quantity < amount) {
            throw new IllegalArgumentException(
                    String.format("재고가 부족합니다. 현재 재고: %d, 요청 수량: %d", this.quantity, amount)
            );
        }

        this.quantity -= amount;
        // updatedAt은 @PreUpdate에서 자동으로 설정됨
    }
}

