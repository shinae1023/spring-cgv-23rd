package cgv_23rd.ceos.dto.food.response;
import cgv_23rd.ceos.entity.enums.FoodOrderStatus;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record FoodOrderResponseDto(
        Long orderId,
        String theaterName,
        Integer totalPrice,
        FoodOrderStatus status,
        LocalDateTime createdAt,
        List<FoodOrderItemResponseDto> items
) {}
