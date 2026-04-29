package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.like.MovieLike;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.MovieLikeRepository;
import cgv_23rd.ceos.repository.movie.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final MovieLikeRepository movieLikeRepository;
    private final UserService userService;

    // 5. 영화 찜
    @Transactional
    public void likeMovie(Long userId, Long movieId) {
        User user = userService.getUser(userId);
        Movie movie = getMovie(movieId);

        if (movieLikeRepository.findMovieLikeByUserAndMovie(user, movie) != null) {
            return;
        }

        try {
            movieLikeRepository.save(MovieLike.of(user, movie));
        } catch (DataIntegrityViolationException e) {
            if (movieLikeRepository.findMovieLikeByUserAndMovie(user, movie) != null) {
                return;
            }
            throw e;
        }
    }

    @Transactional
    public void unlikeMovie(Long userId, Long movieId) {
        User user = userService.getUser(userId);
        Movie movie = getMovie(movieId);

        MovieLike movieLike = movieLikeRepository.findMovieLikeByUserAndMovie(user, movie);
        if (movieLike != null) {
            movieLikeRepository.delete(movieLike);
        }
    }

    @Transactional(readOnly = true)
    public Movie getMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));
    }

}
