package cgv_23rd.ceos.service.query;

import cgv_23rd.ceos.dto.food.response.FoodOrderResponseDto;
import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.mapper.FoodOrderMapper;
import cgv_23rd.ceos.repository.food.FoodOrderRepository;
import cgv_23rd.ceos.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodOrderQueryService {

    private final FoodOrderRepository foodOrderRepository;
    private final UserService userService;
    private final FoodOrderMapper foodOrderMapper;

    public List<FoodOrderResponseDto> getFoodOrderList(Long userId) {
        userService.getUser(userId);

        List<FoodOrder> orders = foodOrderRepository.findAllByUserIdWithDetails(userId);
        return orders.stream()
                .map(foodOrderMapper::toResponse)
                .toList();
    }
}
