package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.food.Food;
import cgv_23rd.ceos.entity.food.TheaterFood;
import cgv_23rd.ceos.entity.theater.Theater;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TheaterFoodRepository extends JpaRepository<TheaterFood,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TheaterFood> findByTheaterAndFood(Theater theater, Food food);
}
