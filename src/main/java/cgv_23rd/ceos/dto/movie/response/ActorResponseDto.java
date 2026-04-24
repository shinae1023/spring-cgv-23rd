package cgv_23rd.ceos.dto.movie.response;

import cgv_23rd.ceos.entity.enums.Role;
import lombok.Builder;

@Builder
public record ActorResponseDto(String name,
                               Role role,
                               String profileUrl) {
}
