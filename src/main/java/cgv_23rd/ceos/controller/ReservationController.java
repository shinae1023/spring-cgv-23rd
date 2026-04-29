package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.payment.response.PaymentResultDto;
import cgv_23rd.ceos.dto.reservation.request.ReservationRequestDto;
import cgv_23rd.ceos.dto.reservation.response.ReservationResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.security.UserDetailsImpl;
import cgv_23rd.ceos.service.ReservationService;
import cgv_23rd.ceos.service.pay.ReservationPaymentFacade;
import cgv_23rd.ceos.service.query.ReservationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
@Tag(name = "예약 API")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationPaymentFacade reservationPaymentFacade;
    private final ReservationQueryService reservationQueryService;

    // 1. 영화 예매
    @PostMapping("")
    @Operation(summary = "영화 예매 API", description = "상영 회차와 좌석을 선택하여 영화를 예매함")
    public ApiResponse<Long> createReservation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReservationRequestDto requestDto) {

        Long userId = userDetails.getUser().getId();
        Long reservationId = reservationService.createReservation(userId, requestDto);

        return ApiResponse.onSuccess("영화 예매 성공",reservationId);
    }

    @PostMapping("/{reservationId}/payments")
    @Operation(summary = "영화 예매 결제 API", description = "예매한 영화에 대해 결제를 진행함")
    public ApiResponse<PaymentResultDto> processPayment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long reservationId) {

        Long userId = userDetails.getUser().getId();
        PaymentResultDto result = reservationPaymentFacade.processPayment(userId, reservationId);
        return ApiResponse.onSuccess("영화 결제 성공",result);
    }

    // 2. 영화 예매 취소
    @PostMapping("/{reservationId}/cancel")
    @Operation(summary = "영화 예매 취소 API", description = "예매 상태를 취소로 변경함")
    public ApiResponse<Void> cancelReservation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long reservationId) {

        Long userId = userDetails.getUser().getId();
        reservationPaymentFacade.cancelReservation(userId, reservationId);

        return ApiResponse.onSuccess("영화 예매 취소 성공");
    }

    // 3. 예매 내역 조회
    @GetMapping("")
    @Operation(summary = "예매 내역 조회 API", description = "특정 유저의 전체 예매 내역 리스트를 조회함")
    public ApiResponse<List<ReservationResponseDto>> getReservationList(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long userId = userDetails.getUser().getId();
        return ApiResponse.onSuccess("예매 내역 조회 성공", reservationQueryService.getReservationList(userId));
    }
}
