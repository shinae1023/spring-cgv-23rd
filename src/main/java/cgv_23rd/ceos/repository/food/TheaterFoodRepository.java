package cgv_23rd.ceos.repository.food;

import cgv_23rd.ceos.entity.food.Food;
import cgv_23rd.ceos.entity.food.TheaterFood;
import cgv_23rd.ceos.entity.theater.Theater;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TheaterFoodRepository extends JpaRepository<TheaterFood,Long> {

    Optional<TheaterFood> findByTheaterAndFood(Theater theater, Food food);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tf FROM TheaterFood tf WHERE tf.theater = :theater AND tf.food = :food")
    Optional<TheaterFood> findByTheaterAndFoodWithLock(@Param("theater") Theater theater,
                                                       @Param("food") Food food);
}
