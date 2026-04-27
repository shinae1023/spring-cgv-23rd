package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.controller.support.ControllerTestSupport;
import cgv_23rd.ceos.dto.user.request.LoginRequestDto;
import cgv_23rd.ceos.dto.user.request.ReissueRequestDto;
import cgv_23rd.ceos.dto.user.request.SignupRequestDto;
import cgv_23rd.ceos.dto.user.response.LoginResponseDto;
import cgv_23rd.ceos.dto.user.response.ReissueResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("회원가입 API 성공")
    void signupSuccess() throws Exception {
        SignupRequestDto request = new SignupRequestDto(
                "테스트유저",
                "test@example.com",
                LocalDate.of(2002, 10, 23),
                "01012345678",
                "password123!"
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("회원가입 성공"));

        verify(authService).signup(any(SignupRequestDto.class));
    }

    @Test
    @DisplayName("로그인 API 성공")
    void loginSuccess() throws Exception {
        LoginRequestDto request = new LoginRequestDto("login@example.com", "password123!");
        given(authService.login(any(LoginRequestDto.class)))
                .willReturn(LoginResponseDto.builder()
                        .accessToken("access-token")
                        .refreshToken("refresh-token")
                        .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"))
                .andExpect(jsonPath("$.result.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("토큰 재발급 API 성공")
    void reissueSuccess() throws Exception {
        ReissueRequestDto request = new ReissueRequestDto("expired-access", "refresh-token");
        given(authService.reissueToken(any(ReissueRequestDto.class)))
                .willReturn(ReissueResponseDto.builder()
                        .accessToken("new-access-token")
                        .refreshToken("new-refresh-token")
                        .build());

        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("토큰 재발급 성공"))
                .andExpect(jsonPath("$.result.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.result.refreshToken").value("new-refresh-token"));
    }
}
