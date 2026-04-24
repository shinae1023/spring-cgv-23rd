package cgv_23rd.ceos.dto.user.response;

import lombok.Builder;

@Builder
public record ReissueResponseDto(String accessToken, String refreshToken) {
}
