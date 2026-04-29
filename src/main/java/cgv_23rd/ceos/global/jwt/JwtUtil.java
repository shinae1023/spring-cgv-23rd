package cgv_23rd.ceos.global.jwt;

import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j(topic = "JwtUtil")
@Component
public class JwtUtil {

    @Value("${jwt.expiration.access-token}")
    private long accessTokenTime;

    @Value("${jwt.expiration.refresh-token}")
    private long refreshTokenTime;

    // "Authorization" 헤더의 KEY 값
    public static final String AUTHORIZATION_HEADER = "Authorization";
    // 토큰 식별자 (Bearer)
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret.key}")
    private String secretKey;
    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // Access Token 생성
    public String createAccessToken(String email, Long userId) {
        return createToken(email, userId, accessTokenTime);
    }

    // Refresh Token 생성
    public String createRefreshToken(String email) {
        return createToken(email, null, refreshTokenTime);
    }

    private String createToken(String email, Long userId, long expireTime) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireTime);

        JwtBuilder builder = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(key, signatureAlgorithm);

        if (userId != null) {
            builder.claim("userId", userId);
        }

        return builder.compact();
    }

    /**
     * 헤더에서 "Bearer " 접두사 제거
     */
    public String substringToken(String tokenValue) {
        if (tokenValue != null && tokenValue.startsWith(BEARER_PREFIX)) {
            return tokenValue.substring(BEARER_PREFIX.length());
        }
        // Bearer 접두사가 없거나 토큰 값이 null이면 예외 발생
        throw new GeneralException(GeneralErrorCode.INVALID_TOKEN,"유효하지 않은 토큰입니다.");
    }

    /**
     * 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature, 유효하지 않은 JWT 서명 입니다.");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token, 만료된 JWT token 입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
        }
        return false;
    }

    /**
     * 토큰에서 Claims 정보(데이터) 추출
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }

    /**
     * Claims에서 email(Subject) 추출
     */
    public String getEmailFromToken(Claims claims) {
        return claims.getSubject();
    }

    public Claims getClaimsFromExpiredToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이라도 서명이 유효하면 Claims를 반환함
            return e.getClaims();
        } catch (Exception e) {
            // 서명 위조나 잘못된 형식 등은 예외를 그대로 던져 차단함
            throw new GeneralException(GeneralErrorCode.INVALID_TOKEN, "유효하지 않은 토큰입니다.");
        }
    }
}