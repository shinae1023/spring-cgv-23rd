package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.reservation.request.ReservationRequestDto;
import cgv_23rd.ceos.dto.reservation.response.ReservationResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
@Tag(name = "예약 API")
public class ReservationController {

    private final ReservationService reservationService;

    // 1. 영화 예매
    @PostMapping("")
    @Operation(summary = "영화 예매 API", description = "상영 회차와 좌석을 선택하여 영화를 예매함")
    public ApiResponse<Void> createReservation(
            @RequestParam(name = "userId") Long userId,
            @Valid @RequestBody ReservationRequestDto requestDto) {

        return reservationService.createReservation(userId, requestDto);
    }

    // 2. 영화 예매 취소
    @PostMapping("/{reservationId}/cancel")
    @Operation(summary = "영화 예매 취소 API", description = "예매 상태를 취소로 변경함")
    public ApiResponse<Void> cancelReservation(
            @RequestParam(name = "userId") Long userId,
            @PathVariable(name = "reservationId") Long reservationId) {

        return reservationService.cancelReservation(userId, reservationId);
    }

    // 3. 예매 내역 조회
    @GetMapping("")
    @Operation(summary = "예매 내역 조회 API", description = "특정 유저의 전체 예매 내역 리스트를 조회함")
    public ApiResponse<List<ReservationResponseDto>> getReservationList(
            @RequestParam(name = "userId") Long userId) {

        return reservationService.getReservationList(userId);
    }
}