package cgv_23rd.ceos.dto.theater.response;

import lombok.Builder;

@Builder
public record TheaterResponseDto(
        Long id,
        String name,
        String address,
        Boolean isActive) {
}
