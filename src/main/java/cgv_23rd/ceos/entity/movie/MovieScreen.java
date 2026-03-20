package cgv_23rd.ceos.entity.movie;

import cgv_23rd.ceos.entity.theater.Screen;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.entity.reservation.ReservationSeat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MovieScreen {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    private Integer sequence;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @OneToMany(mappedBy = "movieScreen")
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "movieScreen")
    private List<ReservationSeat> reservationSeats = new ArrayList<>();
}
