package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.movie.Actor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActorRepository extends JpaRepository<Actor,Long> {
}
