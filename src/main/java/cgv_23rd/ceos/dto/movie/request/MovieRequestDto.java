package cgv_23rd.ceos.dto.movie.request;

import java.time.LocalDate;

public record MovieRequestDto(String title, String description, LocalDate openDate, LocalDate closeDate) {
}
