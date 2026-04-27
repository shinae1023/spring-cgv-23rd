package cgv_23rd.ceos.repository.food;

import cgv_23rd.ceos.entity.food.Food;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food,Long> {
}
