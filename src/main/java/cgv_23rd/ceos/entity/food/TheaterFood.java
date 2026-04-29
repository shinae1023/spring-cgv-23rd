package cgv_23rd.ceos.entity.food;

import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_theater_food", columnNames = {"theater_id", "food_id"})
        }
)
public class TheaterFood {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    private Integer amount;

    public static TheaterFood create(Theater theater, Food food) {
        return TheaterFood.builder()
                .theater(theater)
                .food(food)
                .amount(0)
                .build();
    }

    public void decreaseStock(int quantity) {
        if (this.amount < quantity) {
            throw new GeneralException(GeneralErrorCode.OUT_OF_STOCK, this.food.getName() + "의 재고가 부족합니다.");
        }
        this.amount -= quantity;
    }

    public void increaseStock(int quantity) {
        this.amount += quantity;
    }

    public void updateFoodStock(int stock){
        this.amount = stock;
    }
}
