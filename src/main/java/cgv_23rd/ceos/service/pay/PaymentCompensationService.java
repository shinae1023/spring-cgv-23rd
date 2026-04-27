package cgv_23rd.ceos.service.pay;

import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.food.FoodOrderRepository;
import cgv_23rd.ceos.repository.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentCompensationService {

    private final ReservationRepository reservationRepository;
    private final FoodOrderRepository foodOrderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.RESERVATION_NOT_FOUND));
        reservation.markPaymentCancelled();
        reservation.cancel(LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelFoodOrder(Long orderId) {
        FoodOrder foodOrder = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FOOD_ORDER_NOT_FOUND));
        foodOrder.markPaymentCancelled();
        foodOrder.cancel();
    }
}
