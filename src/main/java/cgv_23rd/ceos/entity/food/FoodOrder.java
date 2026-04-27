package cgv_23rd.ceos.entity.food;

import cgv_23rd.ceos.entity.BaseEntity;
import cgv_23rd.ceos.entity.enums.FoodOrderStatus;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        indexes = {
                @Index(name = "idx_food_order_status_created_at", columnList = "status, createdAt")
        }
)
public class FoodOrder extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    private FoodOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(length = 100)
    private String paymentId;

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
                .status(FoodOrderStatus.대기)
                .paymentStatus(PaymentStatus.READY)
                .paymentId(null)
                .totalPrice(0)
                .foodOrderItems(new ArrayList<>())
                .build();
    }

    public void assignPaymentId(String paymentId) {
        if (this.status != FoodOrderStatus.대기) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY);
        }
        if (this.paymentStatus == PaymentStatus.PROCESSING || this.paymentStatus == PaymentStatus.PAID) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_ALREADY_PROCESSED, "이미 결제가 진행 중이거나 완료된 주문입니다.");
        }
        if (this.paymentId != null && !this.paymentId.isBlank()) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY, "이미 결제 식별자가 할당된 주문입니다.");
        }
        this.paymentId = paymentId;
        this.paymentStatus = PaymentStatus.PROCESSING;
    }

    public void confirm() {
        if (this.status != FoodOrderStatus.대기) {
            throw new GeneralException(
                    GeneralErrorCode.PAYMENT_ALREADY_PROCESSED,
                    "대기 상태의 주문만 완료할 수 있습니다."
            );
        }
        if (this.paymentStatus != PaymentStatus.PAID) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY, "결제 완료 상태의 주문만 확정할 수 있습니다.");
        }
        this.status = FoodOrderStatus.완료;
    }

    public void cancel() {
        if (this.status == FoodOrderStatus.취소) {
            throw new GeneralException(GeneralErrorCode.FOOD_ORDER_ALREADY_CANCELED);
        }

        if (this.status == FoodOrderStatus.완료) {
            throw new GeneralException(
                    GeneralErrorCode.FOOD_ORDER_CANNOT_CANCEL);
        }

        this.status = FoodOrderStatus.취소;
    }

    public void cancelAfterPaymentCancellation() {
        if (this.status == FoodOrderStatus.취소) {
            throw new GeneralException(GeneralErrorCode.FOOD_ORDER_ALREADY_CANCELED);
        }

        if (this.paymentStatus != PaymentStatus.CANCELLED) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_CANCELLABLE, "결제 취소가 완료된 주문만 취소할 수 있습니다.");
        }

        this.status = FoodOrderStatus.취소;
    }

    public void markPaymentPaid() {
        validatePaymentIdExists();
        this.paymentStatus = PaymentStatus.PAID;
    }

    public void markPaymentFailed() {
        validatePaymentIdExists();
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public void markPaymentUnknown() {
        validatePaymentIdExists();
        this.paymentStatus = PaymentStatus.UNKNOWN;
    }

    public void markPaymentCancelled() {
        validatePaymentIdExists();
        this.paymentStatus = PaymentStatus.CANCELLED;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void addItem(Food food, int quantity) {
        if (this.status != FoodOrderStatus.대기) {
            throw new GeneralException(GeneralErrorCode.FOOD_ORDER_INVALID_STATE);
        }

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

    private void validatePaymentIdExists() {
        if (this.paymentId == null || this.paymentId.isBlank()) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY, "결제 식별자가 없는 주문입니다.");
        }
    }
}
