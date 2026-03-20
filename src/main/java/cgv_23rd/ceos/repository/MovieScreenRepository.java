package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.movie.MovieScreen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovieScreenRepository extends JpaRepository<MovieScreen,Long> {
    // 상영 시간 겹침 검증
    @Query("SELECT COUNT(ms) > 0 FROM MovieScreen ms WHERE ms.screen.id = :screenId " +
            "AND ms.startAt < :endAt AND ms.endAt > :startAt")
    boolean existsOverlappingSchedule(@Param("screenId") Long screenId,
                                      @Param("startAt") LocalDateTime startAt,
                                      @Param("endAt") LocalDateTime endAt);

    // 극장과 날짜를 기준으로 상영 시간표 조회
    @Query("SELECT ms FROM MovieScreen ms " +
            "JOIN FETCH ms.movie m " +
            "JOIN FETCH ms.screen s " +
            "WHERE s.theater.id = :theaterId " +
            "AND ms.startAt >= :startOfDay AND ms.startAt <= :endOfDay " +
            "ORDER BY m.id, s.id, ms.startAt")
    List<MovieScreen> findByTheaterIdAndDateBetween(
            @Param("theaterId") Long theaterId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
}
