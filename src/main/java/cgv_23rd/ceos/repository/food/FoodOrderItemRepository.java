package cgv_23rd.ceos.repository.food;

import cgv_23rd.ceos.entity.food.FoodOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem, Long> {
}
