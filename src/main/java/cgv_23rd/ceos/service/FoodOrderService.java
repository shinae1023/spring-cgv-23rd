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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Theater theater = theaterRepository.findById(requestDto.theaterId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND));

        FoodOrder foodOrder = FoodOrder.create(user, theater);

        for (FoodOrderItemRequestDto itemDto : requestDto.items()) {
            Food food = foodRepository.findById(itemDto.foodId())
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_NOT_FOUND));

            // 재고 엔티티를 락 걸고 조회하여 직접 차감
            TheaterFood theaterFood = theaterFoodRepository.findByTheaterAndFoodWithLock(theater, food)
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_FOOD_NOT_FOUND));

            // 주문에 항목 추가
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

    @Transactional
    public void confirmOrderAndDeductStock(FoodOrder foodOrder) {
        for (FoodOrderItem item : foodOrder.getFoodOrderItems()) {
            // 비관적 락으로 재고 조회하여 동시성 제어
            TheaterFood theaterFood = theaterFoodRepository.findByTheaterAndFoodWithLock(foodOrder.getTheater(), item.getFood())
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_FOOD_NOT_FOUND));

            theaterFood.decreaseStock(item.getQuantity());
        }
        foodOrder.confirm();
    }

    // 주문 취소 처리
    @Transactional
    public void cancelOrder(FoodOrder foodOrder) {
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