package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.food.Food;
import cgv_23rd.ceos.entity.food.TheaterFood;
import cgv_23rd.ceos.entity.theater.Theater;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TheaterFoodRepository extends JpaRepository<TheaterFood,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TheaterFood> findByTheaterAndFood(Theater theater, Food food);

    /**
     * @Modifying: INSERT, UPDATE, DELETE 쿼리를 실행할 때 반드시 필요함.
     * @Query: DB의 원자적(Atomic) 연산을 사용하여 한 번에 재고를 차감함.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TheaterFood tf SET tf.amount = tf.amount - :quantity " +
            "WHERE tf.theater = :theater AND tf.food = :food AND tf.amount >= :quantity")
    int decreaseStock(@Param("theater") Theater theater,
                      @Param("food") Food food,
                      @Param("quantity") int quantity);
}
