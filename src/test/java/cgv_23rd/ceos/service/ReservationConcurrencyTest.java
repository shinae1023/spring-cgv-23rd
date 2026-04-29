package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.reservation.request.ReservationRequestDto;
import cgv_23rd.ceos.entity.enums.Region;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.entity.theater.Screen;
import cgv_23rd.ceos.entity.theater.ScreenType;
import cgv_23rd.ceos.entity.theater.Seat;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.*;
import cgv_23rd.ceos.repository.movie.MovieRepository;
import cgv_23rd.ceos.repository.movie.MovieScreenRepository;
import cgv_23rd.ceos.repository.reservation.ReservationRepository;
import cgv_23rd.ceos.repository.reservation.ReservationSeatRepository;
import cgv_23rd.ceos.repository.reservation.SeatRepository;
import cgv_23rd.ceos.repository.theater.ScreenRepository;
import cgv_23rd.ceos.repository.theater.ScreenTypeRepository;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationSeatRepository reservationSeatRepository;

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

    private Long movieScreenId;
    private Long anotherMovieScreenId;
    private Long seatA1Id;
    private Long seatA2Id;
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
                .description("동시성 테스트 극장")
                .imageUrl("https://example.com/theater.jpg")
                .build());

        Screen screen = screenRepository.saveAndFlush(Screen.builder()
                .theater(theater)
                .screenType(screenType)
                .name("1관")
                .totalSeat(2)
                .build());

        Seat seatA1 = seatRepository.saveAndFlush(Seat.builder()
                .screen(screen)
                .rowName("A")
                .colNum(1)
                .isActive(true)
                .build());

        Seat seatA2 = seatRepository.saveAndFlush(Seat.builder()
                .screen(screen)
                .rowName("A")
                .colNum(2)
                .isActive(true)
                .build());

        Movie movie = movieRepository.saveAndFlush(Movie.create(
                "동시성 테스트 영화",
                "예매 동시성 테스트용 영화",
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

        MovieScreen anotherMovieScreen = movieScreenRepository.saveAndFlush(MovieScreen.create(
                screen,
                movie,
                2,
                LocalDateTime.now().plusHours(5),
                LocalDateTime.now().plusHours(7)
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
        anotherMovieScreenId = anotherMovieScreen.getId();
        seatA1Id = seatA1.getId();
        seatA2Id = seatA2.getId();
        user1Id = user1.getId();
        user2Id = user2.getId();
    }

    @Test
    @DisplayName("같은 좌석을 동시에 예매하면 한 건만 성공한다")
    void createReservation_sameSeat_onlyOneSucceeds() throws Exception {
        AtomicInteger successCount = new AtomicInteger();
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        runConcurrently(
                () -> reserve(user1Id, List.of(seatA1Id), successCount, failures),
                () -> reserve(user2Id, List.of(seatA1Id), successCount, failures)
        );

        assertEquals(1, successCount.get());
        assertEquals(1, failures.size());
        assertEquals(1L, reservationRepository.count());
        assertEquals(1L, reservationSeatRepository.count());

        Throwable failure = failures.peek();
        assertInstanceOf(GeneralException.class, failure);
        assertEquals(GeneralErrorCode.RESERVATION_SEAT_DUPLICATION, ((GeneralException) failure).getCode());
    }

    @Test
    @DisplayName("다른 좌석을 동시에 예매하면 모두 성공한다")
    void createReservation_differentSeats_bothSucceed() throws Exception {
        AtomicInteger successCount = new AtomicInteger();
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        runConcurrently(
                () -> reserve(user1Id, List.of(seatA1Id), successCount, failures),
                () -> reserve(user2Id, List.of(seatA2Id), successCount, failures)
        );

        assertEquals(2, successCount.get());
        assertTrue(failures.isEmpty());
        assertEquals(2L, reservationRepository.count());
        assertEquals(2L, reservationSeatRepository.count());
    }

    @Test
    @DisplayName("같은 물리 좌석이어도 상영 회차가 다르면 동시에 예매할 수 있다")
    void createReservation_sameSeatDifferentMovieScreens_bothSucceed() throws Exception {
        AtomicInteger successCount = new AtomicInteger();
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        runConcurrently(
                () -> reserve(user1Id, movieScreenId, List.of(seatA1Id), successCount, failures),
                () -> reserve(user2Id, anotherMovieScreenId, List.of(seatA1Id), successCount, failures)
        );

        assertEquals(2, successCount.get());
        assertTrue(failures.isEmpty());
        assertEquals(2L, reservationRepository.count());
        assertEquals(2L, reservationSeatRepository.count());
    }

    private void reserve(Long userId, List<Long> seatIds, AtomicInteger successCount, Queue<Throwable> failures) {
        reserve(userId, movieScreenId, seatIds, successCount, failures);
    }

    private void reserve(
            Long userId,
            Long targetMovieScreenId,
            List<Long> seatIds,
            AtomicInteger successCount,
            Queue<Throwable> failures
    ) {
        try {
            reservationService.createReservation(userId, new ReservationRequestDto(targetMovieScreenId, seatIds));
            successCount.incrementAndGet();
        } catch (Throwable throwable) {
            failures.add(throwable);
        }
    }

    private void runConcurrently(Runnable firstTask, Runnable secondTask) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        try {
            executorService.submit(wrapTask(firstTask, readyLatch, startLatch, doneLatch));
            executorService.submit(wrapTask(secondTask, readyLatch, startLatch, doneLatch));

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
            startLatch.countDown();
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        } finally {
            executorService.shutdownNow();
        }
    }

    private Runnable wrapTask(
            Runnable task,
            CountDownLatch readyLatch,
            CountDownLatch startLatch,
            CountDownLatch doneLatch
    ) {
        return () -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                doneLatch.countDown();
            }
        };
    }
}
