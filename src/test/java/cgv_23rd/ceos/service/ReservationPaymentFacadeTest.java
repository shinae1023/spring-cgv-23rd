package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.payment.response.PaymentResponse;
import cgv_23rd.ceos.dto.payment.response.PaymentResultDto;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.entity.theater.Screen;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.entity.user.UserRole;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.service.pay.PaymentCompensationService;
import cgv_23rd.ceos.service.pay.PaymentService;
import cgv_23rd.ceos.service.pay.ReservationPaymentFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationPaymentFacadeTest {

    @InjectMocks
    private ReservationPaymentFacade reservationPaymentFacade;

    @Mock
    private ReservationService reservationService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentCompensationService paymentCompensationService;
    @Test
    @DisplayName("결제 성공 시 예매를 완료 상태로 확정한다")
    void processPayment_success() {
        Reservation reservation = createPendingReservation(1L, 1L);
        given(reservationService.getOwnedReservation(1L, 1L)).willReturn(reservation);
        given(paymentService.requestInstantPayment(anyString(), anyString(), anyInt(), anyString()))
                .willReturn(paidResponse("PAID"));

        PaymentResultDto result = reservationPaymentFacade.processPayment(1L, 1L);

        assertEquals(true, result.success());
        verify(reservationService).preparePayment(eq(1L), eq(1L), anyString());
        verify(reservationService).confirmReservation(1L, 1L);
    }

    @Test
    @DisplayName("결제 실패 시 예매는 유지되고 결제 실패 상태만 기록한다")
    void processPayment_fail_marksPaymentFailed() {
        Reservation reservation = createPendingReservation(1L, 1L);
        given(reservationService.getOwnedReservation(1L, 1L)).willReturn(reservation);
        given(paymentService.requestInstantPayment(anyString(), anyString(), anyInt(), anyString()))
                .willThrow(new GeneralException(GeneralErrorCode.PAYMENT_FAILED));

        assertThrows(GeneralException.class, () -> reservationPaymentFacade.processPayment(1L, 1L));

        verify(reservationService).markPaymentFailed(1L, 1L);
        verify(reservationService, never()).confirmReservation(anyLong(), anyLong());
    }

    @Test
    @DisplayName("완료된 예매 취소 시 외부 결제를 먼저 취소한다")
    void cancelReservation_paidReservation_callsExternalCancelFirst() {
        Reservation reservation = createCompletedReservation(1L, 1L, "RES_1_12345678");
        given(reservationService.getOwnedReservation(1L, 1L)).willReturn(reservation);

        reservationPaymentFacade.cancelReservation(1L, 1L);

        verify(reservationService).cancelPaidReservation(1L, 1L, "RES_1_12345678");
    }

    @Test
    @DisplayName("외부 결제 취소가 실패하면 예매는 결제 미확정 상태로 남긴다")
    void cancelReservation_cancelPaymentFails_marksPaymentUnknown() {
        Reservation reservation = createCompletedReservation(1L, 1L, "RES_1_12345678");
        given(reservationService.getOwnedReservation(1L, 1L)).willReturn(reservation);
        willThrow(new GeneralException(GeneralErrorCode.PAYMENT_SERVER_FAILED))
                .given(reservationService).cancelPaidReservation(1L, 1L, "RES_1_12345678");

        assertThrows(GeneralException.class, () -> reservationPaymentFacade.cancelReservation(1L, 1L));

        verify(reservationService).cancelPaidReservation(1L, 1L, "RES_1_12345678");
        verify(reservationService, never()).cancelReservation(1L, 1L);
    }

    private Reservation createPendingReservation(Long reservationId, Long userId) {
        User user = createUser(userId);
        Theater theater = Theater.builder().name("CGV 강남").build();
        Screen screen = Screen.builder().name("1관").theater(theater).build();
        Movie movie = Movie.create(
                "인셉션",
                "꿈을 설계하는 영화",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(30)
        );
        MovieScreen movieScreen = MovieScreen.builder()
                .movie(movie)
                .screen(screen)
                .startAt(LocalDateTime.now().plusHours(2))
                .build();

        Reservation reservation = Reservation.create(user, movieScreen, LocalDateTime.now());
        ReflectionTestUtils.setField(reservation, "id", reservationId);
        ReflectionTestUtils.setField(reservation, "totalPrice", 15000);
        ReflectionTestUtils.setField(reservation, "paymentStatus", PaymentStatus.READY);
        return reservation;
    }

    private Reservation createCompletedReservation(Long reservationId, Long userId, String paymentId) {
        Reservation reservation = createPendingReservation(reservationId, userId);
        ReflectionTestUtils.setField(reservation, "paymentId", paymentId);
        ReflectionTestUtils.setField(reservation, "status", ReservationStatus.완료);
        ReflectionTestUtils.setField(reservation, "paymentStatus", PaymentStatus.PAID);
        return reservation;
    }

    private User createUser(Long userId) {
        User user = User.signup(
                "tester",
                "01012345678",
                LocalDate.of(2000, 1, 1),
                "tester@example.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "role", UserRole.USER);
        return user;
    }

    private PaymentResponse paidResponse(String paymentStatus) {
        return new PaymentResponse(
                200,
                "ok",
                new PaymentResponse.PaymentData(
                        "PAY_1",
                        paymentStatus,
                        "인셉션 예매",
                        "mockPg",
                        "KRW",
                        "{}",
                        "2026-04-25T12:00:00"
                )
        );
    }
}
