package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.controller.support.ControllerTestSupport;
import cgv_23rd.ceos.dto.review.request.ReviewRequestDto;
import cgv_23rd.ceos.dto.review.response.ReviewResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReviewSuccess() throws Exception {
        ReviewRequestDto request = new ReviewRequestDto(1L, 4.5, "재밌었습니다.");

        mockMvc.perform(post("/api/reviews")
                        .with(authenticatedUser())
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("리뷰 작성 성공"));

        verify(reviewService).createReview(any(Long.class), any(ReviewRequestDto.class));
    }

    @Test
    @DisplayName("리뷰 생성은 인증이 필요함")
    void createReviewUnauthorized() throws Exception {
        ReviewRequestDto request = new ReviewRequestDto(1L, 4.5, "재밌었습니다.");

        mockMvc.perform(post("/api/reviews")
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_4011"));
    }

    @Test
    @DisplayName("영화 리뷰 조회 성공")
    void getMovieReviewsSuccess() throws Exception {
        given(reviewQueryService.getMovieReviews(1L)).willReturn(List.of(
                ReviewResponseDto.builder()
                        .reviewId(1L)
                        .username("tester")
                        .rate(4.5)
                        .content("재밌었습니다.")
                        .build()
        ));

        mockMvc.perform(get("/api/reviews/movie/{movieId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("리뷰 조회 성공"))
                .andExpect(jsonPath("$.result[0].username").value("tester"));
    }
}
