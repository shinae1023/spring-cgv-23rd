package cgv_23rd.ceos.dto.reservation.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReservationRequestDto(
        @NotNull Long movieScreenId,
        @NotEmpty List<Long> seatIds
) {}