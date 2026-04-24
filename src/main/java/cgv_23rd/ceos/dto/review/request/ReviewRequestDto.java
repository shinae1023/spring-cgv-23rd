package cgv_23rd.ceos.dto.review.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewRequestDto(
        @NotNull Long movieId,
        @NotNull Double rate,
        @NotBlank String content
) {}
