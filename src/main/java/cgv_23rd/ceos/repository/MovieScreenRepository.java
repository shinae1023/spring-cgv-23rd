package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.movie.MovieScreen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieScreenRepository extends JpaRepository<MovieScreen,Long> {
}
