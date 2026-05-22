package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.repository.reservation.ReservationRepository;
import cgv_23rd.ceos.repository.reservation.ReservationSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingReservationExpirationService {

    private static final long RESERVATION_PENDING_MINUTES = 5L;
    private static final List<PaymentStatus> EXPIRABLE_PAYMENT_STATUSES = List.of(
            PaymentStatus.READY,
            PaymentStatus.FAILED
    );

    private final ReservationSeatRepository reservationSeatRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public int expirePendingReservations() {
        LocalDateTime expiredAt = LocalDateTime.now().minusMinutes(RESERVATION_PENDING_MINUTES);

        int deletedSeats = reservationSeatRepository.deleteSeatsByExpiredPendingReservations(
                ReservationStatus.대기,
                EXPIRABLE_PAYMENT_STATUSES,
                expiredAt
        );

        int expiredReservations = reservationRepository.expirePendingReservations(
                ReservationStatus.대기,
                ReservationStatus.취소,
                EXPIRABLE_PAYMENT_STATUSES,
                expiredAt
        );

        log.info("Expired pending reservations. deletedSeats={}, expiredReservations={}",
                deletedSeats, expiredReservations);

        return expiredReservations;
    }
}
