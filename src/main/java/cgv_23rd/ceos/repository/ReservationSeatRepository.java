package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.reservation.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat,Long> {
    boolean existsByMovieScreenIdAndSeatIdAndReservation_Status(Long movieScreenId, Long seatId, ReservationStatus status);
}
