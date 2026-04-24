package cgv_23rd.ceos.dto.food.request;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FoodOrderRequestDto(
        @NotNull Long theaterId,
        @NotEmpty List<FoodOrderItemRequestDto> items
) {}
