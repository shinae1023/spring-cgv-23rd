package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.like.MovieLike;
import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieLikeRepository extends JpaRepository<MovieLike,Long> {
    MovieLike findMovieLikeByUserAndMovie(User user, Movie movie);
}
