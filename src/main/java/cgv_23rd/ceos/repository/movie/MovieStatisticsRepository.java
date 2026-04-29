package cgv_23rd.ceos.repository.movie;

import cgv_23rd.ceos.entity.movie.MovieStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieStatisticsRepository extends JpaRepository<MovieStatistics,Long> {
}
