package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.movie.MovieImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieImageRepository extends JpaRepository<MovieImage,Long> {
}
