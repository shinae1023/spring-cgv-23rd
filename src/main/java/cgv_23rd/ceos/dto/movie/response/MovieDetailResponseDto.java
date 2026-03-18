package cgv_23rd.ceos.dto.movie.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
//(제목, 개봉일, 예매율, 누적관객, 설명, 에그지수, 사진들)
public record MovieDetailResponseDto(String title, LocalDate openDate, String description, String thumbnailUrl,
                                     List<String> imageUrls, Integer audienceCount, Double reservationRate,
                                     Double averageRating, Double eggRate) {
}
