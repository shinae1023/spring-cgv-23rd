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
    public ApiResponse<Void> createFoodOrder(Long userId, FoodOrderRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Theater theater = theaterRepository.findById(requestDto.theaterId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND));

        FoodOrder foodOrder = FoodOrder.builder()
                .user(user)
                .theater(theater)
                .status(FoodOrderStatus.완료)
                .createdAt(LocalDateTime.now())
                .foodOrderItems(new ArrayList<>())
                .build();

        int totalOrderPrice = 0;

        for (FoodOrderItemRequestDto itemDto : requestDto.items()) {
            Food food = foodRepository.findById(itemDto.foodId())
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_NOT_FOUND));

            TheaterFood theaterFood = theaterFoodRepository.findByTheaterAndFood(theater, food)
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_FOOD_NOT_FOUND));

            if (theaterFood.getAmount() < itemDto.quantity()) {
                throw new GeneralException(GeneralErrorCode.OUT_OF_STOCK, food.getName() + "의 재고가 부족합니다.");
            }

            for (int i = 0; i < itemDto.quantity(); i++) {
                theaterFood.decreaseAmount();
            }

            int itemTotalPrice = food.getPrice() * itemDto.quantity();
            totalOrderPrice += itemTotalPrice;

            FoodOrderItem orderItem = FoodOrderItem.builder()
                    .foodOrder(foodOrder)
                    .food(food)
                    .quantity(itemDto.quantity())
                    .price(itemTotalPrice)
                    .build();

            foodOrder.getFoodOrderItems().add(orderItem);
        }

        foodOrder.updateTotalPrice(totalOrderPrice);
        foodOrderRepository.save(foodOrder);

        return ApiResponse.onSuccess("음식 주문 성공");
    }

    // 2. 주문 내역 확인
    @Transactional(readOnly = true)
    public ApiResponse<List<FoodOrderResponseDto>> getFoodOrderList(Long userId) {
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

        return ApiResponse.onSuccess("음식 주문 내역 조회 성공", responseDtos);
    }

    public ApiResponse<Void> createFood(FoodCreateRequestDto requestDto) {

        Food food = Food.builder()
                .name(requestDto.name())
                .price(requestDto.price())
                .build();

        foodRepository.save(food);

        List<Theater> allTheaters = theaterRepository.findAll();

        List<TheaterFood> theaterFoods = allTheaters.stream()
                .map(theater -> TheaterFood.builder()
                        .theater(theater)
                        .food(food)
                        .amount(100)
                        .build())
                .collect(Collectors.toList());

        theaterFoodRepository.saveAll(theaterFoods);

        return ApiResponse.onSuccess("신규 음식 등록 및 전 지점 반영 성공");
    }
}