package cgv_23rd.ceos.entity.enums;

import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;

public enum PaymentStatus {
    READY,
    PROCESSING,
    PAID,
    FAILED,
    CANCELLED,
    UNKNOWN;

    public static PaymentStatus from(String value) {
        try {
            return PaymentStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new GeneralException(GeneralErrorCode.PAYMENT_SERVER_FAILED, "알 수 없는 결제 상태입니다. status=" + value);
        }
    }
}
