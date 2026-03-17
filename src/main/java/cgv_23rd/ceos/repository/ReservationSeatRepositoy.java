package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.reservation.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatRepositoy extends JpaRepository<ReservationSeat,Long> {
}
