package cgv_23rd.ceos.entity.reservation;

import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.theater.Seat;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_movie_screen_seat", columnNames = {"movie_screen_id", "seat_id"})
        }
)
public class ReservationSeat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_screen_id", nullable = false)
    private MovieScreen movieScreen;

    private Integer price;
}
