package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.food.request.FoodCreateRequestDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.security.UserDetailsImpl;
import cgv_23rd.ceos.service.admin.AdminFoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/foods")
@Tag(name = "어드민 매점 API")
public class AdminFoodController {
    private final AdminFoodService adminFoodService;

    @PostMapping("/")
    @Operation(summary = "음식 등록 API", description = "음식을 모든 매장에 등록합니다.")
    public ApiResponse<Void> getFoodOrderList(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody FoodCreateRequestDto requestDto) {
        Long userId = userDetails.getUser().getId();
        adminFoodService.createFood(requestDto);
        return ApiResponse.onSuccess("음식 등록 성공");
    }

    @PatchMapping("/{theaterFoodId}")
    @Operation(summary = "음식 재고 수정 API", description = "음식 재고를 수정합니다.")
    public ApiResponse<Void> updateFoodStock(@AuthenticationPrincipal UserDetailsImpl userDetails,@PathVariable Long theaterFoodId,
                                             @RequestParam int stock){
        Long userId = userDetails.getUser().getId();
        adminFoodService.updateFoodStock(theaterFoodId, stock);
        return ApiResponse.onSuccess("재고 수정 성공");
    }
}
