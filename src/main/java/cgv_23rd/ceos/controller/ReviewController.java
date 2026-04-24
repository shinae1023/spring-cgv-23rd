package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.review.request.ReviewRequestDto;
import cgv_23rd.ceos.dto.review.response.ReviewResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.security.UserDetailsImpl;
import cgv_23rd.ceos.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
@Tag(name = "리뷰 API")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("")
    @Operation(summary = "리뷰 생성 API", description = "영화에 대한 별점과 리뷰를 작성합니다. (1인 1리뷰 제한)")
    public ApiResponse<Void> createReview(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReviewRequestDto requestDto) {
        Long userId = userDetails.getUser().getId();
        reviewService.createReview(userId, requestDto);
        return ApiResponse.onSuccess("리뷰 작성 성공");
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "특정 영화 리뷰 조회 API", description = "해당 영화에 작성된 모든 리뷰를 조회합니다.")
    public ApiResponse<List<ReviewResponseDto>> getMovieReviews(
            @PathVariable Long movieId) {
        return ApiResponse.onSuccess("리뷰 조회 성공",reviewService.getMovieReviews(movieId));
    }
}