package cgv_23rd.ceos.domain.reservation;

import cgv_23rd.ceos.domain.enums.ReservationStatus;
import cgv_23rd.ceos.domain.movie.MovieScreen;
import cgv_23rd.ceos.domain.user.User;
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
public class Reservation {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_screen_id", nullable = false)
    private MovieScreen movieScreen;

    private Integer totalPrice;
    private LocalDateTime reserveDate;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationSeat> reservationSeats = new ArrayList<>();

    public void updateTotalPrice(int amount){
        this.totalPrice = amount;
    }

    public void updateStatus(ReservationStatus status){
        this.status = status;
    }
}
