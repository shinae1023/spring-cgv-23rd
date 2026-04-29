package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.food.request.FoodOrderItemRequestDto;
import cgv_23rd.ceos.dto.food.request.FoodOrderRequestDto;
import cgv_23rd.ceos.entity.food.Food;
import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.entity.food.FoodOrderItem;
import cgv_23rd.ceos.entity.food.TheaterFood;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.food.FoodOrderRepository;
import cgv_23rd.ceos.repository.food.FoodRepository;
import cgv_23rd.ceos.repository.food.TheaterFoodRepository;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FoodOrderService {
    private final FoodRepository foodRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final TheaterRepository theaterRepository;
    private final TheaterFoodRepository theaterFoodRepository;
    private final UserService userService;

    // 1. 음식 주문
    public Long createFoodOrder(Long userId, FoodOrderRequestDto requestDto) {
        User user = userService.getUser(userId);
        Theater theater = getTheater(requestDto.theaterId());

        FoodOrder foodOrder = FoodOrder.create(user, theater);

        for (FoodOrderItemRequestDto itemDto : requestDto.items()) {
            Food food = getFood(itemDto.foodId());

            theaterFoodRepository.findByTheaterAndFood(theater, food)
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_FOOD_NOT_FOUND));

            foodOrder.addItem(food, itemDto.quantity());
        }

        foodOrderRepository.save(foodOrder);
        return foodOrder.getId();
    }

    @Transactional(readOnly = true)
    public FoodOrder getFoodOrder(Long orderId) {
        return foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public FoodOrder getOwnedFoodOrder(Long userId, Long orderId) {
        FoodOrder order = foodOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!order.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        return order;
    }

    @Transactional
    public void preparePayment(Long userId, Long orderId, String paymentId) {
        FoodOrder foodOrder = foodOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!foodOrder.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        foodOrder.assignPaymentId(paymentId);
    }

    @Transactional
    public void confirmOrderAndDeductStock(Long userId, Long orderId) {
        FoodOrder foodOrder = foodOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!foodOrder.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        if (foodOrder.getStatus() != cgv_23rd.ceos.entity.enums.FoodOrderStatus.대기) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        List<FoodOrderItem> sortedItems = foodOrder.getFoodOrderItems().stream()
                .sorted(Comparator.comparing(item -> item.getFood().getId()))
                .toList();

        for (FoodOrderItem item : sortedItems) {
            // 비관적 락으로 재고 조회하여 동시성 제어
            TheaterFood theaterFood = theaterFoodRepository.findByTheaterAndFoodWithLock(foodOrder.getTheater(), item.getFood())
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_FOOD_NOT_FOUND));

            theaterFood.decreaseStock(item.getQuantity());
        }
        foodOrder.markPaymentPaid();
        foodOrder.confirm();
    }

    @Transactional
    public void markPaymentFailed(Long userId, Long orderId) {
        FoodOrder foodOrder = foodOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!foodOrder.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        foodOrder.markPaymentFailed();
    }

    @Transactional
    public void markPaymentUnknown(Long userId, Long orderId) {
        FoodOrder foodOrder = foodOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!foodOrder.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        foodOrder.markPaymentUnknown();
    }

    @Transactional
    public void cancelOrderAfterPaymentCancellation(Long userId, Long orderId) {
        FoodOrder foodOrder = foodOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!foodOrder.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        List<FoodOrderItem> sortedItems = foodOrder.getFoodOrderItems().stream()
                .sorted(Comparator.comparing(item -> item.getFood().getId()))
                .toList();

        for (FoodOrderItem item : sortedItems) {
            TheaterFood theaterFood = theaterFoodRepository.findByTheaterAndFoodWithLock(foodOrder.getTheater(), item.getFood())
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_FOOD_NOT_FOUND));

            theaterFood.increaseStock(item.getQuantity());
        }

        foodOrder.markPaymentCancelled();
        foodOrder.cancelAfterPaymentCancellation();
    }

    @Transactional
    public void cancelPendingOrder(Long userId, Long orderId) {
        FoodOrder foodOrder = foodOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!foodOrder.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        foodOrder.cancel();
    }

    private Theater getTheater(Long theaterId) {
        return theaterRepository.findById(theaterId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND));
    }

    private Food getFood(Long foodId) {
        return foodRepository.findById(foodId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_NOT_FOUND));
    }

    private void validateUserExists(Long userId) {
        userService.getUser(userId);
    }
}
