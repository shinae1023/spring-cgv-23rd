package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.reservation.request.ReservationRequestDto;
import cgv_23rd.ceos.entity.enums.Region;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.entity.theater.Screen;
import cgv_23rd.ceos.entity.theater.ScreenType;
import cgv_23rd.ceos.entity.theater.Seat;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.movie.MovieRepository;
import cgv_23rd.ceos.repository.movie.MovieScreenRepository;
import cgv_23rd.ceos.repository.reservation.ReservationRepository;
import cgv_23rd.ceos.repository.reservation.ReservationSeatRepository;
import cgv_23rd.ceos.repository.theater.ScreenRepository;
import cgv_23rd.ceos.repository.theater.ScreenTypeRepository;
import cgv_23rd.ceos.repository.reservation.SeatRepository;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import cgv_23rd.ceos.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PendingReservationHoldIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private PendingOrderExpirationService pendingOrderExpirationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationSeatRepository reservationSeatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieScreenRepository movieScreenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ScreenTypeRepository screenTypeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long movieScreenId;
    private Long seatA1Id;
    private Long user1Id;
    private Long user2Id;

    @BeforeEach
    void setUp() {
        ScreenType screenType = screenTypeRepository.saveAndFlush(ScreenType.builder()
                .typeName("2D")
                .basePrice(15000)
                .build());

        Theater theater = theaterRepository.saveAndFlush(Theater.builder()
                .name("CGV 강남")
                .address("서울 강남구")
                .region(Region.서울)
                .isAvailable(true)
                .description("대기 좌석 테스트 극장")
                .imageUrl("https://example.com/theater.jpg")
                .build());

        Screen screen = screenRepository.saveAndFlush(Screen.builder()
                .theater(theater)
                .screenType(screenType)
                .name("1관")
                .totalSeat(1)
                .build());

        Seat seatA1 = seatRepository.saveAndFlush(Seat.builder()
                .screen(screen)
                .rowName("A")
                .colNum(1)
                .isActive(true)
                .build());

        Movie movie = movieRepository.saveAndFlush(Movie.create(
                "대기 좌석 테스트 영화",
                "좌석 홀드 테스트",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(7)
        ));

        MovieScreen movieScreen = movieScreenRepository.saveAndFlush(MovieScreen.create(
                screen,
                movie,
                1,
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(4)
        ));

        User user1 = userRepository.saveAndFlush(User.signup(
                "user1",
                "01011111111",
                LocalDate.of(2000, 1, 1),
                "user1@example.com",
                "password"
        ));

        User user2 = userRepository.saveAndFlush(User.signup(
                "user2",
                "01022222222",
                LocalDate.of(2000, 1, 2),
                "user2@example.com",
                "password"
        ));

        movieScreenId = movieScreen.getId();
        seatA1Id = seatA1.getId();
        user1Id = user1.getId();
        user2Id = user2.getId();
    }

    @Test
    @DisplayName("대기 예매는 5분 만료 전까지 좌석을 점유하고 만료 후에는 해제된다")
    void pendingReservation_blocksSeatUntilExpiredThenReleasesIt() {
        Long reservationId = reservationService.createReservation(
                user1Id,
                new ReservationRequestDto(movieScreenId, List.of(seatA1Id))
        );

        GeneralException blocked = assertThrows(
                GeneralException.class,
                () -> reservationService.createReservation(
                        user2Id,
                        new ReservationRequestDto(movieScreenId, List.of(seatA1Id))
                )
        );
        assertEquals(GeneralErrorCode.RESERVATION_SEAT_DUPLICATION, blocked.getCode());

        jdbcTemplate.update(
                "update reservation set created_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(6)),
                reservationId
        );

        int expiredCount = pendingOrderExpirationService.expirePendingReservations();

        Long newReservationId = reservationService.createReservation(
                user2Id,
                new ReservationRequestDto(movieScreenId, List.of(seatA1Id))
        );

        Reservation expiredReservation = reservationRepository.findById(reservationId).orElseThrow();
        Reservation newReservation = reservationRepository.findById(newReservationId).orElseThrow();

        assertEquals(1, expiredCount);
        assertEquals(ReservationStatus.취소, expiredReservation.getStatus());
        assertEquals(ReservationStatus.대기, newReservation.getStatus());
        assertEquals(1L, reservationSeatRepository.count());
    }

    @Test
    @DisplayName("결제 미확정 대기 예매는 5분이 지나도 자동 취소되지 않는다")
    void unknownPaymentPendingReservation_isNotExpiredAutomatically() {
        Long reservationId = reservationService.createReservation(
                user1Id,
                new ReservationRequestDto(movieScreenId, List.of(seatA1Id))
        );

        jdbcTemplate.update(
                "update reservation set payment_status = ?, created_at = ? where id = ?",
                "UNKNOWN",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(6)),
                reservationId
        );

        int expiredCount = pendingOrderExpirationService.expirePendingReservations();

        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();

        GeneralException blocked = assertThrows(
                GeneralException.class,
                () -> reservationService.createReservation(
                        user2Id,
                        new ReservationRequestDto(movieScreenId, List.of(seatA1Id))
                )
        );

        assertEquals(0, expiredCount);
        assertEquals(ReservationStatus.대기, reservation.getStatus());
        assertEquals(GeneralErrorCode.RESERVATION_SEAT_DUPLICATION, blocked.getCode());
        assertEquals(1L, reservationSeatRepository.count());
    }
}
