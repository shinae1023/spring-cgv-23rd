package cgv_23rd.ceos.repository.reservation;

import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.reservation.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat,Long> {
    boolean existsByMovieScreenIdAndSeatIdAndReservation_StatusIn(Long movieScreenId, Long seatId, List<ReservationStatus> status);

    @Query("""
        select rs.seat.id
          from ReservationSeat rs
         where rs.movieScreen.id = :movieScreenId
           and rs.seat.id in :seatIds
           and rs.reservation.status in :statuses
    """)
    List<Long> findReservedSeatIds(
            @Param("movieScreenId") Long movieScreenId,
            @Param("seatIds") List<Long> seatIds,
            @Param("statuses") List<ReservationStatus> statuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from ReservationSeat rs
         where rs.reservation.id in (
               select r.id
                from Reservation r
               where r.status = :pendingStatus
                 and r.paymentStatus in :expirablePaymentStatuses
                  and r.createdAt < :expiredAt
         )
    """)
    int deleteSeatsByExpiredPendingReservations(
            @Param("pendingStatus") ReservationStatus pendingStatus,
            @Param("expirablePaymentStatuses") List<PaymentStatus> expirablePaymentStatuses,
            @Param("expiredAt") LocalDateTime expiredAt
    );
}
