package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.enums.FoodOrderStatus;
import cgv_23rd.ceos.entity.food.*;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.dto.food.request.FoodCreateRequestDto;
import cgv_23rd.ceos.dto.food.request.FoodOrderItemRequestDto;
import cgv_23rd.ceos.dto.food.request.FoodOrderRequestDto;
import cgv_23rd.ceos.dto.food.response.FoodOrderItemResponseDto;
import cgv_23rd.ceos.dto.food.response.FoodOrderResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    public void createFoodOrder(Long userId, FoodOrderRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Theater theater = theaterRepository.findById(requestDto.theaterId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND));

        FoodOrder foodOrder = FoodOrder.create(user, theater);

        for (FoodOrderItemRequestDto itemDto : requestDto.items()) {
            Food food = foodRepository.findById(itemDto.foodId())
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_NOT_FOUND));

            // 재고 엔티티를 락 걸고 조회하여 직접 차감
            TheaterFood theaterFood = theaterFoodRepository.findByTheaterAndFoodWithLock(theater, food)
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_FOOD_NOT_FOUND));

            theaterFood.decreaseStock(itemDto.quantity());

            // 주문에 항목 추가
            foodOrder.addItem(food, itemDto.quantity());
        }

        foodOrderRepository.save(foodOrder);

    }

    // 2. 주문 내역 확인
    @Transactional(readOnly = true)
    public List<FoodOrderResponseDto> getFoodOrderList(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        List<FoodOrderResponseDto> responseDtos = user.getFoodOrders().stream()
                .map(order -> FoodOrderResponseDto.builder()
                        .orderId(order.getId())
                        .theaterName(order.getTheater().getName())
                        .totalPrice(order.getTotalPrice())
                        .status(order.getStatus())
                        .createdAt(order.getCreatedAt())
                        .items(order.getFoodOrderItems().stream()
                                .map(item -> FoodOrderItemResponseDto.builder()
                                        .foodName(item.getFood().getName())
                                        .quantity(item.getQuantity())
                                        .price(item.getPrice())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return responseDtos;
    }

}