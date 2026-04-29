package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.food.request.FoodOrderRequestDto;
import cgv_23rd.ceos.dto.food.response.FoodOrderResponseDto;
import cgv_23rd.ceos.dto.payment.response.PaymentResultDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.security.UserDetailsImpl;
import cgv_23rd.ceos.service.FoodOrderService;
import cgv_23rd.ceos.service.pay.FoodPaymentFacade;
import cgv_23rd.ceos.service.query.FoodOrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/foods/orders")
@Tag(name = "매점 주문 API")
public class FoodOrderController {

    private final FoodOrderService foodOrderService;
    private final FoodPaymentFacade foodPaymentFacade;
    private final FoodOrderQueryService foodOrderQueryService;

    @PostMapping("/")
    @Operation(summary = "매점 음식 주문 API", description = "특정 극장의 매점 음식을 주문")
    public ApiResponse<Long> createFoodOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody FoodOrderRequestDto requestDto) {

        Long orderId = foodOrderService.createFoodOrder(userDetails.getUser().getId(), requestDto);
        return ApiResponse.onSuccess("음식 주문 요청 성공",orderId);
    }

    @PostMapping("/{orderId}/payments")
    @Operation(summary = "매점 주문 결제 API", description = "주문한 매점 음식에 대해 결제를 진행함. 결제 성공 시 재고 차감, 재고 부족 시 환불 처리")
    public ApiResponse<PaymentResultDto> processFoodPayment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {

        Long userId = userDetails.getUser().getId();
        PaymentResultDto result = foodPaymentFacade.processPayment(userId, orderId);
        return ApiResponse.onSuccess("음식 주문 결제 성공", result);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "매점 주문 취소 API", description = "대기 주문은 바로 취소하고, 완료 주문은 결제 취소 후 주문을 취소합니다.")
    public ApiResponse<Void> cancelFoodOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {

        Long userId = userDetails.getUser().getId();
        foodPaymentFacade.cancelOrder(userId, orderId);
        return ApiResponse.onSuccess("매점 주문 취소 성공");
    }

    @GetMapping("/")
    @Operation(summary = "내 매점 주문 내역 조회 API", description = "특정 사용자의 전체 음식 주문 내역을 조회합니다.")
    public ApiResponse<List<FoodOrderResponseDto>> getFoodOrderList(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.onSuccess("주문 내역 조회 성공", foodOrderQueryService.getFoodOrderList(userId));
    }

}
