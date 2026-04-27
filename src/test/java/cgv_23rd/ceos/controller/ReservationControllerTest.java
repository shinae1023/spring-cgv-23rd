package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.controller.support.ControllerTestSupport;
import cgv_23rd.ceos.dto.payment.response.PaymentResultDto;
import cgv_23rd.ceos.dto.reservation.request.ReservationRequestDto;
import cgv_23rd.ceos.dto.reservation.response.ReservationResponseDto;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("영화 예매 성공")
    void createReservationSuccess() throws Exception {
        ReservationRequestDto request = new ReservationRequestDto(1L, List.of(10L, 11L));

        mockMvc.perform(post("/api/reservations")
                        .with(authenticatedUser())
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화 예매 성공"));

        verify(reservationService).createReservation(any(Long.class), any(ReservationRequestDto.class));
    }

    @Test
    @DisplayName("영화 예매는 인증이 필요함")
    void createReservationUnauthorized() throws Exception {
        ReservationRequestDto request = new ReservationRequestDto(1L, List.of(10L, 11L));

        mockMvc.perform(post("/api/reservations")
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_4011"));
    }

    @Test
    @DisplayName("영화 예매 취소 성공")
    void cancelReservationSuccess() throws Exception {
        mockMvc.perform(post("/api/reservations/{reservationId}/cancel", 1L)
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화 예매 취소 성공"));

        verify(reservationPaymentFacade).cancelReservation(1L, 1L);
    }

    @Test
    @DisplayName("영화 결제 성공")
    void processPaymentSuccess() throws Exception {
        given(reservationPaymentFacade.processPayment(1L, 1L))
                .willReturn(new PaymentResultDto(true, "결제가 완료되었습니다."));

        mockMvc.perform(post("/api/reservations/{reservationId}/payments", 1L)
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화 결제 성공"))
                .andExpect(jsonPath("$.result.success").value(true));

        verify(reservationPaymentFacade).processPayment(1L, 1L);
    }

    @Test
    @DisplayName("예매 내역 조회 성공")
    void getReservationListSuccess() throws Exception {
        given(reservationService.getReservationList(1L)).willReturn(List.of(
                ReservationResponseDto.builder()
                        .reservationId(1L)
                        .movieTitle("인셉션")
                        .theaterName("CGV 강남")
                        .screenName("1관")
                        .startAt(LocalDateTime.of(2026, 4, 24, 14, 0))
                        .seatInfo(List.of("A1", "A2"))
                        .totalPrice(30000)
                        .status(ReservationStatus.완료)
                        .paymentStatus(PaymentStatus.PAID)
                        .reservationAt(LocalDateTime.of(2026, 4, 20, 10, 0))
                        .build()
        ));

        mockMvc.perform(get("/api/reservations")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("예매 내역 조회 성공"))
                .andExpect(jsonPath("$.result[0].movieTitle").value("인셉션"))
                .andExpect(jsonPath("$.result[0].status").value("완료"));
    }
}
