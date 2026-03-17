package cgv_23rd.ceos.service;

import cgv_23rd.ceos.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class FoodOrderService {
    private final FoodRepository foodRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final UserRepository userRepository;
    private final TheaterFoodRepository theaterFoodRepository;

    // 1. 음식 주문

    // 2. 주문 내역 확인
}
