package cgv_23rd.ceos.service;

import cgv_23rd.ceos.domain.enums.MovieStatus;
import cgv_23rd.ceos.domain.like.MovieLike;
import cgv_23rd.ceos.domain.movie.Movie;
import cgv_23rd.ceos.domain.movie.MovieStatistics;
import cgv_23rd.ceos.domain.user.User;
import cgv_23rd.ceos.dto.movie.request.MovieRequestDto;
import cgv_23rd.ceos.dto.movie.response.ActorResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieDetailResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieService {
    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;
    private final MovieLikeRepository movieLikeRepository;
    private final UserRepository userRepository;
    private final MovieStatisticsRepository movieStatisticsRepository;

    // 1. 영화 생성
    public ApiResponse<Long> createMovie(MovieRequestDto requestDto){
        Movie movie = Movie.builder()
                .title(requestDto.title())
                .description(requestDto.description())
                .status(MovieStatus.상영중)
                .openDate(requestDto.openDate())
                .closeDate(requestDto.closeDate())
                .build();
        MovieStatistics movieStatistics = MovieStatistics.builder()
                .movie(movie)
                .build();
        movieRepository.save(movie);
        movieStatisticsRepository.save(movieStatistics);

        return ApiResponse.onSuccess("영화 생성 성공", movie.getId());
    }

    // 2. 현재 상영중인 영화 목록 조회 (제목, 썸네일)
    public ApiResponse<List<MovieResponseDto>> getMovieList(){
        List<Movie> movies = movieRepository.findAllByStatus(MovieStatus.상영중);
        List<MovieResponseDto> movieResponseDtoList = new ArrayList<>();
        return ApiResponse.onSuccess("현재 상영중 영화 리스트 조회 성공",movieResponseDtoList);
    }

    // 3. 영화 상세 조회 (제목, 개봉일, 예매율, 누적관객, 설명, 에그지수, 사진들)
    public ApiResponse<MovieDetailResponseDto> getMovieDetail(Long movieId){
        MovieDetailResponseDto movieDetailResponseDto = MovieDetailResponseDto.builder().build();
        return ApiResponse.onSuccess("영화 상세 조회 성공", movieDetailResponseDto);
    }

    // 4. 출연진 조회
    public ApiResponse<List<ActorResponseDto>> getMovieActors(Long movieId){
        List<ActorResponseDto> actorResponseDtos = new ArrayList<>();
        return ApiResponse.onSuccess("영화 출연진 조회 성공", actorResponseDtos);
    }

    // 5. 영화 찜
    public ApiResponse<Void> toggleMovieLike(Long userId, Long movieId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.USER_NOT_FOUND,"유저 조회 불가"));

        MovieLike movieLike = movieLikeRepository.findMovieLikeByUser(user);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND,"영화 조회 불가"));

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
