package cgv_23rd.ceos.domain.food;

import cgv_23rd.ceos.domain.theater.Theater;
import cgv_23rd.ceos.domain.enums.FoodOrderStatus;
import cgv_23rd.ceos.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FoodOrder {

    @Id @GeneratedValue
    private Long id;

    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    private FoodOrderStatus status;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @OneToMany(mappedBy = "foodOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodOrderItem> foodOrderItems = new ArrayList<>();

    public void updateTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }
}
