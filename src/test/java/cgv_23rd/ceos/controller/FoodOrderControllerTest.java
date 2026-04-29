package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.controller.support.ControllerTestSupport;
import cgv_23rd.ceos.dto.food.request.FoodOrderItemRequestDto;
import cgv_23rd.ceos.dto.food.request.FoodOrderRequestDto;
import cgv_23rd.ceos.dto.food.response.FoodOrderItemResponseDto;
import cgv_23rd.ceos.dto.food.response.FoodOrderResponseDto;
import cgv_23rd.ceos.entity.enums.FoodOrderStatus;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FoodOrderControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("매점 음식 주문 성공")
    void createFoodOrderSuccess() throws Exception {
        FoodOrderRequestDto request = new FoodOrderRequestDto(
                1L,
                List.of(new FoodOrderItemRequestDto(10L, 2))
        );

                mockMvc.perform(post("/api/foods/orders/")
                                .with(authenticatedUser())
                                .contentType(APPLICATION_JSON)
                                .content(toJson(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("음식 주문 요청 성공"));

        verify(foodOrderService).createFoodOrder(any(Long.class), any(FoodOrderRequestDto.class));
    }

    @Test
    @DisplayName("매점 음식 주문은 인증이 필요함")
    void createFoodOrderUnauthorized() throws Exception {
        FoodOrderRequestDto request = new FoodOrderRequestDto(
                1L,
                List.of(new FoodOrderItemRequestDto(10L, 2))
        );

        mockMvc.perform(post("/api/foods/orders/")
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_4011"));
    }

    @Test
    @DisplayName("매점 음식 주문 요청의 수량이 0 이하이면 검증에 실패한다")
    void createFoodOrderValidationFail_whenQuantityIsZero() throws Exception {
        FoodOrderRequestDto request = new FoodOrderRequestDto(
                1L,
                List.of(new FoodOrderItemRequestDto(10L, 0))
        );

        mockMvc.perform(post("/api/foods/orders/")
                        .with(authenticatedUser())
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());

        verify(foodOrderService, never()).createFoodOrder(any(Long.class), any(FoodOrderRequestDto.class));
    }

    @Test
    @DisplayName("매점 주문 취소 성공")
    void cancelFoodOrderSuccess() throws Exception {
        mockMvc.perform(post("/api/foods/orders/{orderId}/cancel", 1L)
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("매점 주문 취소 성공"));

        verify(foodPaymentFacade).cancelOrder(1L, 1L);
    }

    @Test
    @DisplayName("매점 주문 내역 조회 성공")
    void getFoodOrderListSuccess() throws Exception {
        given(foodOrderQueryService.getFoodOrderList(1L)).willReturn(List.of(
                FoodOrderResponseDto.builder()
                        .orderId(1L)
                        .theaterName("CGV 강남")
                        .totalPrice(12000)
                        .status(FoodOrderStatus.완료)
                        .paymentStatus(PaymentStatus.PAID)
                        .createdAt(LocalDateTime.of(2026, 4, 24, 12, 0))
                        .items(List.of(
                                FoodOrderItemResponseDto.builder()
                                        .foodName("팝콘")
                                        .quantity(2)
                                        .price(6000)
                                        .build()
                        ))
                        .build()
        ));

        mockMvc.perform(get("/api/foods/orders/")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("주문 내역 조회 성공"))
                .andExpect(jsonPath("$.result[0].theaterName").value("CGV 강남"))
                .andExpect(jsonPath("$.result[0].items[0].foodName").value("팝콘"));
    }
}
