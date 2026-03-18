package cgv_23rd.ceos.domain.movie;

import cgv_23rd.ceos.domain.enums.MovieStatus;
import cgv_23rd.ceos.domain.like.MovieLike;
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

    @Id @GeneratedValue
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
}
