package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.enums.MovieStatus;
import cgv_23rd.ceos.entity.movie.Movie;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie,Long> {

    @EntityGraph(attributePaths = {"movieImages"})
    @Query("SELECT m FROM Movie m JOIN m.movieStatistics ms WHERE m.status = :status ORDER BY ms.reservationRate DESC")
    List<Movie> findAllByStatusOrderByReservationRateDesc(@Param("status") MovieStatus status);

    @Query("SELECT m FROM Movie m " +
            "LEFT JOIN FETCH m.movieImages " +
            "JOIN FETCH m.movieStatistics " +
            "WHERE m.id = :movieId")
    Optional<Movie> findDetailById(@Param("movieId") Long movieId);
}
