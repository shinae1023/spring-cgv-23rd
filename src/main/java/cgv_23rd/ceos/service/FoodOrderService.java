package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.food.request.FoodOrderItemRequestDto;
import cgv_23rd.ceos.dto.food.request.FoodOrderRequestDto;
import cgv_23rd.ceos.dto.food.response.FoodOrderItemResponseDto;
import cgv_23rd.ceos.dto.food.response.FoodOrderResponseDto;
import cgv_23rd.ceos.entity.food.Food;
import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.entity.food.FoodOrderItem;
import cgv_23rd.ceos.entity.food.TheaterFood;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.*;
import cgv_23rd.ceos.repository.food.FoodOrderRepository;
import cgv_23rd.ceos.repository.food.FoodRepository;
import cgv_23rd.ceos.repository.food.TheaterFoodRepository;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FoodOrderService {
    private final FoodRepository foodRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final UserRepository userRepository;
    private final TheaterRepository theaterRepository;
    private final TheaterFoodRepository theaterFoodRepository;

    // 1. 음식 주문
    public Long createFoodOrder(Long userId, FoodOrderRequestDto requestDto) {
        User user = getUser(userId);
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

    @Transactional(noRollbackFor = GeneralException.class)
    public void preparePayment(Long userId, Long orderId, String paymentId) {
        FoodOrder foodOrder = foodOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!foodOrder.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        foodOrder.assignPaymentId(paymentId);
    }

    @Transactional(noRollbackFor = GeneralException.class)
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

    @Transactional(noRollbackFor = GeneralException.class)
    public void markPaymentFailed(Long userId, Long orderId) {
        FoodOrder foodOrder = foodOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!foodOrder.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        foodOrder.markPaymentFailed();
    }

    @Transactional(noRollbackFor = GeneralException.class)
    public void markPaymentUnknown(Long userId, Long orderId) {
        FoodOrder foodOrder = foodOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!foodOrder.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        foodOrder.markPaymentUnknown();
    }

    @Transactional(noRollbackFor = GeneralException.class)
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

    @Transactional(noRollbackFor = GeneralException.class)
    public void cancelPendingOrder(Long userId, Long orderId) {
        FoodOrder foodOrder = foodOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));

        validateUserExists(userId);

        if (!foodOrder.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        foodOrder.cancel();
    }

    // 2. 주문 내역 확인
    @Transactional(readOnly = true)
    public List<FoodOrderResponseDto> getFoodOrderList(Long userId) {
        validateUserExists(userId);

        List<FoodOrder> orders = foodOrderRepository.findAllByUserIdWithDetails(userId);

        //foodOderItems 헬퍼 메서드를 반환하는 코드로 리팩토링
        return orders.stream()
                .map(this::toFoodOrderResponse)
                .collect(Collectors.toList());
    }

    // Helper Method
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));
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
        if (!userRepository.existsById(userId)) {
            throw new GeneralException(GeneralErrorCode.USER_NOT_FOUND);
        }
    }

    private FoodOrderResponseDto toFoodOrderResponse(FoodOrder order) {
        return FoodOrderResponseDto.builder()
                .orderId(order.getId())
                .theaterName(order.getTheater().getName())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt())
                .items(order.getFoodOrderItems().stream()
                        .map(this::toFoodOrderItemResponse)
                        .toList())
                .build();
    }

    private FoodOrderItemResponseDto toFoodOrderItemResponse(FoodOrderItem item) {
        return FoodOrderItemResponseDto.builder()
                .foodName(item.getFood().getName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }
}
