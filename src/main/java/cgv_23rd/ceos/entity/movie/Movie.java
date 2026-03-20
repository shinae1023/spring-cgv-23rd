package cgv_23rd.ceos.entity.movie;

import cgv_23rd.ceos.entity.enums.MovieStatus;
import cgv_23rd.ceos.entity.like.MovieLike;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Movie {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    private MovieStatus status;

    private LocalDate openDate;
    private LocalDate closeDate;

    @OneToMany(mappedBy = "movie")
    private List<MovieImage> movieImages = new ArrayList<>();

    @OneToOne(mappedBy = "movie", cascade = CascadeType.ALL)
    private MovieStatistics movieStatistics;

    @OneToMany(mappedBy = "movie")
    private List<MovieScreen> movieScreens = new ArrayList<>();

    @OneToMany(mappedBy = "movie")
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "movie")
    private List<MovieLike> movieLikes = new ArrayList<>();

    @OneToMany(mappedBy = "movie")
    private List<MovieActor> movieActors = new ArrayList<>();

    //영화 생성 시 빈 통계 객체 생성을 위한 편의 method
    public void createDefaultStatistics() {
        this.movieStatistics = MovieStatistics.builder()
                .movie(this)
                .audienceCount(0)
                .reservationRate(0.0)
                .averageRating(0.0)
                .reviewCount(0)
                .build();
    }
}
