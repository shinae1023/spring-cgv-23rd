package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.reservation.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {
    @Query("SELECT distinct r FROM Reservation r " +
            "JOIN FETCH r.movieScreen ms " +
            "JOIN FETCH ms.movie m " +
            "JOIN FETCH ms.screen s " +
            "JOIN FETCH s.theater t " +
            "JOIN FETCH r.reservationSeats rs " +
            "JOIN FETCH rs.seat " +
            "WHERE r.user.id = :userId")
    List<Reservation> findAllByUserIdWithDetails(@Param("userId") Long userId);
}
