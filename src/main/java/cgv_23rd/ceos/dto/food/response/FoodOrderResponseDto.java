package cgv_23rd.ceos.dto.food.response;

import cgv_23rd.ceos.entity.enums.FoodOrderStatus;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record FoodOrderResponseDto(
        Long orderId,
        String theaterName,
        Integer totalPrice,
        FoodOrderStatus status,
        PaymentStatus paymentStatus,
        LocalDateTime createdAt,
        List<FoodOrderItemResponseDto> items
) {}
