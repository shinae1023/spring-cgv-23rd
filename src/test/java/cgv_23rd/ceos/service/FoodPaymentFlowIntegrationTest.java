package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.food.request.FoodOrderItemRequestDto;
import cgv_23rd.ceos.dto.food.request.FoodOrderRequestDto;
import cgv_23rd.ceos.dto.payment.response.PaymentResponse;
import cgv_23rd.ceos.entity.enums.FoodOrderStatus;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.enums.Region;
import cgv_23rd.ceos.entity.food.Food;
import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.entity.food.TheaterFood;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.food.FoodOrderRepository;
import cgv_23rd.ceos.repository.food.FoodRepository;
import cgv_23rd.ceos.repository.food.TheaterFoodRepository;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import cgv_23rd.ceos.repository.UserRepository;
import cgv_23rd.ceos.service.pay.FoodPaymentFacade;
import cgv_23rd.ceos.service.pay.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class FoodPaymentFlowIntegrationTest {

    @Autowired
    private FoodOrderService foodOrderService;

    @Autowired
    private FoodPaymentFacade foodPaymentFacade;

    @Autowired
    private FoodOrderRepository foodOrderRepository;

    @Autowired
    private TheaterFoodRepository theaterFoodRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private PaymentService paymentService;

    private Long userId;
    private Long theaterId;
    private Long foodId;

    @BeforeEach
    void setUp() {
        User user = userRepository.saveAndFlush(User.signup(
                "buyer",
                "01033333333",
                LocalDate.of(1999, 3, 3),
                "buyer@example.com",
                "password"
        ));

        Theater theater = theaterRepository.saveAndFlush(Theater.builder()
                .name("CGV 성수")
                .address("서울 성동구")
                .region(Region.서울)
                .isAvailable(true)
                .description("매점 결제 테스트 극장")
                .imageUrl("https://example.com/theater-food.jpg")
                .build());

        Food food = foodRepository.saveAndFlush(Food.builder()
                .name("팝콘")
                .description("카라멜 팝콘")
                .price(7000)
                .foodImageUrl("https://example.com/popcorn.jpg")
                .build());

        theaterFoodRepository.saveAndFlush(TheaterFood.builder()
                .theater(theater)
                .food(food)
                .amount(10)
                .build());

        userId = user.getId();
        theaterId = theater.getId();
        foodId = food.getId();
    }

    @Test
    @DisplayName("매점 재고는 결제 성공 응답을 받은 뒤에만 차감된다")
    void stockIsDeductedOnlyAfterPaymentSucceeds() {
        Long orderId = foodOrderService.createFoodOrder(
                userId,
                new FoodOrderRequestDto(theaterId, List.of(new FoodOrderItemRequestDto(foodId, 2)))
        );

        AtomicInteger stockAtPaymentCall = new AtomicInteger(-1);
        doAnswer(invocation -> {
            stockAtPaymentCall.set(currentStock());
            return paidResponse("PAID");
        }).when(paymentService).requestInstantPayment(anyString(), anyString(), anyInt(), anyString());

        foodPaymentFacade.processPayment(userId, orderId);

        FoodOrder order = foodOrderRepository.findById(orderId).orElseThrow();

        assertEquals(10, stockAtPaymentCall.get());
        assertEquals(8, currentStock());
        assertEquals(FoodOrderStatus.완료, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
    }

    @Test
    @DisplayName("결제가 실패하면 매점 재고는 차감되지 않고 주문은 대기 상태를 유지하며 결제 실패로 기록된다")
    void paymentFailure_doesNotDeductStockAndMarksPaymentFailed() {
        Long orderId = foodOrderService.createFoodOrder(
                userId,
                new FoodOrderRequestDto(theaterId, List.of(new FoodOrderItemRequestDto(foodId, 2)))
        );

        given(paymentService.requestInstantPayment(anyString(), anyString(), anyInt(), anyString()))
                .willThrow(new GeneralException(GeneralErrorCode.PAYMENT_FAILED));

        assertThrows(GeneralException.class, () -> foodPaymentFacade.processPayment(userId, orderId));

        FoodOrder order = foodOrderRepository.findById(orderId).orElseThrow();

        assertEquals(10, currentStock());
        assertEquals(FoodOrderStatus.대기, order.getStatus());
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    }

    @Test
    @DisplayName("완료된 매점 주문을 취소하면 결제 취소 후 재고가 복원된다")
    void cancelPaidOrder_restoresStock() {
        Long orderId = foodOrderService.createFoodOrder(
                userId,
                new FoodOrderRequestDto(theaterId, List.of(new FoodOrderItemRequestDto(foodId, 2)))
        );

        given(paymentService.requestInstantPayment(anyString(), anyString(), anyInt(), anyString()))
                .willReturn(paidResponse("PAID"));
        given(paymentService.cancelPayment(anyString()))
                .willReturn(paidResponse("CANCELLED"));

        foodPaymentFacade.processPayment(userId, orderId);
        assertEquals(8, currentStock());

        foodPaymentFacade.cancelOrder(userId, orderId);

        FoodOrder order = foodOrderRepository.findById(orderId).orElseThrow();

        assertEquals(10, currentStock());
        assertEquals(FoodOrderStatus.취소, order.getStatus());
        assertEquals(PaymentStatus.CANCELLED, order.getPaymentStatus());
    }

    private int currentStock() {
        return theaterFoodRepository.findAll().stream()
                .filter(theaterFood -> theaterFood.getTheater().getId().equals(theaterId))
                .filter(theaterFood -> theaterFood.getFood().getId().equals(foodId))
                .findFirst()
                .orElseThrow()
                .getAmount();
    }

    private PaymentResponse paidResponse(String paymentStatus) {
        return new PaymentResponse(
                200,
                "ok",
                new PaymentResponse.PaymentData(
                        "FOOD_1_12345678",
                        paymentStatus,
                        "CGV 성수 매점 주문",
                        "mockPg",
                        "KRW",
                        "{\"orderId\":1}",
                        "2026-04-25T15:00:00"
                )
        );
    }
}
