package cgv_23rd.ceos.dto.movie.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MovieRequestDto(@NotBlank String title,
                              @NotBlank String description,
                              @NotNull LocalDate openDate,
                              @NotNull LocalDate closeDate) {
}
