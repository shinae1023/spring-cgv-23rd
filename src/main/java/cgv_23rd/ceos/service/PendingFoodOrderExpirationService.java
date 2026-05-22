package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.enums.FoodOrderStatus;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.repository.food.FoodOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingFoodOrderExpirationService {

    private static final long FOOD_ORDER_PENDING_MINUTES = 5L;
    private static final List<PaymentStatus> EXPIRABLE_PAYMENT_STATUSES = List.of(
            PaymentStatus.READY,
            PaymentStatus.FAILED
    );

    private final FoodOrderRepository foodOrderRepository;

    @Transactional
    public int expirePendingFoodOrders() {
        LocalDateTime expiredAt = LocalDateTime.now().minusMinutes(FOOD_ORDER_PENDING_MINUTES);

        int expiredFoodOrders = foodOrderRepository.expirePendingFoodOrders(
                FoodOrderStatus.대기,
                FoodOrderStatus.취소,
                EXPIRABLE_PAYMENT_STATUSES,
                expiredAt
        );

        log.info("Expired pending food orders. expiredFoodOrders={}", expiredFoodOrders);

        return expiredFoodOrders;
    }
}
