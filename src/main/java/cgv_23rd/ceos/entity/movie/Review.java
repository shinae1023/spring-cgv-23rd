package cgv_23rd.ceos.entity.movie;

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
                @UniqueConstraint(name = "uk_user_movie_review", columnNames = {"user_id", "movie_id"})
        }
)
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double rate;
    private String content;

    @Version
    private Long version;

    public static Review create(User user, Movie movie, Double rate, String content) {
        return Review.builder()
                .user(user)
                .movie(movie)
                .rate(rate)
                .content(content)
                .build();
    }
}
