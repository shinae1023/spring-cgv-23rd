package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.movie.request.MovieRequestDto;
import cgv_23rd.ceos.dto.movie.response.ActorResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieDetailResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.service.MovieService;
import cgv_23rd.ceos.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies")
@Tag(name = "영화 API")
public class MovieController {
    private final MovieService movieService;

    // 1. 영화 생성
    @PostMapping("/")
    @Operation(summary = "영화 생성 API", description = "새로운 영화 정보를 등록함")
    public ApiResponse<Long> createMovie(@Valid @RequestBody MovieRequestDto requestDto) {
        return movieService.createMovie(requestDto);
    }

    // 2. 현재 상영중인 영화 목록 조회
    @GetMapping("/")
    @Operation(summary = "상영 중인 영화 목록 조회 API", description = "예매율 내림차순으로 정렬된 영화 목록을 반환함")
    public ApiResponse<List<MovieResponseDto>> getMovieList() {
        return movieService.getMovieList();
    }

    // 3. 영화 상세 조회
    @GetMapping("/{movieId}")
    @Operation(summary = "영화 상세 정보 조회 API", description = "특정 영화의 상세 정보와 이미지 리스트를 조회함")
    public ApiResponse<MovieDetailResponseDto> getMovieDetail(@PathVariable(name = "movieId") Long movieId) {
        return movieService.getMovieDetail(movieId);
    }

    // 4. 출연진 조회
    @GetMapping("/{movieId}/actors")
    @Operation(summary = "영화 출연진 조회 API", description = "해당 영화에 참여한 배우 및 감독 리스트를 조회함")
    public ApiResponse<List<ActorResponseDto>> getMovieActors(@PathVariable(name = "movieId") Long movieId) {
        return movieService.getMovieActors(movieId);
    }

    // 5. 영화 찜하기/취소
    @PostMapping("/{movieId}/like")
    @Operation(summary = "영화 찜 토글 API", description = "영화 찜하기 또는 찜 취소 처리를 수행함")
    public ApiResponse<Void> toggleMovieLike(
            @PathVariable(name = "movieId") Long movieId,
            @RequestParam(name = "userId") Long userId) {
        return movieService.toggleMovieLike(userId, movieId);
    }
}
