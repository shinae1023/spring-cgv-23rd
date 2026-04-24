package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.movie.request.MovieRequestDto;
import cgv_23rd.ceos.dto.schedule.request.ScheduleCreateRequestDto;
import cgv_23rd.ceos.dto.theater.request.TheaterRequestDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.security.UserDetailsImpl;
import cgv_23rd.ceos.service.admin.AdminMovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/movies")
@Tag(name = "어드민 영화 API")
public class AdminMovieController {

    private final AdminMovieService adminMovieService;

    // 1. 영화 생성
    @PostMapping("/")
    @Operation(summary = "영화 생성 API", description = "새로운 영화 정보를 등록함")
    public ApiResponse<Long> createMovie(@Valid @RequestBody MovieRequestDto requestDto) {
        return ApiResponse.onSuccess("영화 생성 성공", adminMovieService.createMovie(requestDto));
    }

    // 1. 극장별 상영 시간표 등록
    @PostMapping("/{theaterId}")
    @Operation(summary = "상영 시간표 등록 API", description = "특정 상영관에 영화 상영 일정을 등록함")
    public ApiResponse<Void> createSchedule(@PathVariable Long theaterId,
                                            @Valid @RequestBody ScheduleCreateRequestDto requestDto) {

        adminMovieService.createSchedule(theaterId, requestDto);

        return ApiResponse.onSuccess("상영 시간표 등록 성공");
    }

    @PostMapping("/theaters")
    @Operation(summary = "극장 생성 API", description = "새로운 극장 정보를 등록함")
    public ApiResponse<Void> createTheater(@Valid @RequestBody TheaterRequestDto requestDto) {
        adminMovieService.createTheater(requestDto);
        return ApiResponse.onSuccess("극장 생성 성공");
    }
}
