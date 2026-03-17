package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.movie.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie,Long> {
}
