package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.review.request.ReviewRequestDto;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.Review;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final MovieService movieService;

    // 1. 리뷰 생성
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    public void createReview(Long userId, ReviewRequestDto requestDto) {
        User user = userService.getUser(userId);
        Movie movie = movieService.getMovie(requestDto.movieId());

        validateRate(requestDto.rate());

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

    private static void validateRate(Double rate) {
        if (rate == null || rate < 0.5 || rate > 5.0 || (rate * 10) % 5 != 0) {
            throw new GeneralException(GeneralErrorCode.INVALID_PARAMETER, "별점은 0.5 ~ 5.0 사이의 0.5 단위여야 합니다.");
        }
    }

    @Recover
    public void recoverReviewCreate(ObjectOptimisticLockingFailureException e, Long userId, ReviewRequestDto requestDto) {
        throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "리뷰 저장 충돌이 발생했습니다. 다시 시도해주세요.");
    }
}
