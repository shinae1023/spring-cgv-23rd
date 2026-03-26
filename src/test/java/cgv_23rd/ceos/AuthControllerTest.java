package cgv_23rd.ceos;

import cgv_23rd.ceos.dto.user.request.LoginRequestDto;
import cgv_23rd.ceos.dto.user.request.SignupRequestDto;
import cgv_23rd.ceos.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;


@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // 각 테스트 전 데이터 정리를 수행함
    }

    @Test
    @DisplayName("회원가입 API 성공 테스트")
    void signup_Success() throws Exception {
        // given: 회원가입 요청 데이터
        SignupRequestDto signupRequest = new SignupRequestDto(
                "테스트유저",
                "test@example.com",
                LocalDate.of(2002, 10, 23),
                "01012345678",
                "password123!"
        );

        // when & then: 회원가입 요청을 보내고 결과 확인
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("회원가입 성공"));
    }

    @Test
    @DisplayName("로그인 API 성공 테스트 - 토큰 발급 확인")
    void login_Success() throws Exception {
        // given: 회원가입을 먼저 진행하여 DB에 유저를 생성
        SignupRequestDto signupRequest = new SignupRequestDto(
                "로그인테스트",
                "login@example.com",
                LocalDate.of(2002, 10, 23),
                "01098765432",
                "password123!"
        );

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)));

        // login 데이터 준비
        LoginRequestDto loginRequest = new LoginRequestDto("login@example.com", "password123!");

        // when & then: 로그인 API를 호출하고 AccessToken과 RefreshToken이 반환되는지 확인
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.accessToken").exists())
                .andExpect(jsonPath("$.result.refreshToken").exists());
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 잘못된 비밀번호")
    void login_Fail_InvalidPassword() throws Exception {
        // given: 유저 생성
        SignupRequestDto signupRequest = new SignupRequestDto(
                "실패테스트",
                "fail@example.com",
                LocalDate.of(1990, 1, 1),
                "01000000000",
                "correctPassword"
        );
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)));

        // 잘못된 비밀번호로 로그인 시도
        LoginRequestDto loginRequest = new LoginRequestDto("fail@example.com", "wrongPassword");

        // when & then: AUTH_4012 에러가 발생하는지 확인
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_4012"))
                .andExpect(jsonPath("$.isSuccess").value(false));
    }
}
