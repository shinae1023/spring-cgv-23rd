package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.movie.response.ActorResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieDetailResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieResponseDto;
import cgv_23rd.ceos.entity.enums.MovieStatus;
import cgv_23rd.ceos.entity.like.MovieLike;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.MovieActor;
import cgv_23rd.ceos.entity.movie.MovieImage;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.movie.MovieActorRepository;
import cgv_23rd.ceos.repository.MovieLikeRepository;
import cgv_23rd.ceos.repository.movie.MovieRepository;
import cgv_23rd.ceos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final MovieActorRepository movieActorRepository;
    private final MovieLikeRepository movieLikeRepository;
    private final UserRepository userRepository;

    // 2. 현재 상영중인 영화 목록 조회 (제목, 썸네일)
    @Transactional(readOnly = true)
    public List<MovieResponseDto> getMovieList(){
        List<Movie> movies = movieRepository.findAllByStatusOrderByReservationRateDesc(MovieStatus.상영중);
        List<MovieResponseDto> movieResponseDtoList = movies.stream()
                .map(movie -> {
                    String thumbnail = movie.getMovieImages().stream()
                            .filter(MovieImage::getIsThumbnail)
                            .map(MovieImage::getMovieImageUrl)
                            .findFirst()
                            .orElse(null);

                    return MovieResponseDto.builder()
                            .movieId(movie.getId())
                            .title(movie.getTitle())
                            .movieImageUrl(thumbnail)
                            .build();
                })
                .toList();
        return movieResponseDtoList;
    }

    // 3. 영화 상세 조회 (제목, 개봉일, 예매율, 누적관객, 설명, 에그지수, 사진들)
    @Transactional(readOnly = true)
    public MovieDetailResponseDto getMovieDetail(Long movieId){
        Movie movie = movieRepository.findDetailById(movieId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));

        String thumbnail = movie.getMovieImages().stream()
                .filter(MovieImage::getIsThumbnail)
                .map(MovieImage::getMovieImageUrl)
                .findFirst()
                .orElse(null);

        List<String> allUrls = movie.getMovieImages().stream()
                .map(MovieImage::getMovieImageUrl)
                .toList();

        MovieDetailResponseDto movieDetailResponseDto = MovieDetailResponseDto.builder()
                .title(movie.getTitle())
                .openDate(movie.getOpenDate())
                .description(movie.getDescription())
                .thumbnailUrl(thumbnail)
                .imageUrls(allUrls)
                .audienceCount(movie.getMovieStatistics().getAudienceCount())
                .reservationRate(movie.getMovieStatistics().getReservationRate())
                .averageRating(movie.getMovieStatistics().getAverageRating())
                .eggRate(movie.getMovieStatistics().getEggRate())
                .build();

        return movieDetailResponseDto;
    }

    // 4. 출연진 조회
    @Transactional(readOnly = true)
    public List<ActorResponseDto> getMovieActors(Long movieId){
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));

        List<MovieActor> movieActorcs = movieActorRepository.findAllByMovieWithActor(movie);

        List<ActorResponseDto> actorResponseDtos = movieActorcs.stream()
                .map(movieActor -> {
                    return ActorResponseDto.builder()
                            .name(movieActor.getActor().getName())
                            .role(movieActor.getActor().getRole())
                            .profileUrl(movieActor.getActor().getProfileImageUrl())
                            .build();
                })
                .toList();
        return actorResponseDtos;
    }

    // 5. 영화 찜
    @Transactional
    public void likeMovie(Long userId, Long movieId) {
        User user = getUser(userId);
        Movie movie = getMovie(movieId);

        if (movieLikeRepository.findMovieLikeByUserAndMovie(user, movie) != null) {
            return;
        }

        try {
            movieLikeRepository.save(MovieLike.of(user, movie));
        } catch (DataIntegrityViolationException e) {
            return;
        }
    }

    @Transactional
    public void unlikeMovie(Long userId, Long movieId) {
        User user = getUser(userId);
        Movie movie = getMovie(movieId);

        MovieLike movieLike = movieLikeRepository.findMovieLikeByUserAndMovie(user, movie);
        if (movieLike != null) {
            movieLikeRepository.delete(movieLike);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));
    }

    private Movie getMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));
    }

}
