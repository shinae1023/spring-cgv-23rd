package cgv_23rd.ceos.auth.dto;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        UserSummary user
) {
}
