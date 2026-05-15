package com.ceos.voteservice.auth.controller;

import com.ceos.voteservice.auth.dto.LoginRequest;
import com.ceos.voteservice.auth.dto.SignUpRequest;
import com.ceos.voteservice.auth.dto.TokenResponse;
import com.ceos.voteservice.auth.dto.UserSummary;
import com.ceos.voteservice.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하고 JWT access token을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "회원가입", description = "아이디, 비밀번호, 이메일, 이름, 파트, 팀 정보를 입력해 회원가입합니다.")
    @PostMapping("/signup")
    public ResponseEntity<UserSummary> signUp(@Valid @RequestBody SignUpRequest request) {
        UserSummary user = authService.signUp(request);
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
    }
}
