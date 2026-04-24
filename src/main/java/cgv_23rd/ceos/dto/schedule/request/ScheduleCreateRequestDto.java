package cgv_23rd.ceos.dto.schedule.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ScheduleCreateRequestDto(
        @NotNull Long movieId,
        @NotNull Long screenId,
        @NotNull Integer sequence,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt
) {}