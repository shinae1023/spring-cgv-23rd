package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.MovieStatistics;
import cgv_23rd.ceos.entity.movie.Review;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.dto.review.request.ReviewRequestDto;
import cgv_23rd.ceos.dto.review.response.ReviewResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.MovieRepository;
import cgv_23rd.ceos.repository.ReviewRepository;
import cgv_23rd.ceos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;

    // 1. 리뷰 생성
    public ApiResponse<Void> createReview(Long userId, ReviewRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Movie movie = movieRepository.findById(requestDto.movieId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));

        if (reviewRepository.existsByUserAndMovie(user, movie)) {
            throw new GeneralException(GeneralErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .user(user)
                .movie(movie)
                .rate(requestDto.rate())
                .content(requestDto.content())
                .build();

        reviewRepository.save(review);

        MovieStatistics statistics = movie.getMovieStatistics();
        if(statistics != null){
            statistics.addReviewRating(requestDto.rate());
        }

        return ApiResponse.onSuccess("리뷰 작성 성공");
    }

    // 2. 특정 영화 리뷰 조회
    @Transactional(readOnly = true)
    public ApiResponse<List<ReviewResponseDto>> getMovieReviews(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND);
        }

        List<Review> reviews = reviewRepository.findAllByMovieId(movieId);

        List<ReviewResponseDto> responseDtos = reviews.stream()
                .map(review -> ReviewResponseDto.builder()
                        .reviewId(review.getId())
                        .username(review.getUser().getName())
                        .rate(review.getRate())
                        .content(review.getContent())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.onSuccess("리뷰 조회 성공", responseDtos);
    }
}
