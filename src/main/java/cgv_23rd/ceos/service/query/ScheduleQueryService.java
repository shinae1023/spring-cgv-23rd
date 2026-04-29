package cgv_23rd.ceos.service.query;

import cgv_23rd.ceos.dto.schedule.response.ScheduleResponseDto;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.mapper.ScheduleMapper;
import cgv_23rd.ceos.repository.movie.MovieScreenRepository;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleQueryService {

    private final TheaterRepository theaterRepository;
    private final MovieScreenRepository movieScreenRepository;
    private final ScheduleMapper scheduleMapper;

    public List<ScheduleResponseDto> getSchedules(Long theaterId, LocalDate targetDate) {
        theaterRepository.findById(theaterId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND));

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

        List<MovieScreen> schedules = movieScreenRepository.findByTheaterIdAndDateBetween(
                theaterId, startOfDay, endOfDay
        );

        return schedules.stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }
}
