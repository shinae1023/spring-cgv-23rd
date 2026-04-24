package cgv_23rd.ceos.entity.reservation;

import cgv_23rd.ceos.entity.BaseEntity;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.theater.Seat;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더용, 외부 생성 방지
@Builder(access = AccessLevel.PRIVATE)
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

    public static Reservation create(User user, MovieScreen movieScreen, LocalDateTime now) {
        // 1. 상영 시작 여부 검사 로직을 엔티티 내부로 이동
        if (movieScreen.getStartAt().isBefore(now)) {
            throw new GeneralException(GeneralErrorCode.MOVIE_ALREADY_STARTED);
        }

        return Reservation.builder()
                .user(user)
                .movieScreen(movieScreen)
                .status(ReservationStatus.완료)
                .totalPrice(0)
                .reservationSeats(new ArrayList<>())
                .build();
    }

    // 예매 취소 편의 메서드
    public void cancel(LocalDateTime now) {
        if (this.status == ReservationStatus.취소) {
            throw new GeneralException(GeneralErrorCode.RESERVATION_ALREADY_CANCELED);
        }

        if (this.movieScreen.getStartAt().isBefore(now)) {
            throw new GeneralException(GeneralErrorCode.MOVIE_ALREADY_STARTED);
        }

        this.status = ReservationStatus.취소;
        this.reservationSeats.clear();
    }

    public void addSeat(Seat seat) {
        if (!seat.getScreen().getId().equals(this.movieScreen.getScreen().getId())) {
            throw new GeneralException(GeneralErrorCode.SEAT_SCREEN_INVALID);
        }

        Integer price = this.movieScreen.getScreen().getScreenType().getBasePrice();

        ReservationSeat reservationSeat = ReservationSeat.builder()
                .reservation(this)
                .seat(seat)
                .movieScreen(this.movieScreen)
                .price(price)
                .build();

        this.reservationSeats.add(reservationSeat);
        this.totalPrice += price;
    }

    public String getMovieTitle() {
        return this.movieScreen.getMovie().getTitle();
    }

    public String getTheaterName() {
        return this.movieScreen.getScreen().getTheater().getName();
    }

    public List<String> getSeatLabels() {
        return this.reservationSeats.stream()
                .map(rs -> rs.getSeat().getRowName() + rs.getSeat().getColNum())
                .toList();
    }

    public String getScreenName() {
        return this.movieScreen.getScreen().getName();
    }
}
