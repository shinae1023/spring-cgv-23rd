package cgv_23rd.ceos.dto.food.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FoodOrderItemRequestDto(
        @NotNull Long foodId,
        @NotNull @Min(1) Integer quantity
) {}
