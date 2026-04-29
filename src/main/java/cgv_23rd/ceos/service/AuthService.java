package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.user.request.LoginRequestDto;
import cgv_23rd.ceos.dto.user.request.ReissueRequestDto;
import cgv_23rd.ceos.dto.user.request.SignupRequestDto;
import cgv_23rd.ceos.dto.user.response.LoginResponseDto;
import cgv_23rd.ceos.dto.user.response.ReissueResponseDto;
import cgv_23rd.ceos.entity.RefreshToken;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.global.jwt.JwtUtil;
import cgv_23rd.ceos.repository.auth.RefreshTokenRepository;
import cgv_23rd.ceos.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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
        if (userRepository.existsByEmail(requestDto.email())) {
            throw new GeneralException(GeneralErrorCode.DUPLICATE_LOGINID, "이미 존재하는 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(requestDto.password());

        User user = User.signup(
                requestDto.name(),
                requestDto.phone(),
                requestDto.birth(),
                requestDto.email(),
                encodedPassword
        );

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(GeneralErrorCode.DUPLICATE_LOGINID, "이미 존재하는 이메일입니다.");
        }
    }

    @Transactional
    @Retryable(
            retryFor = {
                    ObjectOptimisticLockingFailureException.class,
                    DataIntegrityViolationException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    public LoginResponseDto login(LoginRequestDto requestDto) {
        User user = getUserByEmail(requestDto.email());

        if (!passwordEncoder.matches(requestDto.password(), user.getPassword())) {
            throw new GeneralException(GeneralErrorCode.INVALID_LOGIN);
        }

        String accessToken = jwtUtil.createAccessToken(user.getEmail(), user.getId());
        String refreshTokenValue = jwtUtil.createRefreshToken(user.getEmail());

        upsertRefreshToken(user, refreshTokenValue);

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .build();
    }

    @Transactional
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    public ReissueResponseDto reissueToken(ReissueRequestDto reissueRequestDto) {
        RefreshToken token = refreshTokenRepository.findByToken(reissueRequestDto.refreshToken())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_TOKEN));

        Claims accessClaims = jwtUtil.getClaimsFromExpiredToken(reissueRequestDto.accessToken());
        Long accessUserId = accessClaims.get("userId", Long.class);

        if (!token.isOwnedBy(accessUserId)) {
            throw new GeneralException(GeneralErrorCode.INVALID_TOKEN, "토큰 정보가 일치하지 않습니다.");
        }

        User user = token.getUser();
        String newAccessToken = jwtUtil.createAccessToken(user.getEmail(), user.getId());
        String newRefreshToken = jwtUtil.createRefreshToken(user.getEmail());

        token.rotate(newRefreshToken);

        return ReissueResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private void upsertRefreshToken(User user, String refreshTokenValue) {
        refreshTokenRepository.findByUser_Id(user.getId())
                .ifPresentOrElse(
                        token -> token.rotate(refreshTokenValue),
                        () -> refreshTokenRepository.save(RefreshToken.create(user, refreshTokenValue))
                );
    }

    @Recover
    public LoginResponseDto recoverLogin(Exception e, LoginRequestDto requestDto) {
        throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "토큰 저장 충돌이 발생했습니다.");
    }

    @Recover
    public ReissueResponseDto recoverReissue(ObjectOptimisticLockingFailureException e, ReissueRequestDto requestDto) {
        throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "토큰 갱신 충돌이 발생했습니다.");
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_LOGIN));
    }
}
