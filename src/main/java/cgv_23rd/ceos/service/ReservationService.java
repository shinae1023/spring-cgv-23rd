package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.reservation.request.ReservationRequestDto;
import cgv_23rd.ceos.dto.payment.response.PaymentResponse;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.entity.theater.Seat;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.movie.MovieScreenRepository;
import cgv_23rd.ceos.repository.reservation.ReservationRepository;
import cgv_23rd.ceos.repository.reservation.ReservationSeatRepository;
import cgv_23rd.ceos.repository.reservation.SeatRepository;
import cgv_23rd.ceos.service.lock.ReservationNamedLockManager;
import cgv_23rd.ceos.service.pay.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final MovieScreenRepository movieScreenRepository;
    private final SeatRepository seatRepository;
    private final ReservationNamedLockManager reservationNamedLockManager;
    private final PaymentService paymentService;
    private final UserService userService;

    // 1. 영화 예매
    public Long createReservation(Long userId, ReservationRequestDto requestDto) {
        validateSeatRequest(requestDto);
        User user = userService.getUser(userId);
        MovieScreen movieScreen = getMovieScreen(requestDto.movieScreenId());
        List<Long> sortedSeatIds = requestDto.seatIds().stream().sorted().toList();

        reservationNamedLockManager.acquireLocks(buildSeatLockKeys(movieScreen.getId(), sortedSeatIds));

        Reservation reservation = Reservation.create(user, movieScreen, LocalDateTime.now());
        processSeatReservations(reservation, movieScreen.getId(), sortedSeatIds);

        try {
            reservationRepository.save(reservation);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(GeneralErrorCode.RESERVATION_SEAT_DUPLICATION);
        }

        // 결제창으로 넘어가기 위해 생성된 예약 정보를 반환
        return reservation.getId();
    }

    @Transactional
    public void confirmReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(userId, reservation);
        reservation.markPaymentPaid();
        reservation.confirm();
    }

    @Transactional
    public void preparePayment(Long userId, Long reservationId, String paymentId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(userId, reservation);
        reservation.assignPaymentId(paymentId);
    }

    @Transactional
    public void markPaymentFailed(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(userId, reservation);
        reservation.markPaymentFailed();
    }

    @Transactional
    public void markPaymentUnknown(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(userId, reservation);
        reservation.markPaymentUnknown();
    }

    @Transactional
    public void markPaymentCancelled(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(userId, reservation);
        reservation.markPaymentCancelled();
    }

    @Transactional(readOnly = true)
    public Reservation getOwnedReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(userId, reservation);
        return reservation;
    }

    @Transactional
    public Reservation getReservationWithLock(Long reservationId) {
        return reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
    }

    @Transactional
    public Reservation getOwnedReservationWithLock(Long userId, Long reservationId) {
        Reservation reservation = getReservationWithLock(reservationId);
        validateOwner(userId, reservation);
        return reservation;
    }

    // 2. 영화 예매 취소
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = getOwnedReservationWithLock(userId, reservationId);
        reservation.cancel(LocalDateTime.now());
    }

    public void cancelPaidReservation(Long userId, Long reservationId, String paymentId) {
        PaymentResponse response;
        try {
            response = paymentService.cancelPayment(paymentId);
        } catch (GeneralException e) {
            markPaymentUnknown(userId, reservationId);
            throw e;
        }

        if (response == null
                || response.data() == null
                || PaymentStatus.from(response.data().paymentStatus()) != PaymentStatus.CANCELLED) {
            markPaymentUnknown(userId, reservationId);
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_CANCELLABLE);
        }

        Reservation reservation = getOwnedReservationWithLock(userId, reservationId);
        reservation.markPaymentCancelled();
        reservation.cancel(LocalDateTime.now());
    }

    private void validateOwner(Long userId, Reservation reservation) {
        userService.getUser(userId);

        if (!reservation.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }
    }

    // --- Helper Methods ---
    private void validateSeatRequest(ReservationRequestDto requestDto) {
        // 좌석 없는 예매 방지
        if (requestDto.seatIds() == null || requestDto.seatIds().isEmpty()) {
            throw new GeneralException(GeneralErrorCode.RESERVATION_SEAT_EMPTY);
        }

        // 요청 자체의 중복 좌석 검사
        Set<Long> uniqueSeatIds = new HashSet<>(requestDto.seatIds());
        if (uniqueSeatIds.size() != requestDto.seatIds().size()) {
            throw new GeneralException(GeneralErrorCode.RESERVATION_SEAT_DUPLICATION);
        }
    }

    private void processSeatReservations(Reservation reservation, Long movieScreenId, List<Long> seatIds) {
        List<Seat> seats = seatRepository.findAllByIdInWithScreen(seatIds);
        validateAllSeatsFound(seatIds, seats);

        Set<Long> reservedSeatIds = new HashSet<>(reservationSeatRepository.findReservedSeatIds(
                movieScreenId,
                seatIds,
                List.of(ReservationStatus.완료, ReservationStatus.대기)
        ));

        if (!reservedSeatIds.isEmpty()) {
            throw new GeneralException(GeneralErrorCode.RESERVATION_SEAT_DUPLICATION);
        }

        Map<Long, Seat> seatById = seats.stream()
                .collect(Collectors.toMap(Seat::getId, Function.identity()));

        seatIds.stream()
                .map(seatById::get)
                .sorted(Comparator.comparing(Seat::getId))
                .forEach(reservation::addSeat);
    }

    private List<String> buildSeatLockKeys(Long movieScreenId, List<Long> seatIds) {
        return seatIds.stream()
                .map(seatId -> "reservation:" + movieScreenId + ":" + seatId)
                .toList();
    }

    private void validateUserExists(Long userId) {
        userService.getUser(userId);
    }

    private MovieScreen getMovieScreen(Long id) {
        return movieScreenRepository.findById(id)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIESCREEN_NOT_FOUND));
    }

    private void validateAllSeatsFound(List<Long> seatIds, List<Seat> seats) {
        if (seats.size() != seatIds.size()) {
            throw new GeneralException(GeneralErrorCode.SEAT_NOT_FOUND);
        }
    }
}
