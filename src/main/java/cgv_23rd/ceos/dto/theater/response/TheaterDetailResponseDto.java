package cgv_23rd.ceos.dto.theater.response;

import lombok.Builder;

@Builder
public record TheaterDetailResponseDto(Long id, String name,
                                       String address,
                                       Boolean isActive,
                                       String description,
                                       String imageUrl,
                                       Boolean isAvailable
) {
}
