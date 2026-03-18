package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.enums.MovieStatus;
import cgv_23rd.ceos.domain.movie.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import static cgv_23rd.ceos.domain.enums.MovieStatus.상영중;

public interface MovieRepository extends JpaRepository<Movie,Long> {
    List<Movie> findAllByStatus(MovieStatus movieStatus);
}
