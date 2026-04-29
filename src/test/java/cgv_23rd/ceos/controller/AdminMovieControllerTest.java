package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.controller.support.ControllerTestSupport;
import cgv_23rd.ceos.dto.movie.request.MovieRequestDto;
import cgv_23rd.ceos.dto.schedule.request.ScheduleCreateRequestDto;
import cgv_23rd.ceos.dto.theater.request.TheaterRequestDto;
import cgv_23rd.ceos.entity.enums.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMovieControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("관리자 영화 생성 성공")
    void createMovieSuccess() throws Exception {
        MovieRequestDto request = new MovieRequestDto(
                "인셉션",
                "꿈속으로 들어가는 영화",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 1)
        );
        given(adminMovieService.createMovie(any(MovieRequestDto.class))).willReturn(1L);

        mockMvc.perform(post("/api/admin/movies/")
                        .with(authenticatedAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화 생성 성공"))
                .andExpect(jsonPath("$.result").value(1L));
    }

    @Test
    @DisplayName("관리자 상영 시간표 등록 성공")
    void createScheduleSuccess() throws Exception {
        ScheduleCreateRequestDto request = new ScheduleCreateRequestDto(
                1L,
                2L,
                1,
                LocalDateTime.of(2026, 4, 24, 14, 0),
                LocalDateTime.of(2026, 4, 24, 16, 30)
        );

        mockMvc.perform(post("/api/admin/movies/{theaterId}", 1L)
                        .with(authenticatedAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상영 시간표 등록 성공"));

        verify(adminMovieService).createSchedule(1L, request);
    }

    @Test
    @DisplayName("관리자 극장 생성 성공")
    void createTheaterSuccess() throws Exception {
        TheaterRequestDto request = new TheaterRequestDto(
                "CGV 강남",
                "서울 강남구",
                "프리미엄 상영관",
                "https://example.com/theater.jpg",
                Region.서울
        );

        mockMvc.perform(post("/api/admin/movies/theaters")
                        .with(authenticatedAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("극장 생성 성공"));

        verify(adminMovieService).createTheater(request);
    }

    @Test
    @DisplayName("관리자 영화 생성은 관리자 권한이 필요함")
    void createMovieForbiddenForUser() throws Exception {
        MovieRequestDto request = new MovieRequestDto(
                "인셉션",
                "꿈속으로 들어가는 영화",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 1)
        );

        mockMvc.perform(post("/api/admin/movies/")
                        .with(authenticatedUser())
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_4031"));
    }
}
