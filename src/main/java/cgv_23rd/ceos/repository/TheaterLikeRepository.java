package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.like.TheaterLike;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheaterLikeRepository extends JpaRepository<TheaterLike,Long> {
    TheaterLike findByUserAndTheater(User user, Theater theater);
}
