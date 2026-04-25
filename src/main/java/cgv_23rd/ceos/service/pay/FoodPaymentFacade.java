package cgv_23rd.ceos.service.pay;

import cgv_23rd.ceos.dto.payment.response.PaymentResponse;
import cgv_23rd.ceos.dto.payment.response.PaymentResultDto;
import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.service.FoodOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FoodPaymentFacade {

    private final FoodOrderService foodOrderService;
    private final PaymentService paymentService;
    private final PaymentCompensationService paymentCompensationService;

    @Transactional
    public PaymentResultDto processPayment(Long userId ,Long orderId) {
        FoodOrder order = foodOrderService.getOwnedFoodOrderWithLock(userId, orderId);

        // 매점 결제용 고유 paymentId 생성
        String paymentId = "FOOD_" + orderId + "_" + UUID.randomUUID().toString().substring(0, 8);
        String orderName = order.getTheater().getName() + " 매점 주문";

        try {
            // 외부 결제 서버 API 호출
            PaymentResponse response = paymentService.requestInstantPayment(
                    paymentId,
                    orderName,
                    order.getTotalPrice(),
                    "{\"orderId\":" + order.getId() + "}"
            );

            if (response != null && response.data() != null && "PAID".equals(response.data().paymentStatus())) {
                try {
                    // 결제 성공 시 비관적 락을 획득하며 재고 차감 진행
                    foodOrderService.confirmOrderAndDeductStock(order);
                    return new PaymentResultDto(true, "매점 결제 및 주문이 완료되었습니다.");

                } catch (GeneralException e) {
                    // 재고 차감 실패 시 (OUT_OF_STOCK) 결제 취소 API 호출
                    paymentService.cancelPayment(paymentId);
                    throw e;
                }
            } else {
                throw new GeneralException(GeneralErrorCode.PAYMENT_SERVER_FAILED);
            }

        } catch (GeneralException e) {
            paymentCompensationService.cancelFoodOrder(order.getId());
            throw e;
        } catch (Exception e) {
            paymentCompensationService.cancelFoodOrder(order.getId());
            throw new GeneralException(GeneralErrorCode.PAYMENT_FAILED);
        }
    }
}
