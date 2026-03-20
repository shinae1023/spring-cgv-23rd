package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.food.request.FoodOrderRequestDto;
import cgv_23rd.ceos.dto.food.response.FoodOrderResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.service.FoodOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/foods/orders")
@Tag(name = "매점 주문 API")
public class FoodOrderController {

    private final FoodOrderService foodOrderService;

    @PostMapping("")
    @Operation(summary = "매점 음식 주문 API", description = "특정 극장의 매점 음식을 주문하고 재고를 차감합니다.")
    public ApiResponse<Void> createFoodOrder(
            @RequestParam(name = "userId") Long userId,
            @Valid @RequestBody FoodOrderRequestDto requestDto) {
        return foodOrderService.createFoodOrder(userId, requestDto);
    }

    @GetMapping("")
    @Operation(summary = "내 매점 주문 내역 조회 API", description = "특정 사용자의 전체 음식 주문 내역을 조회합니다.")
    public ApiResponse<List<FoodOrderResponseDto>> getFoodOrderList(
            @RequestParam(name = "userId") Long userId) {
        return foodOrderService.getFoodOrderList(userId);
    }
}