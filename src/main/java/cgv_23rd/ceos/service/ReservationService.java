package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.reservation.request.ReservationRequestDto;
import cgv_23rd.ceos.dto.reservation.response.ReservationResponseDto;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.entity.theater.Seat;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.*;
import cgv_23rd.ceos.repository.movie.MovieScreenRepository;
import cgv_23rd.ceos.repository.reservation.ReservationRepository;
import cgv_23rd.ceos.repository.reservation.ReservationSeatRepository;
import cgv_23rd.ceos.repository.reservation.SeatRepository;
import cgv_23rd.ceos.service.lock.ReservationNamedLockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final MovieScreenRepository movieScreenRepository;
    private final SeatRepository seatRepository;
    private final ReservationNamedLockManager reservationNamedLockManager;

    // 1. 영화 예매
    public Long createReservation(Long userId, ReservationRequestDto requestDto) {
        validateSeatRequest(requestDto);
        User user = getUser(userId);
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

    @Transactional(noRollbackFor = GeneralException.class)
    public void confirmReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(userId, reservation);
        reservation.markPaymentPaid();
        reservation.confirm();
    }

    @Transactional(noRollbackFor = GeneralException.class)
    public void preparePayment(Long userId, Long reservationId, String paymentId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(userId, reservation);
        reservation.assignPaymentId(paymentId);
    }

    @Transactional(noRollbackFor = GeneralException.class)
    public void markPaymentFailed(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(userId, reservation);
        reservation.markPaymentFailed();
    }

    @Transactional(noRollbackFor = GeneralException.class)
    public void markPaymentUnknown(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(userId, reservation);
        reservation.markPaymentUnknown();
    }

    @Transactional(noRollbackFor = GeneralException.class)
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

    @Transactional(noRollbackFor = GeneralException.class)
    public Reservation getReservationWithLock(Long reservationId) {
        return reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
    }

    @Transactional(noRollbackFor = GeneralException.class)
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

    // 3. 예매 내역 조회
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getReservationList(Long userId) {
        validateUserExists(userId);
        return reservationRepository.findAllByUserIdWithDetails(userId).stream()
                .map(this::toReservationResponse)
                .toList();
    }

    private void validateOwner(Long userId, Reservation reservation) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        if (!reservation.isOwnedBy(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }
    }

    // --- Helper Methods ---
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));
    }

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
        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.SEAT_NOT_FOUND));

            boolean isAlreadyReserved = reservationSeatRepository
                    .existsByMovieScreenIdAndSeatIdAndReservation_StatusIn(
                            movieScreenId, seatId, List.of(ReservationStatus.완료, ReservationStatus.대기)
                    );

            if (isAlreadyReserved) {
                throw new GeneralException(GeneralErrorCode.RESERVATION_SEAT_DUPLICATION);
            }

            reservation.addSeat(seat);
        }
    }

    private List<String> buildSeatLockKeys(Long movieScreenId, List<Long> seatIds) {
        return seatIds.stream()
                .map(seatId -> "reservation:" + movieScreenId + ":" + seatId)
                .toList();
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new GeneralException(GeneralErrorCode.USER_NOT_FOUND);
        }
    }

    private MovieScreen getMovieScreen(Long id) {
        return movieScreenRepository.findById(id)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIESCREEN_NOT_FOUND));
    }

    private ReservationResponseDto toReservationResponse(Reservation res) {
        return ReservationResponseDto.builder()
                .reservationId(res.getId())
                .movieTitle(res.getMovieTitle())
                .theaterName(res.getTheaterName())
                .screenName(res.getScreenName())
                .startAt(res.getMovieScreen().getStartAt())
                .seatInfo(res.getSeatLabels())
                .totalPrice(res.getTotalPrice())
                .status(res.getStatus())
                .paymentStatus(res.getPaymentStatus())
                .reservationAt(res.getCreatedAt())
                .build();
    }
}
