package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.movie.MovieActor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieActorRepository extends JpaRepository<MovieActor,Long> {
}
