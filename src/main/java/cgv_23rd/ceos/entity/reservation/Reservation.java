package cgv_23rd.ceos.entity.reservation;

import cgv_23rd.ceos.entity.BaseEntity;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.user.User;
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
public class Reservation extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_screen_id", nullable = false)
    private MovieScreen movieScreen;

    private Integer totalPrice;

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

    // 예매 취소 편의 메서드
    public void cancel() {
        this.status = ReservationStatus.취소;
        this.reservationSeats.clear();
    }
}
