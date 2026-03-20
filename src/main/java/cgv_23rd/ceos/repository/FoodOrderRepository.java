package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.food.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
}
