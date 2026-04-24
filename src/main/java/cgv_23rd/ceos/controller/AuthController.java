package cgv_23rd.ceos.controller;

import cgv_23rd.ceos.dto.user.request.LoginRequestDto;
import cgv_23rd.ceos.dto.user.request.ReissueRequestDto;
import cgv_23rd.ceos.dto.user.request.SignupRequestDto;
import cgv_23rd.ceos.dto.user.response.LoginResponseDto;
import cgv_23rd.ceos.dto.user.response.ReissueResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증 API", description = "회원가입, 로그인, 토큰 재발급 등 인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입 API", description = "새로운 유저를 등록합니다.")
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody @Valid SignupRequestDto requestDto) {
        authService.signup(requestDto);
        return ApiResponse.onSuccess("회원가입 성공");
    }

    @Operation(summary = "로그인 API", description = "이메일과 비밀번호로 로그인하여 Access/Refresh 토큰을 발급합니다.")
    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(@RequestBody @Valid LoginRequestDto requestDto) {
        return ApiResponse.onSuccess("로그인 성공",authService.login(requestDto));
    }

    @Operation(summary = "토큰 재발급 API", description = "만료된 Access Token을 Refresh Token을 통해 재발급합니다.")
    @PostMapping("/reissue")
    public ApiResponse<ReissueResponseDto> reissueToken(@RequestBody @Valid ReissueRequestDto requestDto) {
        return ApiResponse.onSuccess("토큰 재발급 성공",authService.reissueToken(requestDto));
    }
}