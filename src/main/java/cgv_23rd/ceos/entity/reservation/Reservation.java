package cgv_23rd.ceos.entity.reservation;

import cgv_23rd.ceos.entity.BaseEntity;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
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
@Table(
        indexes = {
                @Index(name = "idx_reservation_status_created_at", columnList = "status, createdAt")
        }
)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(length = 100)
    private String paymentId;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationSeat> reservationSeats = new ArrayList<>();

    public static Reservation create(User user, MovieScreen movieScreen, LocalDateTime now) {
        // 상영 시작 여부 검사 로직을 엔티티 내부로 이동
        if (movieScreen.getStartAt().isBefore(now)) {
            throw new GeneralException(GeneralErrorCode.MOVIE_ALREADY_STARTED);
        }

        return Reservation.builder()
                .user(user)
                .movieScreen(movieScreen)
                .status(ReservationStatus.대기)
                .paymentStatus(PaymentStatus.READY)
                .totalPrice(0)
                .paymentId(null)
                .reservationSeats(new ArrayList<>())
                .build();
    }

    public void assignPaymentId(String paymentId) {
        if (this.status != ReservationStatus.대기) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY);
        }
        if (this.paymentStatus == PaymentStatus.PROCESSING || this.paymentStatus == PaymentStatus.PAID) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_ALREADY_PROCESSED, "이미 결제가 진행 중이거나 완료된 예매입니다.");
        }
        if (this.paymentId != null && !this.paymentId.isBlank()) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY, "이미 결제 식별자가 할당된 예매입니다.");
        }
        this.paymentId = paymentId;
        this.paymentStatus = PaymentStatus.PROCESSING;
    }

    public void confirm() {
        if (this.status != ReservationStatus.대기) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        if (this.paymentId == null || this.paymentId.isBlank()) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY, "결제 식별자가 없는 예매입니다.");
        }

        if (this.paymentStatus != PaymentStatus.PAID) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY, "결제 완료 상태의 예매만 확정할 수 있습니다.");
        }

        this.status = ReservationStatus.완료;
    }

    public void markPaymentPaid() {
        validatePaymentIdExists();
        this.paymentStatus = PaymentStatus.PAID;
    }

    public void markPaymentFailed() {
        validatePaymentIdExists();
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public void markPaymentUnknown() {
        validatePaymentIdExists();
        this.paymentStatus = PaymentStatus.UNKNOWN;
    }

    public void markPaymentCancelled() {
        validatePaymentIdExists();
        this.paymentStatus = PaymentStatus.CANCELLED;
    }

    public void validateCancelable(LocalDateTime now) {
        if (this.status == ReservationStatus.취소) {
            throw new GeneralException(GeneralErrorCode.RESERVATION_ALREADY_CANCELED);
        }

        if (this.movieScreen.getStartAt().isBefore(now)) {
            throw new GeneralException(GeneralErrorCode.MOVIE_ALREADY_STARTED);
        }
    }

    // 예매 취소 편의 메서드
    public void cancel(LocalDateTime now) {
        validateCancelable(now);
        this.status = ReservationStatus.취소;
        this.reservationSeats.clear();
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
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

    private void validatePaymentIdExists() {
        if (this.paymentId == null || this.paymentId.isBlank()) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY, "결제 식별자가 없는 예매입니다.");
        }
    }
}
