package cgv_23rd.ceos.auth.service;

import cgv_23rd.ceos.auth.dto.LoginRequest;
import cgv_23rd.ceos.auth.dto.SignUpRequest;
import cgv_23rd.ceos.auth.dto.TokenResponse;
import cgv_23rd.ceos.auth.dto.UserSummary;
import cgv_23rd.ceos.global.exception.DuplicateResourceException;
import cgv_23rd.ceos.global.security.JwtProvider;
import cgv_23rd.ceos.user.entity.User;
import cgv_23rd.ceos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.loginId(), request.password())
        );

        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BadCredentialsException("아이디 혹은 비밀번호가 올바르지 않습니다."));
        String accessToken = jwtProvider.createAccessToken(user);

        return new TokenResponse(
                "Bearer",
                accessToken,
                jwtProvider.getExpirationSeconds(),
                UserSummary.from(user)
        );
    }

    @Transactional
    public UserSummary signUp(SignUpRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new DuplicateResourceException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .name(request.name())
                .part(request.part())
                .team(request.team())
                .build();

        return UserSummary.from(userRepository.save(user));
    }
}
