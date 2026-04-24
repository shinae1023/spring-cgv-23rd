package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.food.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    @Query("SELECT DISTINCT fo FROM FoodOrder fo " +
            "JOIN FETCH fo.theater t " +
            "LEFT JOIN FETCH fo.foodOrderItems foi " +
            "LEFT JOIN FETCH foi.food f " +
            "WHERE fo.user.id = :userId")
    List<FoodOrder> findAllByUserIdWithDetails(@Param("userId") Long userId);

}
