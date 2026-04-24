package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.user.request.LoginRequestDto;
import cgv_23rd.ceos.dto.user.request.ReissueRequestDto;
import cgv_23rd.ceos.dto.user.request.SignupRequestDto;
import cgv_23rd.ceos.dto.user.response.LoginResponseDto;
import cgv_23rd.ceos.dto.user.response.ReissueResponseDto;
import cgv_23rd.ceos.entity.RefreshToken;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.entity.user.UserRole;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.global.jwt.JwtUtil;
import cgv_23rd.ceos.repository.RefreshTokenRepository;
import cgv_23rd.ceos.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public void signup(SignupRequestDto requestDto) {

        // 이메일 중복 확인
        if (userRepository.existsByEmail(requestDto.email())) {
            throw new GeneralException(GeneralErrorCode.DUPLICATE_LOGINID, "이미 존재하는 이메일입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(requestDto.password());

        // 유저 객체 생성 및 저장
        User user = User.builder()
                .email(requestDto.email())
                .password(encodedPassword)
                .name(requestDto.name())
                .birth(requestDto.birth())
                .phone(requestDto.phone())
                .role(UserRole.USER)
                .build();
        userRepository.save(user);
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto requestDto) {
        // 1. 이메일로 유저 조회
        User user = userRepository.findByEmail(requestDto.email())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_LOGIN));

        // 2. 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(requestDto.password(), user.getPassword())) {
            throw new GeneralException(GeneralErrorCode.INVALID_LOGIN);
        }

        // 3. 토큰 생성
        String accessToken = jwtUtil.createAccessToken(user.getEmail(), user.getId());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());

        RefreshToken token = refreshTokenRepository.findByUserId(user.getId());

        if(token != null){
            token.updateToken(refreshToken);
        }
        else {
            RefreshToken newRefreshToken = RefreshToken.builder()
                    .user(user)
                    .token(refreshToken)
                    .build();
            refreshTokenRepository.save(newRefreshToken);
        }

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    //AccessToken 재발급
    @Transactional
    public ReissueResponseDto reissueToken(ReissueRequestDto reissueRequestDto) {

        RefreshToken token = refreshTokenRepository.findByToken(reissueRequestDto.refreshToken())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_TOKEN));

        // 소유자 검증 (서비스가 직접 ID를 대조하지 않고 엔티티에게 물어봄)
        Claims accessClaims = jwtUtil.getClaimsFromExpiredToken(reissueRequestDto.accessToken());
        Long accessUserId = accessClaims.get("userId", Long.class);

        if (!token.isOwnedBy(accessUserId)) {
            throw new GeneralException(GeneralErrorCode.INVALID_TOKEN, "토큰 정보가 일치하지 않습니다.");
        }

        // 토큰 재발급
        User user = token.getUser();
        String newAccessToken = jwtUtil.createAccessToken(user.getEmail(), user.getId());
        String newRefreshToken = jwtUtil.createRefreshToken(user.getEmail());

        token.rotate(newRefreshToken);

        return ReissueResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
