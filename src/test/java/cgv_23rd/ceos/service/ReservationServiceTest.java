package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.entity.theater.Screen;
import cgv_23rd.ceos.entity.theater.ScreenType;
import cgv_23rd.ceos.entity.theater.Seat;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.dto.reservation.request.ReservationRequestDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;

    @Mock private UserRepository userRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationSeatRepository reservationSeatRepository;
    @Mock private MovieScreenRepository movieScreenRepository;
    @Mock private SeatRepository seatRepository;

    @Test
    @DisplayName("영화 예매 성공 테스트")
    void createReservation_Success() {
        // given
        Long userId = 1L;
        Long movieScreenId = 1L;
        List<Long> seatIds = List.of(1L, 2L);
        ReservationRequestDto requestDto = new ReservationRequestDto(movieScreenId, seatIds);

        User user = User.builder().id(userId).build();
        Screen screen = Screen.builder().id(1L).screenType(ScreenType.builder().basePrice(15000).build()).build();
        MovieScreen movieScreen = MovieScreen.builder()
                .id(movieScreenId)
                .screen(screen)
                .startAt(LocalDateTime.now().plusHours(2))
                .build();
        Seat seat1 = Seat.builder().id(1L).screen(screen).rowName("A").colNum(1).build();
        Seat seat2 = Seat.builder().id(2L).screen(screen).rowName("A").colNum(2).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(movieScreenRepository.findById(movieScreenId)).willReturn(Optional.of(movieScreen));
        given(seatRepository.findById(1L)).willReturn(Optional.of(seat1));
        given(seatRepository.findById(2L)).willReturn(Optional.of(seat2));
        given(reservationSeatRepository.existsByMovieScreenIdAndSeatIdAndReservation_Status(any(), any(), any()))
                .willReturn(false);

        // when
        reservationService.createReservation(userId, requestDto);

        // then
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("이미 예약된 좌석일 경우 예매 실패")
    void createReservation_Fail_AlreadyReserved() {
        // given
        Long userId = 1L;
        ReservationRequestDto requestDto = new ReservationRequestDto(1L, List.of(1L));

        User user = User.builder().id(userId).build();

        ScreenType screenType = ScreenType.builder().basePrice(15000).build();
        Screen screen = Screen.builder().id(1L).screenType(screenType).build();

        MovieScreen movieScreen = MovieScreen.builder()
                .id(1L)
                .screen(screen) // Screen 설정 추가
                .startAt(LocalDateTime.now().plusHours(2))
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(movieScreenRepository.findById(1L)).willReturn(Optional.of(movieScreen));

        // 이미 좌석이 예약된 상황으로 가정
        given(reservationSeatRepository.existsByMovieScreenIdAndSeatIdAndReservation_Status(any(), any(), any()))
                .willReturn(true);

        // when & then
        GeneralException exception = assertThrows(GeneralException.class, () ->
                reservationService.createReservation(userId, requestDto)
        );

        // 발생한 예외가 우리가 정의한 에러 코드와 맞는지 확인
        assertEquals(GeneralErrorCode.RESERVATION_SEAT_DUPLICATION, exception.getCode());
    }
}