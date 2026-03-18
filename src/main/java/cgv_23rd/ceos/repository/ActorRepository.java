package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.movie.Actor;
import cgv_23rd.ceos.domain.movie.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActorRepository extends JpaRepository<Actor,Long> {
}
