package cgv_23rd.ceos.dto.movie.response;

import lombok.Builder;

@Builder
public record MovieResponseDto(String title, String movieImageUrl) {
}
