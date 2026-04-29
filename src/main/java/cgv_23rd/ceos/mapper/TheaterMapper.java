package cgv_23rd.ceos.mapper;

import cgv_23rd.ceos.dto.theater.response.TheaterDetailResponseDto;
import cgv_23rd.ceos.dto.theater.response.TheaterResponseDto;
import cgv_23rd.ceos.entity.theater.Theater;
import org.springframework.stereotype.Component;

@Component
public class TheaterMapper {

    public TheaterResponseDto toResponse(Theater theater) {
        return TheaterResponseDto.builder()
                .id(theater.getId())
                .name(theater.getName())
                .address(theater.getAddress())
                .isActive(theater.getIsAvailable())
                .build();
    }

    public TheaterDetailResponseDto toDetailResponse(Theater theater) {
        return TheaterDetailResponseDto.builder()
                .id(theater.getId())
                .name(theater.getName())
                .address(theater.getAddress())
                .isActive(theater.getIsAvailable())
                .description(theater.getDescription())
                .imageUrl(theater.getImageUrl())
                .isAvailable(theater.getIsAvailable())
                .build();
    }
}
