package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.controller.support.ControllerTestSupport;
import cgv_23rd.ceos.dto.food.request.FoodCreateRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminFoodControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("관리자 음식 등록 성공")
    void createFoodSuccess() throws Exception {
        FoodCreateRequestDto request = new FoodCreateRequestDto("팝콘", 6000);

        mockMvc.perform(post("/api/admin/foods/")
                        .with(authenticatedAdmin())
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("음식 등록 성공"));

        verify(adminFoodService).createFood(request);
    }

    @Test
    @DisplayName("관리자 음식 재고 수정 성공")
    void updateFoodStockSuccess() throws Exception {
        mockMvc.perform(patch("/api/admin/foods/{theaterFoodId}", 1L)
                        .with(authenticatedAdmin())
                        .param("stock", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("재고 수정 성공"));

        verify(adminFoodService).updateFoodStock(1L, 15);
    }

    @Test
    @DisplayName("관리자 음식 등록은 관리자 권한이 필요함")
    void createFoodForbiddenForUser() throws Exception {
        FoodCreateRequestDto request = new FoodCreateRequestDto("팝콘", 6000);

        mockMvc.perform(post("/api/admin/foods/")
                        .with(authenticatedUser())
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_4031"));
    }
}
