package cgv_23rd.ceos.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonAlias;

public record PaymentResponse(
        @JsonAlias("status")
        Integer code,
        String message,
        @JsonAlias("payload")
        PaymentData data
) {
    public record PaymentData(
            String paymentId,
            String paymentStatus,
            String orderName,
            String pgProvider,
            String currency,
            String customData,
            String paidAt
    ) {
    }
}
