package cgv_23rd.ceos.entity.payment;

import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.entity.theater.Screen;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentPreparationGuardTest {

    @Test
    @DisplayName("결제 진행 중인 음식 주문은 paymentId를 다시 할당할 수 없다")
    void foodOrder_assignPaymentId_rejectsProcessingOrder() {
        FoodOrder order = FoodOrder.create(createUser(), Theater.builder().name("CGV 강남").build());
        ReflectionTestUtils.setField(order, "paymentStatus", PaymentStatus.PROCESSING);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> order.assignPaymentId("FOOD_1_retry")
        );

        assertEquals(GeneralErrorCode.PAYMENT_ALREADY_PROCESSED, exception.getCode());
    }

    @Test
    @DisplayName("이미 결제 식별자가 있는 예매는 paymentId를 다시 할당할 수 없다")
    void reservation_assignPaymentId_rejectsExistingPaymentId() {
        Reservation reservation = Reservation.create(
                createUser(),
                MovieScreen.builder()
                        .movie(Movie.create("인셉션", "꿈", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)))
                        .screen(Screen.builder().name("1관").theater(Theater.builder().name("CGV 강남").build()).build())
                        .startAt(LocalDateTime.now().plusHours(2))
                        .build(),
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(reservation, "paymentId", "RES_1_existing");
        ReflectionTestUtils.setField(reservation, "paymentStatus", PaymentStatus.FAILED);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> reservation.assignPaymentId("RES_1_retry")
        );

        assertEquals(GeneralErrorCode.PAYMENT_NOT_READY, exception.getCode());
    }

    private User createUser() {
        return User.signup(
                "tester",
                "01012345678",
                LocalDate.of(2000, 1, 1),
                "tester@example.com",
                "encoded-password"
        );
    }
}
