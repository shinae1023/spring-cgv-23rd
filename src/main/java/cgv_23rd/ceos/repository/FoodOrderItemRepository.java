package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.domain.food.FoodOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem, Long> {
}
