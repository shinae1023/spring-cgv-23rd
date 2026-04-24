package cgv_23rd.ceos.dto.food.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FoodCreateRequestDto(
        @NotBlank String name,
        @NotNull @Min(0) Integer price
) {}