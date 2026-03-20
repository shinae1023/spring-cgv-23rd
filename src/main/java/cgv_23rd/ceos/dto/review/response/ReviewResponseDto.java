package cgv_23rd.ceos.dto.review.response;
import lombok.Builder;

@Builder
public record ReviewResponseDto(
        Long reviewId,
        String username,
        Double rate,
        String content
) {}