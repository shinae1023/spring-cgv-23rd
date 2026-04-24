package cgv_23rd.ceos.dto.user.response;

import lombok.Builder;

@Builder
public record LoginResponseDto(String accessToken, String refreshToken) {
}
