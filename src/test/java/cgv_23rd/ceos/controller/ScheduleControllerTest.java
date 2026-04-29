package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.controller.support.ControllerTestSupport;
import cgv_23rd.ceos.dto.schedule.response.ScheduleResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScheduleControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("극장 상영 시간표 조회 성공")
    void getSchedulesSuccess() throws Exception {
        given(scheduleQueryService.getSchedules(1L, LocalDate.of(2026, 4, 24))).willReturn(List.of(
                ScheduleResponseDto.builder()
                        .movieScreenId(1L)
                        .movieId(10L)
                        .movieTitle("인셉션")
                        .screenId(100L)
                        .screenName("1관")
                        .sequence(1)
                        .startAt(LocalDateTime.of(2026, 4, 24, 14, 0))
                        .endAt(LocalDateTime.of(2026, 4, 24, 16, 30))
                        .build()
        ));

        mockMvc.perform(get("/api/schedules/{theaterId}", 1L)
                        .param("targetDate", "2026-04-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상영 시간표 조회 성공"))
                .andExpect(jsonPath("$.result[0].movieTitle").value("인셉션"));
    }
}
