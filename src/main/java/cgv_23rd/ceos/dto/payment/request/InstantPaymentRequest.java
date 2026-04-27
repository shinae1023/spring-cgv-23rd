package cgv_23rd.ceos.dto.payment.request;

public record InstantPaymentRequest(String storeId,
                                    String orderName,
                                    Integer totalPayAmount,
                                    String currency,
                                    String customData) {
}
