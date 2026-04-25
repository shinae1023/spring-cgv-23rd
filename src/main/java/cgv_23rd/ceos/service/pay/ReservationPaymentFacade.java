package cgv_23rd.ceos.service.pay;

import cgv_23rd.ceos.dto.payment.response.PaymentResponse;
import cgv_23rd.ceos.dto.payment.response.PaymentResultDto;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReservationPaymentFacade {

    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final PaymentCompensationService paymentCompensationService;

    @Transactional
    public PaymentResultDto processPayment(Long userId, Long reservationId) {
        Reservation reservation = reservationService.getOwnedReservationWithLock(userId, reservationId);

        if (reservation.getStatus() == ReservationStatus.완료) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        String paymentId = "RES_" + reservationId + "_" + UUID.randomUUID().toString().substring(0, 8);
        String orderName = reservation.getMovieTitle() + " 예매";
        String customData = "{\"reservationId\":" + reservation.getId() + "}";
        reservationService.assignPaymentId(reservation, paymentId);

        try {
            PaymentResponse response = paymentService.requestInstantPayment(
                    paymentId,
                    orderName,
                    reservation.getTotalPrice(),
                    customData
            );

            validatePaymentSuccess(response);

            try {
                reservationService.confirmReservation(reservation);
                return new PaymentResultDto(true, "결제가 완료되었습니다.");
            } catch (RuntimeException e) {
                paymentService.cancelPayment(paymentId);
                paymentCompensationService.cancelReservation(reservation.getId());
                throw e;
            }
        } catch (GeneralException e) {
            paymentCompensationService.cancelReservation(reservation.getId());
            throw e;
        } catch (Exception e) {
            paymentCompensationService.cancelReservation(reservation.getId());
            throw new GeneralException(GeneralErrorCode.PAYMENT_FAILED, "결제 처리 중 알 수 없는 오류가 발생했습니다.");
        }
    }

    @Transactional
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationService.getOwnedReservationWithLock(userId, reservationId);
        reservation.validateCancelable(java.time.LocalDateTime.now());

        if (reservation.getStatus() == ReservationStatus.완료) {
            if (reservation.getPaymentId() == null || reservation.getPaymentId().isBlank()) {
                throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY, "결제 식별자가 없는 완료 예매입니다.");
            }

            PaymentResponse response = paymentService.cancelPayment(reservation.getPaymentId());
            if (response == null || response.data() == null || !"CANCELLED".equals(response.data().paymentStatus())) {
                throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_CANCELLABLE);
            }
        }

        reservationService.cancelReservation(userId, reservationId);
    }

    private void validatePaymentSuccess(PaymentResponse response) {
        if (response == null || response.data() == null || !"PAID".equals(response.data().paymentStatus())) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_SERVER_FAILED);
        }
    }
}
