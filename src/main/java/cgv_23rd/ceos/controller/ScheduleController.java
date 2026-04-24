package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.schedule.request.ScheduleCreateRequestDto;
import cgv_23rd.ceos.dto.schedule.response.ScheduleResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.security.UserDetailsImpl;
import cgv_23rd.ceos.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
@Tag(name = "상영시간표 API")
public class ScheduleController {
    private final ScheduleService scheduleService;

    // 2. 극장별 상영 시간표 조회
    @GetMapping("/{theaterId}")
    @Operation(summary = "극장 상영 시간표 조회 API", description = "특정 극장과 날짜를 기준으로 상영 시간표를 조회함")
    public ApiResponse<List<ScheduleResponseDto>> getSchedules(
            @PathVariable Long theaterId,
            @RequestParam(name = "targetDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate) {

        return ApiResponse.onSuccess("상영 시간표 조회 성공",scheduleService.getSchedules(theaterId, targetDate));
    }
}
