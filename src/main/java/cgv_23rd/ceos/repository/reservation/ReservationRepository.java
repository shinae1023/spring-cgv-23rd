package cgv_23rd.ceos.repository.reservation;

import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.reservation.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {

    @Query("""
        select r
          from Reservation r
          join fetch r.user u
          join fetch r.movieScreen ms
          join fetch ms.movie m
          join fetch ms.screen s
          join fetch s.theater t
         where r.id = :reservationId
    """)
    Optional<Reservation> findByIdWithDetails(@Param("reservationId") Long reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :reservationId")
    Optional<Reservation> findByIdWithLock(@Param("reservationId") Long reservationId);

    //취소 예매 내역 남기기 위해 left join으로 변경
    @Query("SELECT distinct r FROM Reservation r " +
            "JOIN FETCH r.movieScreen ms " +
            "JOIN FETCH ms.movie m " +
            "JOIN FETCH ms.screen s " +
            "JOIN FETCH s.theater t " +
            "LEFT JOIN FETCH r.reservationSeats rs " +
            "LEFT JOIN FETCH rs.seat " +
            "WHERE r.user.id = :userId")
    List<Reservation> findAllByUserIdWithDetails(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Reservation r
           set r.status = :canceledStatus
         where r.status = :pendingStatus
           and r.paymentStatus in :expirablePaymentStatuses
           and r.createdAt < :expiredAt
    """)
    int expirePendingReservations(
            @Param("pendingStatus") ReservationStatus pendingStatus,
            @Param("canceledStatus") ReservationStatus canceledStatus,
            @Param("expirablePaymentStatuses") List<PaymentStatus> expirablePaymentStatuses,
            @Param("expiredAt") LocalDateTime expiredAt
    );

}
