package cgv_23rd.ceos.service.admin;

import cgv_23rd.ceos.dto.food.request.FoodCreateRequestDto;
import cgv_23rd.ceos.entity.food.Food;
import cgv_23rd.ceos.entity.food.TheaterFood;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.food.FoodRepository;
import cgv_23rd.ceos.repository.food.TheaterFoodRepository;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminFoodService {
    private final FoodRepository foodRepository;
    private final TheaterRepository theaterRepository;
    private final TheaterFoodRepository theaterFoodRepository;

    //음식 등록
    @Transactional
    public void createFood(FoodCreateRequestDto requestDto) {

        Food food = Food.builder()
                .name(requestDto.name())
                .price(requestDto.price())
                .build();

        foodRepository.save(food);

        List<Theater> allTheaters = theaterRepository.findAll();

        List<TheaterFood> theaterFoods = allTheaters.stream()
                .map(theater -> TheaterFood.builder()
                        .theater(theater)
                        .food(food)
                        .amount(0)
                        .build())
                .collect(Collectors.toList());

        theaterFoodRepository.saveAll(theaterFoods);
    }

    //음식 재고 수정
    public void updateFoodStock(Long theaterFoodId, int stock){
        TheaterFood theaterFood = theaterFoodRepository.findById(theaterFoodId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.FOOD_NOT_FOUND));

        theaterFood.updateFoodStock(stock);
    }
}
