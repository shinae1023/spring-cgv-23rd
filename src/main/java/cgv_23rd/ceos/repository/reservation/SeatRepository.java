package cgv_23rd.ceos.repository.reservation;

import cgv_23rd.ceos.entity.theater.Seat;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat,Long> {

    @Query("""
        select s
          from Seat s
          join fetch s.screen
         where s.id in :seatIds
    """)
    List<Seat> findAllByIdInWithScreen(@Param("seatIds") List<Long> seatIds);
}
