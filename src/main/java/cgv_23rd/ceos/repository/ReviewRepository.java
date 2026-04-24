package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.movie.Movie;
import cgv_23rd.ceos.entity.movie.Review;
import cgv_23rd.ceos.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review,Long> {
    boolean existsByUserAndMovie(User user, Movie movie); // 리뷰 중복 검사
    List<Review> findAllByMovieId(Long movieId); // 특정 영화의 리뷰 조회
}
