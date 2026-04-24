package cgv_23rd.ceos.global.jwt;

import cgv_23rd.ceos.global.security.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

// OncePerRequestFilter
@Slf4j(topic = "JWT 검증 및 인가")
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 헤더에서 토큰 값 가져오기
        String tokenValue = request.getHeader(JwtUtil.AUTHORIZATION_HEADER);

        if (StringUtils.hasText(tokenValue) && tokenValue.startsWith(JwtUtil.BEARER_PREFIX)) {
            // 2. Bearer 접두사 제거
            String token = jwtUtil.substringToken(tokenValue);

            // 3. 토큰 검증
            if (jwtUtil.validateToken(token)) {
                // 4. 토큰에서 사용자 정보 가져오기
                Claims claims = jwtUtil.getClaimsFromToken(token);

                // a. 토큰에서 email
                String email = jwtUtil.getEmailFromToken(claims);

                // b. email로 UserDetails 객체를 조회
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // c. userId 대신 UserDetails 객체로 인증 토큰을 생성 (권한 정보 포함)
                Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // d. SecurityContext에 인증 정보 저장
                SecurityContext context = SecurityContextHolder.getContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                log.info("사용자 인증 성공: email = {}", email);
            }
        }

        // 다음 필터로 요청/응답 전달
        filterChain.doFilter(request, response);
    }
}
