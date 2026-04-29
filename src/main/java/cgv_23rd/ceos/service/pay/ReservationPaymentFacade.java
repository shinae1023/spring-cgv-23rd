package cgv_23rd.ceos.service.pay;

import cgv_23rd.ceos.dto.payment.response.PaymentResponse;
import cgv_23rd.ceos.dto.payment.response.PaymentResultDto;
import cgv_23rd.ceos.entity.enums.PaymentStatus;
import cgv_23rd.ceos.entity.enums.ReservationStatus;
import cgv_23rd.ceos.entity.reservation.Reservation;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReservationPaymentFacade {

    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final PaymentCompensationService paymentCompensationService;

    public PaymentResultDto processPayment(Long userId, Long reservationId) {
        Reservation reservation = reservationService.getOwnedReservation(userId, reservationId);

        if (reservation.getStatus() == ReservationStatus.완료) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        String paymentId = "RES_" + reservationId + "_" + UUID.randomUUID().toString().substring(0, 8);
        String orderName = reservation.getMovieTitle() + " 예매";
        String customData = "{\"reservationId\":" + reservation.getId() + "}";
        reservationService.preparePayment(userId, reservationId, paymentId);

        try {
            PaymentResponse response = paymentService.requestInstantPayment(
                    paymentId,
                    orderName,
                    reservation.getTotalPrice(),
                    customData
            );

            validatePaymentSuccess(response);

            try {
                reservationService.confirmReservation(userId, reservationId);
                return new PaymentResultDto(true, "결제가 완료되었습니다.");
            } catch (RuntimeException e) {
                compensateFailedReservation(userId, reservationId, paymentId);
                throw e;
            }
        } catch (GeneralException e) {
            updatePaymentStatusOnFailure(userId, reservationId, e);
            throw e;
        } catch (Exception e) {
            reservationService.markPaymentUnknown(userId, reservationId);
            throw new GeneralException(GeneralErrorCode.PAYMENT_FAILED, "결제 처리 중 알 수 없는 오류가 발생했습니다.");
        }
    }

    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationService.getOwnedReservation(userId, reservationId);
        reservation.validateCancelable(java.time.LocalDateTime.now());

        if (reservation.getStatus() == ReservationStatus.완료) {
            if (reservation.getPaymentId() == null || reservation.getPaymentId().isBlank()) {
                throw new GeneralException(GeneralErrorCode.PAYMENT_NOT_READY, "결제 식별자가 없는 완료 예매입니다.");
            }

            reservationService.cancelPaidReservation(userId, reservationId, reservation.getPaymentId());
            return;
        }

        reservationService.cancelReservation(userId, reservationId);
    }

    private void validatePaymentSuccess(PaymentResponse response) {
        if (response == null
                || response.data() == null
                || PaymentStatus.from(response.data().paymentStatus()) != PaymentStatus.PAID) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_SERVER_FAILED);
        }
    }

    private void updatePaymentStatusOnFailure(Long userId, Long reservationId, GeneralException e) {
        if (e.getCode() == GeneralErrorCode.PAYMENT_FAILED) {
            reservationService.markPaymentFailed(userId, reservationId);
            return;
        }

        if (e.getCode() == GeneralErrorCode.PAYMENT_SERVER_FAILED
                || e.getCode() == GeneralErrorCode.EXTERNAL_SERVICE_TIMEOUT) {
            reservationService.markPaymentUnknown(userId, reservationId);
        }
    }

    private void compensateFailedReservation(Long userId, Long reservationId, String paymentId) {
        try {
            paymentService.cancelPayment(paymentId);
        } catch (GeneralException e) {
            reservationService.markPaymentUnknown(userId, reservationId);
            throw e;
        }

        try {
            paymentCompensationService.cancelReservation(reservationId);
        } catch (GeneralException e) {
            reservationService.markPaymentUnknown(userId, reservationId);
            throw e;
        }
    }

}
