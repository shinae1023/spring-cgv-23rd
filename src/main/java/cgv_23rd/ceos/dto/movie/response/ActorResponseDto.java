package cgv_23rd.ceos.dto.movie.response;

import cgv_23rd.ceos.domain.enums.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ActorResponseDto(String name,
                               Role role,
                               String profileUrl) {
}
