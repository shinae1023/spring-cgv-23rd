package cgv_23rd.ceos.service.query;

import cgv_23rd.ceos.dto.movie.response.ActorResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieDetailResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieResponseDto;
import cgv_23rd.ceos.entity.enums.MovieStatus;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.mapper.MovieMapper;
import cgv_23rd.ceos.repository.movie.MovieActorRepository;
import cgv_23rd.ceos.repository.movie.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieQueryService {

    private final MovieRepository movieRepository;
    private final MovieActorRepository movieActorRepository;
    private final MovieMapper movieMapper;

    public List<MovieResponseDto> getMovieList() {
        return movieRepository.findAllByStatusOrderByReservationRateDesc(MovieStatus.상영중).stream()
                .map(movieMapper::toResponse)
                .toList();
    }

    public MovieDetailResponseDto getMovieDetail(Long movieId) {
        Movie movie = movieRepository.findDetailById(movieId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));
        return movieMapper.toDetailResponse(movie);
    }

    public List<ActorResponseDto> getMovieActors(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));

        return movieActorRepository.findAllByMovieWithActor(movie).stream()
                .map(movieMapper::toActorResponse)
                .toList();
    }
}
