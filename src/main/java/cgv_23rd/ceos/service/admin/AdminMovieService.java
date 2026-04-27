package cgv_23rd.ceos.service.admin;

import cgv_23rd.ceos.dto.movie.request.MovieRequestDto;
import cgv_23rd.ceos.dto.schedule.request.ScheduleCreateRequestDto;
import cgv_23rd.ceos.dto.theater.request.TheaterRequestDto;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.theater.Screen;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.movie.MovieRepository;
import cgv_23rd.ceos.repository.movie.MovieScreenRepository;
import cgv_23rd.ceos.repository.theater.ScreenRepository;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMovieService {

    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final MovieScreenRepository movieScreenRepository;
    private final ScreenRepository screenRepository;

    // 4. 극장 생성
    @Transactional
    public void createTheater(TheaterRequestDto requestDto){
        Theater theater = Theater.builder()
                .name(requestDto.name())
                .address(requestDto.address())
                .region(requestDto.region())
                .isAvailable(true)
                .description(requestDto.description())
                .imageUrl(requestDto.imageUrl())
                .build();

        theaterRepository.save(theater);
    }

    // 1. 영화 생성
    @Transactional
    public Long createMovie(MovieRequestDto requestDto){
        Movie movie = Movie.create(
                requestDto.title(),
                requestDto.description(),
                requestDto.openDate(),
                requestDto.closeDate()
        );

        return movieRepository.save(movie).getId();
    }

    // 1. 극장별 상영 시간표 등록
    @Transactional
    public void createSchedule(Long theaterId, ScheduleCreateRequestDto requestDto) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND));

        Movie movie = movieRepository.findById(requestDto.movieId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));

        Screen screen = screenRepository.findByIdWithLock(requestDto.screenId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.SCREEN_NOT_FOUND));

        screen.validateBelongsTo(theaterId);

        boolean isOverlapping = movieScreenRepository.existsOverlappingSchedule(
                requestDto.screenId(), requestDto.startAt(), requestDto.endAt()
        );
        if (isOverlapping) {
            throw new GeneralException(GeneralErrorCode.SCHEDULE_OVERLAPPED);
        }

        MovieScreen movieScreen = MovieScreen.create(
                screen,
                movie,
                requestDto.sequence(),
                requestDto.startAt(),
                requestDto.endAt()
        );

        movieScreenRepository.save(movieScreen);
    }
}
