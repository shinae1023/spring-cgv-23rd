package cgv_23rd.ceos.entity.like;

import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_movie_like", columnNames = {"user_id", "movie_id"})
        }
)
public class MovieLike {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    public static MovieLike of(User user, Movie movie) {
        return MovieLike.builder()
                .user(user)
                .movie(movie)
                .build();
    }
}
