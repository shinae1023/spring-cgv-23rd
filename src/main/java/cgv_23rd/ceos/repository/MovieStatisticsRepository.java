package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.movie.MovieStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieStatisticsRepository extends JpaRepository<MovieStatistics,Long> {
}
