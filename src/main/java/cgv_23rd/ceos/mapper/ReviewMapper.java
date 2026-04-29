package cgv_23rd.ceos.mapper;

import cgv_23rd.ceos.dto.review.response.ReviewResponseDto;
import cgv_23rd.ceos.entity.movie.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponseDto toResponse(Review review) {
        return ReviewResponseDto.builder()
                .reviewId(review.getId())
                .username(review.getUser().getName())
                .rate(review.getRate())
                .content(review.getContent())
                .build();
    }
}
