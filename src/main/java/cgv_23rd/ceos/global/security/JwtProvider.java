package cgv_23rd.ceos.global.security;

import cgv_23rd.ceos.user.entity.User;
import cgv_23rd.ceos.user.entity.UserRole;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class JwtProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final RoleHierarchy roleHierarchy;

    @Getter
    private final long expirationSeconds;

    public JwtProvider(
            ObjectMapper objectMapper,
            RoleHierarchy roleHierarchy,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.objectMapper = objectMapper;
        this.roleHierarchy = roleHierarchy;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationMinutes * 60;
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> payload = Map.of(
                "sub", user.getLoginId(),
                "userId", user.getId(),
                "name", user.getName(),
                "role", user.getRole().name(),
                "iat", now.getEpochSecond(),
                "exp", now.plusSeconds(expirationSeconds).getEpochSecond()
        );

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String unsignedToken = encodedHeader + "." + encodedPayload;

        return unsignedToken + "." + sign(unsignedToken);
    }

    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String unsignedToken = parts[0] + "." + parts[1];
            if (!sign(unsignedToken).equals(parts[2])) {
                return false;
            }

            Map<String, Object> payload = parsePayload(parts[1]);
            long expiration = ((Number) payload.get("exp")).longValue();
            return expiration > Instant.now().getEpochSecond();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Map<String, Object> payload = parsePayload(token.split("\\.")[1]);
        String loginId = (String) payload.get("sub");
        UserRole role = UserRole.valueOf((String) payload.get("role"));
        List<GrantedAuthority> authorities = List.copyOf(roleHierarchy.getReachableGrantedAuthorities(
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        ));
        return new UsernamePasswordAuthenticationToken(
                payload.get("userId").toString(),
                null,
                authorities
        );
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            return objectMapper.readValue(decoded, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalArgumentException("JWT payload를 읽을 수 없습니다.", exception);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException("JWT 값을 만들 수 없습니다.", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 서명을 만들 수 없습니다.", exception);
        }
    }
}
