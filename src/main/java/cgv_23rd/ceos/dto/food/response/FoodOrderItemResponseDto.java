package cgv_23rd.ceos.dto.food.response;
import lombok.Builder;

@Builder
public record FoodOrderItemResponseDto(
        String foodName,
        Integer quantity,
        Integer price
) {}
