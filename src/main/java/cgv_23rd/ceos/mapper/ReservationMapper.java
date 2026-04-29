package cgv_23rd.ceos.mapper;

import cgv_23rd.ceos.dto.reservation.response.ReservationResponseDto;
import cgv_23rd.ceos.entity.reservation.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationResponseDto toResponse(Reservation reservation) {
        return ReservationResponseDto.builder()
                .reservationId(reservation.getId())
                .movieTitle(reservation.getMovieTitle())
                .theaterName(reservation.getTheaterName())
                .screenName(reservation.getScreenName())
                .startAt(reservation.getMovieScreen().getStartAt())
                .seatInfo(reservation.getSeatLabels())
                .totalPrice(reservation.getTotalPrice())
                .status(reservation.getStatus())
                .paymentStatus(reservation.getPaymentStatus())
                .reservationAt(reservation.getCreatedAt())
                .build();
    }
}
