package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.entity.enums.Region;
import cgv_23rd.ceos.dto.theater.request.TheaterRequestDto;
import cgv_23rd.ceos.dto.theater.response.TheaterDetailResponseDto;
import cgv_23rd.ceos.dto.theater.response.TheaterResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.service.TheaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/theaters")
@Tag(name = "영화관 API")
public class TheaterController {

    private final TheaterService theaterService;

    // 1. 영화관 목록 조회 (지역 카테고리별)
    @GetMapping("")
    @Operation(summary = "지역별 영화관 목록 조회 API", description = "특정 지역(Region)에 속한 영화관 목록을 조회함")
    public ApiResponse<List<TheaterResponseDto>> getTheatersByRegion(@RequestParam(name = "region") Region region) {
        return theaterService.getTheatersByRegion(region);
    }

    // 2. 영화관 상세 조회
    @GetMapping("/{theaterId}")
    @Operation(summary = "영화관 상세 조회 API", description = "특정 영화관의 상세 정보(설명, 이미지 등)를 조회함")
    public ApiResponse<TheaterDetailResponseDto> getTheaterDetail(@PathVariable(name = "theaterId") Long theaterId) {
        return theaterService.getTheaterDetail(theaterId);
    }

    // 3. 영화관 찜
    @PostMapping("/{theaterId}/like")
    @Operation(summary = "영화관 찜 토글 API", description = "영화관 찜하기 또는 찜 취소 처리를 수행함")
    public ApiResponse<Void> toggleTheaterLike(
            @PathVariable(name = "theaterId") Long theaterId,
            @RequestParam(name = "userId") Long userId) {
        return theaterService.toggleTheaterLike(userId, theaterId);
    }

    // 4. 극장 생성
    @PostMapping("")
    @Operation(summary = "극장 생성 API", description = "새로운 극장 정보를 등록함")
    public ApiResponse<Void> createTheater(@Valid @RequestBody TheaterRequestDto requestDto) {
        return theaterService.createTheater(requestDto);
    }
}