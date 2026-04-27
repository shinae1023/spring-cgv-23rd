package cgv_23rd.ceos.repository.movie;

import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.MovieActor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieActorRepository extends JpaRepository<MovieActor,Long> {
    @Query("SELECT ma FROM MovieActor ma JOIN FETCH ma.actor WHERE ma.movie = :movie")
    List<MovieActor> findAllByMovieWithActor(@Param("movie") Movie movie);
}
