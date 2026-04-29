package cgv_23rd.ceos.dto.review.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewRequestDto(
        @NotNull Long movieId,
        @NotNull
        @DecimalMin("0.5")
        @DecimalMax("5.0")
        Double rate,
        @NotBlank String content
) {}
