package cgv_23rd.ceos.dto.theater.request;

import cgv_23rd.ceos.domain.enums.Region;
import lombok.Builder;

public record TheaterRequestDto(String name,
                                String address,
                                String description,
                                String imageUrl,
                                Region region) {
}
