package cgv_23rd.ceos.entity.food;

import cgv_23rd.ceos.entity.BaseEntity;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.enums.FoodOrderStatus;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class FoodOrder extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    private FoodOrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @OneToMany(mappedBy = "foodOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodOrderItem> foodOrderItems = new ArrayList<>();

    public static FoodOrder create(User user, Theater theater) {
        return FoodOrder.builder()
                .user(user)
                .theater(theater)
                .status(FoodOrderStatus.완료)
                .totalPrice(0)
                .foodOrderItems(new ArrayList<>())
                .build();
    }

    public void addItem(Food food, int quantity) {
        int itemTotalPrice = food.getPrice() * quantity;

        FoodOrderItem orderItem = FoodOrderItem.builder()
                .foodOrder(this)
                .food(food)
                .quantity(quantity)
                .price(itemTotalPrice)
                .build();

        this.foodOrderItems.add(orderItem);
        this.totalPrice += itemTotalPrice;
    }

}
