package cgv_23rd.ceos.service.pay;

import cgv_23rd.ceos.dto.payment.response.PaymentResponse;
import cgv_23rd.ceos.dto.payment.response.PaymentResultDto;
import cgv_23rd.ceos.entity.enums.FoodOrderStatus;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.service.FoodOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FoodPaymentFacade {

    private final FoodOrderService foodOrderService;
    private final PaymentService paymentService;
    private final PaymentCompensationService paymentCompensationService;

    public PaymentResultDto processPayment(Long userId ,Long orderId) {
        FoodOrder order = foodOrderService.getOwnedFoodOrder(userId, orderId);

        // 매점 결제용 고유 paymentId 생성
        String paymentId = "FOOD_" + orderId + "_" + UUID.randomUUID().toString().substring(0, 8);
        String orderName = order.getTheater().getName() + " 매점 주문";
        foodOrderService.preparePayment(userId, orderId, paymentId);

        try {
            // 외부 결제 서버 API 호출
            PaymentResponse response = paymentService.requestInstantPayment(
                    paymentId,
                    orderName,
                    order.getTotalPrice(),
                    "{\"orderId\":" + order.getId() + "}"
            );

            if (response != null
                    && response.data() != null
                    && PaymentStatus.from(response.data().paymentStatus()) == PaymentStatus.PAID) {
                try {
                    // 결제 성공 뒤에만 짧은 로컬 트랜잭션을 열어 재고 차감과 주문 확정을 수행
                    foodOrderService.confirmOrderAndDeductStock(userId, orderId);
                    return new PaymentResultDto(true, "매점 결제 및 주문이 완료되었습니다.");

                } catch (GeneralException e) {
                    compensateFailedFoodOrder(userId, orderId, paymentId);
                    throw e;
                }
            } else {
                foodOrderService.markPaymentUnknown(userId, orderId);
                throw new GeneralException(GeneralErrorCode.PAYMENT_SERVER_FAILED);
            }

        } catch (GeneralException e) {
            updatePaymentStatusOnFailure(userId, orderId, e);
            throw e;
        } catch (Exception e) {
            foodOrderService.markPaymentUnknown(userId, orderId);
            throw new GeneralException(GeneralErrorCode.PAYMENT_FAILED);
        }
    }

    public void cancelOrder(Long userId, Long orderId) {
        FoodOrder order = foodOrderService.getOwnedFoodOrder(userId, orderId);

        if (order.getStatus() == FoodOrderStatus.완료) {
            if (order.getPaymentId() == null || order.getPaymentId().isBlank()) {
                throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY, "결제 식별자가 없는 완료 주문입니다.");
            }

            PaymentResponse response;
            try {
                response = paymentService.cancelPayment(order.getPaymentId());
            } catch (GeneralException e) {
                foodOrderService.markPaymentUnknown(userId, orderId);
                throw e;
            }

            if (response == null
                    || response.data() == null
                    || PaymentStatus.from(response.data().paymentStatus()) != PaymentStatus.CANCELLED) {
                foodOrderService.markPaymentUnknown(userId, orderId);
                throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_CANCELLABLE);
            }

            foodOrderService.cancelOrderAfterPaymentCancellation(userId, orderId);
            return;
        }

        foodOrderService.cancelPendingOrder(userId, orderId);
    }

    private void updatePaymentStatusOnFailure(Long userId, Long orderId, GeneralException e) {
        if (e.getCode() == GeneralErrorCode.PAYMENT_FAILED) {
            foodOrderService.markPaymentFailed(userId, orderId);
            return;
        }

        if (e.getCode() == GeneralErrorCode.PAYMENT_SERVER_FAILED
                || e.getCode() == GeneralErrorCode.EXTERNAL_SERVICE_TIMEOUT) {
            foodOrderService.markPaymentUnknown(userId, orderId);
        }
    }

    private void compensateFailedFoodOrder(Long userId, Long orderId, String paymentId) {
        try {
            paymentService.cancelPayment(paymentId);
        } catch (GeneralException e) {
            foodOrderService.markPaymentUnknown(userId, orderId);
            throw e;
        }

        try {
            paymentCompensationService.cancelFoodOrder(orderId);
        } catch (GeneralException e) {
            foodOrderService.markPaymentUnknown(userId, orderId);
            throw e;
        }
    }
}
