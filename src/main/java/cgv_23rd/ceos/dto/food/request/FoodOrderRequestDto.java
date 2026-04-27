package cgv_23rd.ceos.dto.food.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record FoodOrderRequestDto(
        @NotNull @Positive Long theaterId,
        @NotEmpty List<@Valid FoodOrderItemRequestDto> items
) {}
