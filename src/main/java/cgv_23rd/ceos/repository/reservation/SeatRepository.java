package cgv_23rd.ceos.repository.reservation;

import cgv_23rd.ceos.entity.theater.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat,Long> {
}
