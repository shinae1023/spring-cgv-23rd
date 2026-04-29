package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.controller.support.ControllerTestSupport;
import cgv_23rd.ceos.dto.movie.response.ActorResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieDetailResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieResponseDto;
import cgv_23rd.ceos.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MovieControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("상영 중인 영화 목록 조회 성공")
    void getMovieListSuccess() throws Exception {
        given(movieQueryService.getMovieList()).willReturn(List.of(
                MovieResponseDto.builder()
                        .movieId(1L)
                        .title("인셉션")
                        .movieImageUrl("https://example.com/poster.jpg")
                        .build()
        ));

        mockMvc.perform(get("/api/movies/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상영 중인 영화 조회 성공"))
                .andExpect(jsonPath("$.result[0].title").value("인셉션"));
    }

    @Test
    @DisplayName("영화 상세 조회 성공")
    void getMovieDetailSuccess() throws Exception {
        given(movieQueryService.getMovieDetail(1L)).willReturn(MovieDetailResponseDto.builder()
                .title("인셉션")
                .openDate(LocalDate.of(2024, 1, 1))
                .description("꿈속으로 들어가는 영화")
                .thumbnailUrl("https://example.com/thumb.jpg")
                .imageUrls(List.of("https://example.com/1.jpg"))
                .audienceCount(100000)
                .reservationRate(42.5)
                .averageRating(4.8)
                .eggRate(95.0)
                .build());

        mockMvc.perform(get("/api/movies/{movieId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화 상세 조회 성공"))
                .andExpect(jsonPath("$.result.title").value("인셉션"));
    }

    @Test
    @DisplayName("영화 출연진 조회 성공")
    void getMovieActorsSuccess() throws Exception {
        given(movieQueryService.getMovieActors(1L)).willReturn(List.of(
                ActorResponseDto.builder()
                        .name("크리스토퍼 놀란")
                        .role(Role.감독)
                        .profileUrl("https://example.com/director.jpg")
                        .build()
        ));

        mockMvc.perform(get("/api/movies/{movieId}/actors", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화 출연진 조회 성공"))
                .andExpect(jsonPath("$.result[0].name").value("크리스토퍼 놀란"))
                .andExpect(jsonPath("$.result[0].role").value("감독"));
    }

    @Test
    @DisplayName("영화 찜하기 성공")
    void likeMovieSuccess() throws Exception {
        mockMvc.perform(post("/api/movies/{movieId}/likes", 1L)
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화 찜 성공"));

        verify(movieService).likeMovie(1L, 1L);
    }

    @Test
    @DisplayName("영화 찜 취소 성공")
    void unlikeMovieSuccess() throws Exception {
        mockMvc.perform(delete("/api/movies/{movieId}/likes", 1L)
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화 찜 취소 성공"));

        verify(movieService).unlikeMovie(1L, 1L);
    }

    @Test
    @DisplayName("영화 찜하기는 인증이 필요함")
    void likeMovieUnauthorized() throws Exception {
        mockMvc.perform(post("/api/movies/{movieId}/likes", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_4011"));
    }
}
