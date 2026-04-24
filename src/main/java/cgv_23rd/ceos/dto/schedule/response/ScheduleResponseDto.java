package cgv_23rd.ceos.dto.schedule.response;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ScheduleResponseDto(
        Long movieScreenId,
        Long movieId,
        String movieTitle,
        Long screenId,
        String screenName,
        Integer sequence,
        LocalDateTime startAt,
        LocalDateTime endAt
) {}
