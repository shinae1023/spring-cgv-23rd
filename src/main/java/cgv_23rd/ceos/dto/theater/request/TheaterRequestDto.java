package cgv_23rd.ceos.dto.theater.request;

import cgv_23rd.ceos.entity.enums.Region;

public record TheaterRequestDto(String name,
                                String address,
                                String description,
                                String imageUrl,
                                Region region) {
}
