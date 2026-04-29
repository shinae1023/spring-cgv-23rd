package cgv_23rd.ceos.dto.food.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FoodOrderItemRequestDto(
        @NotNull @Positive Long foodId,
        @NotNull @Min(1) Integer quantity
) {}
