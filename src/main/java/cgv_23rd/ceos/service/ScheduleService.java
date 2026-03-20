package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.theater.Screen;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.dto.schedule.request.ScheduleCreateRequestDto;
import cgv_23rd.ceos.dto.schedule.response.ScheduleResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.MovieRepository;
import cgv_23rd.ceos.repository.MovieScreenRepository;
import cgv_23rd.ceos.repository.ScreenRepository;
import cgv_23rd.ceos.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleService {

    private final TheaterRepository theaterRepository;
    private final MovieRepository movieRepository;
    private final MovieScreenRepository movieScreenRepository;
    private final ScreenRepository screenRepository;

    // 1. 극장별 상영 시간표 등록
    public ApiResponse<Void> createSchedule(Long theaterId, ScheduleCreateRequestDto requestDto) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND));

        Movie movie = movieRepository.findById(requestDto.movieId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIE_NOT_FOUND));

        Screen screen = screenRepository.findByIdWithLock(requestDto.screenId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.SCREEN_NOT_FOUND));

        // 해당 상영관이 극장 내에 있는지 확인
        if (!screen.getTheater().getId().equals(theaterId)) {
            throw new GeneralException(GeneralErrorCode.SCREEN_THEATER_MISMATCH);
        }

        // 종료 시간이 시작 시간보다 빨라서는 안 됨
        if (!requestDto.endAt().isAfter(requestDto.startAt())) {
            throw new GeneralException(GeneralErrorCode.INVALID_SCHEDULE_TIME);
        }

        // 상영 시간 겹침 검증
        boolean isOverlapping = movieScreenRepository.existsOverlappingSchedule(
                requestDto.screenId(), requestDto.startAt(), requestDto.endAt()
        );

        if (isOverlapping) {
            throw new GeneralException(GeneralErrorCode.SCHEDULE_OVERLAPPED);
        }

        MovieScreen movieScreen = MovieScreen.builder()
                .movie(movie)
                .screen(screen)
                .sequence(requestDto.sequence())
                .startAt(requestDto.startAt())
                .endAt(requestDto.endAt())
                .build();

        movieScreenRepository.save(movieScreen);

        return ApiResponse.onSuccess("상영 시간표 등록 성공");
    }

    // 2. 극장별 상영 시간표 조회
    @Transactional(readOnly = true)
    public ApiResponse<List<ScheduleResponseDto>> getSchedules(Long theaterId, LocalDate targetDate) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND));

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

        List<MovieScreen> schedules = movieScreenRepository.findByTheaterIdAndDateBetween(
                theaterId, startOfDay, endOfDay
        );

        List<ScheduleResponseDto> responseDtos = schedules.stream()
                .map(ms -> ScheduleResponseDto.builder()
                        .movieScreenId(ms.getId())
                        .movieId(ms.getMovie().getId())
                        .movieTitle(ms.getMovie().getTitle())
                        .screenId(ms.getScreen().getId())
                        .screenName(ms.getScreen().getName())
                        .sequence(ms.getSequence())
                        .startAt(ms.getStartAt())
                        .endAt(ms.getEndAt())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.onSuccess("상영 시간표 조회 성공", responseDtos);
    }
}
