package com.ceos.voteservice.auth.dto;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        UserSummary user
) {
}
