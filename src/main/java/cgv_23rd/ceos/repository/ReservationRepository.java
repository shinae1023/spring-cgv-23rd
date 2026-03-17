package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.reservation.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {
}
