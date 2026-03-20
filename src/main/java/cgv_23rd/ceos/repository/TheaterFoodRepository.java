package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.food.Food;
import cgv_23rd.ceos.domain.food.TheaterFood;
import cgv_23rd.ceos.domain.theater.Theater;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TheaterFoodRepository extends JpaRepository<TheaterFood,Long> {
    Optional<TheaterFood> findByTheaterAndFood(Theater theater, Food food);
}
