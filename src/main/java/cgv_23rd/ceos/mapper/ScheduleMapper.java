package cgv_23rd.ceos.mapper;

import cgv_23rd.ceos.dto.schedule.response.ScheduleResponseDto;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import org.springframework.stereotype.Component;

@Component
public class ScheduleMapper {

    public ScheduleResponseDto toResponse(MovieScreen movieScreen) {
        return ScheduleResponseDto.builder()
                .movieScreenId(movieScreen.getId())
                .movieId(movieScreen.getMovie().getId())
                .movieTitle(movieScreen.getMovie().getTitle())
                .screenId(movieScreen.getScreen().getId())
                .screenName(movieScreen.getScreen().getName())
                .sequence(movieScreen.getSequence())
                .startAt(movieScreen.getStartAt())
                .endAt(movieScreen.getEndAt())
                .build();
    }
}
