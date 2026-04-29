package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.controller.support.ControllerTestSupport;
import cgv_23rd.ceos.dto.theater.response.TheaterDetailResponseDto;
import cgv_23rd.ceos.dto.theater.response.TheaterResponseDto;
import cgv_23rd.ceos.entity.enums.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TheaterControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("지역별 영화관 목록 조회 성공")
    void getTheatersByRegionSuccess() throws Exception {
        given(theaterQueryService.getTheatersByRegion(Region.서울)).willReturn(List.of(
                TheaterResponseDto.builder()
                        .id(1L)
                        .name("CGV 강남")
                        .address("서울 강남구")
                        .isActive(true)
                        .build()
        ));

        mockMvc.perform(get("/api/theaters")
                        .param("region", "서울"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화관 목록 조회 성공"))
                .andExpect(jsonPath("$.result[0].name").value("CGV 강남"));
    }

    @Test
    @DisplayName("영화관 상세 조회 성공")
    void getTheaterDetailSuccess() throws Exception {
        given(theaterQueryService.getTheaterDetail(1L)).willReturn(TheaterDetailResponseDto.builder()
                .id(1L)
                .name("CGV 강남")
                .address("서울 강남구")
                .isActive(true)
                .description("프리미엄 상영관")
                .imageUrl("https://example.com/theater.jpg")
                .isAvailable(true)
                .build());

        mockMvc.perform(get("/api/theaters/{theaterId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화관 상세 조회 성공"))
                .andExpect(jsonPath("$.result.name").value("CGV 강남"));
    }

    @Test
    @DisplayName("영화관 찜하기 성공")
    void likeTheaterSuccess() throws Exception {
        mockMvc.perform(post("/api/theaters/{theaterId}/likes", 1L)
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화관 찜 성공"));

        verify(theaterService).likeTheater(1L, 1L);
    }

    @Test
    @DisplayName("영화관 찜 취소 성공")
    void unlikeTheaterSuccess() throws Exception {
        mockMvc.perform(delete("/api/theaters/{theaterId}/likes", 1L)
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("영화관 찜 취소 성공"));

        verify(theaterService).unlikeTheater(1L, 1L);
    }

    @Test
    @DisplayName("영화관 찜하기는 인증이 필요함")
    void likeTheaterUnauthorized() throws Exception {
        mockMvc.perform(post("/api/theaters/{theaterId}/likes", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_4011"));
    }
}
