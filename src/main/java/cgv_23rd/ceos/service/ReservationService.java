package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.entity.reservation.ReservationSeat;
import cgv_23rd.ceos.entity.theater.Seat;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.dto.reservation.request.ReservationRequestDto;
import cgv_23rd.ceos.dto.reservation.response.ReservationResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final MovieScreenRepository movieScreenRepository;
    private final SeatRepository seatRepository;

    // 1. 영화 예매
    public ApiResponse<Void> createReservation(Long userId, ReservationRequestDto requestDto) {
        // 좌석 없는 예메 방지
        if (requestDto.seatIds() == null || requestDto.seatIds().isEmpty()) {
            throw new GeneralException(GeneralErrorCode.RESERVATION_SEAT_EMPTY);
        }

        // 요청 자체의 중복 좌석 검사
        Set<Long> uniqueSeatIds = new HashSet<>(requestDto.seatIds());
        if (uniqueSeatIds.size() != requestDto.seatIds().size()) {
            throw new GeneralException(GeneralErrorCode.RESERVATION_SEAT_DUPLICATION);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        MovieScreen movieScreen = movieScreenRepository.findById(requestDto.movieScreenId())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.MOVIESCREEN_NOT_FOUND));

        //이미 시작된 영화는 예매 불가
        if (movieScreen.getStartAt().isBefore(LocalDateTime.now())) {
            throw new GeneralException(GeneralErrorCode.MOVIE_ALREADY_STARTED);
        }

        Integer seatPrice = movieScreen.getScreen().getScreenType().getBasePrice();

        Reservation reservation = Reservation.builder()
                .user(user)
                .movieScreen(movieScreen)
                .status(ReservationStatus.완료)
                .totalPrice(0)
                .reservationSeats(new ArrayList<>())
                .build();

        int calculatedTotalPrice = 0;

        for (Long seatId : requestDto.seatIds()) {
            boolean isAlreadyReserved = reservationSeatRepository.existsByMovieScreenIdAndSeatIdAndReservation_Status(
                    movieScreen.getId(), seatId, ReservationStatus.완료
            );
            if (isAlreadyReserved) {
                throw new GeneralException(GeneralErrorCode.RESERVATION_SEAT_DUPLICATION);
            }

            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.SEAT_NOT_FOUND));

            if (!seat.getScreen().getId().equals(movieScreen.getScreen().getId())) {
                throw new GeneralException(GeneralErrorCode.SEAT_SCREEN_INVALID);
            }

            ReservationSeat reservationSeat = ReservationSeat.builder()
                    .reservation(reservation)
                    .seat(seat)
                    .movieScreen(movieScreen)
                    .price(seatPrice)
                    .build();

            reservation.getReservationSeats().add(reservationSeat);
            calculatedTotalPrice += seatPrice;
        }

        reservation.updateTotalPrice(calculatedTotalPrice);
        reservationRepository.save(reservation);

        return ApiResponse.onSuccess("예매 성공");
    }

    // 2. 영화 예매 취소
    public ApiResponse<Void> cancelReservation(Long userId, Long reservationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));

        //예매 유저와 예매 취소 유저가 동일한지 확인
        if (!reservation.getUser().getId().equals(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }

        //이미 취소된 예매인지 확인
        if (reservation.getStatus() == ReservationStatus.취소) {
            throw new GeneralException(GeneralErrorCode.RESERVATION_ALREADY_CANCELED);
        }

        //이미 시작한 영화인지 확인
        if (reservation.getMovieScreen().getStartAt().isBefore(LocalDateTime.now())) {
            throw new GeneralException(GeneralErrorCode.MOVIE_ALREADY_STARTED);
        }

        reservation.cancel();

        return ApiResponse.onSuccess("예매 취소 성공");
    }

    // 3. 예매 내역 조회
    @Transactional(readOnly = true)
    public ApiResponse<List<ReservationResponseDto>> getReservationList(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND));

        List<Reservation> reservations = reservationRepository.findAllByUserIdWithDetails(userId);

        List<ReservationResponseDto> responseDtos =  user.getReservations().stream()
                .map(res -> ReservationResponseDto.builder()
                        .reservationId(res.getId())
                        .movieTitle(res.getMovieScreen().getMovie().getTitle())
                        .theaterName(res.getMovieScreen().getScreen().getTheater().getName())
                        .screenName(res.getMovieScreen().getScreen().getName())
                        .startAt(res.getMovieScreen().getStartAt())
                        .seatInfo(res.getReservationSeats().stream()
                                .map(rs -> rs.getSeat().getRowName() + rs.getSeat().getColNum())
                                .collect(Collectors.toList()))
                        .totalPrice(res.getTotalPrice())
                        .status(res.getStatus())
                        .reservationAt(res.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.onSuccess("예매 내역 조회 성공", responseDtos);
    }
}