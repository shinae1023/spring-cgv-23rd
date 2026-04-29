package cgv_23rd.ceos.mapper;

import cgv_23rd.ceos.dto.food.response.FoodOrderItemResponseDto;
import cgv_23rd.ceos.dto.food.response.FoodOrderResponseDto;
import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.entity.food.FoodOrderItem;
import org.springframework.stereotype.Component;

@Component
public class FoodOrderMapper {

    public FoodOrderResponseDto toResponse(FoodOrder order) {
        return FoodOrderResponseDto.builder()
                .orderId(order.getId())
                .theaterName(order.getTheater().getName())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt())
                .items(order.getFoodOrderItems().stream()
                        .map(this::toItemResponse)
                        .toList())
                .build();
    }

    public FoodOrderItemResponseDto toItemResponse(FoodOrderItem item) {
        return FoodOrderItemResponseDto.builder()
                .foodName(item.getFood().getName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }
}
