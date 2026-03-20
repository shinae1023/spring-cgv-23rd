package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.enums.MovieStatus;
import cgv_23rd.ceos.entity.like.MovieLike;
import cgv_23rd.ceos.entity.movie.*;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.dto.movie.request.MovieRequestDto;
import cgv_23rd.ceos.dto.movie.response.ActorResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieDetailResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final MovieActorRepository movieActorRepository;
    private final MovieLikeRepository movieLikeRepository;
    private final UserRepository userRepository;
    private final MovieStatisticsRepository movieStatisticsRepository;

    // 1. 영화 생성
    @Transactional
    public ApiResponse<Long> createMovie(MovieRequestDto requestDto){

        if (requestDto.closeDate().isBefore(requestDto.openDate())) {
            throw new GeneralException(GeneralErrorCode.INVALID_MOVIE_DATE);
        }

        LocalDate today = LocalDate.now();
        MovieStatus calculatedStatus;

        if (today.isBefore(requestDto.openDate())) {
            calculatedStatus = MovieStatus.예정;
        } else if (today.isAfter(requestDto.closeDate())) {
            calculatedStatus = MovieStatus.종료;
        } else {
            calculatedStatus = MovieStatus.상영중;
        }

        Movie movie = Movie.builder()
                .title(requestDto.title())
                .description(requestDto.description())
                .status(calculatedStatus)
                .openDate(requestDto.openDate())
                .closeDate(requestDto.closeDate())
                .build();

        movie.createDefaultStatistics();

        movieRepository.save(movie);

        return ApiResponse.onSuccess("영화 생성 성공", movie.getId());
    }

    // 2. 현재 상영중인 영화 목록 조회 (제목, 썸네일)
    @Transactional(readOnly = true)
    public ApiResponse<List<MovieResponseDto>> getMovieList(){
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
        return ApiResponse.onSuccess("현재 상영중 영화 리스트 조회 성공",movieResponseDtoList);
    }

    // 3. 영화 상세 조회 (제목, 개봉일, 예매율, 누적관객, 설명, 에그지수, 사진들)
    @Transactional(readOnly = true)
    public ApiResponse<MovieDetailResponseDto> getMovieDetail(Long movieId){
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

        return ApiResponse.onSuccess("영화 상세 조회 성공", movieDetailResponseDto);
    }

    // 4. 출연진 조회
    @Transactional(readOnly = true)
    public ApiResponse<List<ActorResponseDto>> getMovieActors(Long movieId){
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
        return ApiResponse.onSuccess("영화 출연진 조회 성공", actorResponseDtos);
    }

    // 5. 영화 찜
    @Transactional
    public ApiResponse<Void> toggleMovieLike(Long userId, Long movieId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));


        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));

        MovieLike movieLike = movieLikeRepository.findMovieLikeByUserAndMovie(user,movie);

        if(movieLike==null){
            movieLike = MovieLike.builder()
                    .user(user)
                    .movie(movie)
                    .build();
            movieLikeRepository.save(movieLike);
            return ApiResponse.onSuccess("영화 id = " + movieId + " 찜 성공");
        }
        else{
            movieLikeRepository.delete(movieLike);
            return ApiResponse.onSuccess("영화 id = " + movieId + " 찜 삭제 성공");
        }
    }

}
