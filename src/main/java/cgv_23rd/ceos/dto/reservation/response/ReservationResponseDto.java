package cgv_23rd.ceos.dto.reservation.response;

import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ReservationResponseDto(
        Long reservationId,
        String movieTitle,
        String theaterName,
        String screenName,
        LocalDateTime startAt,
        List<String> seatInfo,
        Integer totalPrice,
        ReservationStatus status,
        PaymentStatus paymentStatus,
        LocalDateTime reservationAt
) {}
