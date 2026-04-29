package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.theater.response.TheaterDetailResponseDto;
import cgv_23rd.ceos.dto.theater.response.TheaterResponseDto;
import cgv_23rd.ceos.entity.enums.Region;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.security.UserDetailsImpl;
import cgv_23rd.ceos.service.TheaterService;
import cgv_23rd.ceos.service.query.TheaterQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/theaters")
@Tag(name = "영화관 API")
public class TheaterController {

    private final TheaterService theaterService;
    private final TheaterQueryService theaterQueryService;

    // 1. 영화관 목록 조회 (지역 카테고리별)
    @GetMapping("")
    @Operation(summary = "지역별 영화관 목록 조회 API", description = "특정 지역(Region)에 속한 영화관 목록을 조회함")
    public ApiResponse<List<TheaterResponseDto>> getTheatersByRegion(@RequestParam(name = "region") Region region) {
        return ApiResponse.onSuccess("영화관 목록 조회 성공", theaterQueryService.getTheatersByRegion(region));
    }

    // 2. 영화관 상세 조회
    @GetMapping("/{theaterId}")
    @Operation(summary = "영화관 상세 조회 API", description = "특정 영화관의 상세 정보(설명, 이미지 등)를 조회함")
    public ApiResponse<TheaterDetailResponseDto> getTheaterDetail(@PathVariable(name = "theaterId") Long theaterId) {
        return ApiResponse.onSuccess("영화관 상세 조회 성공", theaterQueryService.getTheaterDetail(theaterId));
    }

    // 3. 영화관 찜
    @PostMapping("/{theaterId}/likes")
    @Operation(summary = "영화관 찜하기 API", description = "사용자가 특정 영화관을 찜하도록 처리함")
    public ApiResponse<Void> likeTheater(
            @PathVariable Long theaterId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        theaterService.likeTheater(userDetails.getUser().getId(), theaterId);
        return ApiResponse.onSuccess("영화관 찜 성공");
    }

    @DeleteMapping("/{theaterId}/likes")
    @Operation(summary = "영화관 찜 취소 API", description = "사용자가 특정 영화관에 대한 찜을 취소하도록 처리함")
    public ApiResponse<Void> unlikeTheater(
            @PathVariable Long theaterId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        theaterService.unlikeTheater(userDetails.getUser().getId(), theaterId);
        return ApiResponse.onSuccess("영화관 찜 취소 성공");
    }

}
