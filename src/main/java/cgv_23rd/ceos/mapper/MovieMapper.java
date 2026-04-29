package cgv_23rd.ceos.mapper;

import cgv_23rd.ceos.dto.movie.response.ActorResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieDetailResponseDto;
import cgv_23rd.ceos.dto.movie.response.MovieResponseDto;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.MovieActor;
import cgv_23rd.ceos.entity.movie.MovieImage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MovieMapper {

    public MovieResponseDto toResponse(Movie movie) {
        return MovieResponseDto.builder()
                .movieId(movie.getId())
                .title(movie.getTitle())
                .movieImageUrl(extractThumbnail(movie))
                .build();
    }

    public MovieDetailResponseDto toDetailResponse(Movie movie) {
        return MovieDetailResponseDto.builder()
                .title(movie.getTitle())
                .openDate(movie.getOpenDate())
                .description(movie.getDescription())
                .thumbnailUrl(extractThumbnail(movie))
                .imageUrls(extractImageUrls(movie))
                .audienceCount(movie.getMovieStatistics().getAudienceCount())
                .reservationRate(movie.getMovieStatistics().getReservationRate())
                .averageRating(movie.getMovieStatistics().getAverageRating())
                .eggRate(movie.getMovieStatistics().getEggRate())
                .build();
    }

    public ActorResponseDto toActorResponse(MovieActor movieActor) {
        return ActorResponseDto.builder()
                .name(movieActor.getActor().getName())
                .role(movieActor.getActor().getRole())
                .profileUrl(movieActor.getActor().getProfileImageUrl())
                .build();
    }

    private String extractThumbnail(Movie movie) {
        return movie.getMovieImages().stream()
                .filter(MovieImage::getIsThumbnail)
                .map(MovieImage::getMovieImageUrl)
                .findFirst()
                .orElse(null);
    }

    private List<String> extractImageUrls(Movie movie) {
        return movie.getMovieImages().stream()
                .map(MovieImage::getMovieImageUrl)
                .toList();
    }
}
