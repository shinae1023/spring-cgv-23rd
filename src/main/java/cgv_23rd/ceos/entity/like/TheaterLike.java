package cgv_23rd.ceos.entity.like;

import cgv_23rd.ceos.entity.theater.Theater;
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
                @UniqueConstraint(name = "uk_user_theater_like", columnNames = {"user_id", "theater_id"})
        }
)
public class TheaterLike {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    public static TheaterLike of(User user, Theater theater) {
        return TheaterLike.builder()
                .user(user)
                .theater(theater)
                .build();
    }
}
