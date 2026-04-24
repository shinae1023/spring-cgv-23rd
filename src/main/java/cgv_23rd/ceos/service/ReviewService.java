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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    public void createReview(Long userId, ReviewRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Movie movie = movieRepository.findById(requestDto.movieId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));

        if (reviewRepository.existsByUserAndMovie(user, movie)) {
            throw new GeneralException(GeneralErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.create(user, movie, requestDto.rate(), requestDto.content());

        try {
            reviewRepository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(GeneralErrorCode.REVIEW_ALREADY_EXISTS);
        }

        movie.registerReview(requestDto.rate());
    }

    @Recover
    public void recoverReviewCreate(ObjectOptimisticLockingFailureException e, Long userId, ReviewRequestDto requestDto) {
        throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "리뷰 저장 충돌이 발생했습니다. 다시 시도해주세요.");
    }

    // 2. 특정 영화 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getMovieReviews(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND);
        }

        List<Review> reviews = reviewRepository.findAllByMovieId(movieId);

        return reviews.stream()
                .map(review -> ReviewResponseDto.builder()
                        .reviewId(review.getId())
                        .username(review.getUser().getName())
                        .rate(review.getRate())
                        .content(review.getContent())
                        .build())
                .collect(Collectors.toList());
    }
}
