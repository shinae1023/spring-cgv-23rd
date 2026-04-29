package cgv_23rd.ceos.service.pay;

import cgv_23rd.ceos.dto.payment.request.InstantPaymentRequest;
import cgv_23rd.ceos.dto.payment.response.PaymentResponse;
import cgv_23rd.ceos.global.config.PaymentFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "paymentClient",
        url = "${payment.server.url}",
        configuration = PaymentFeignConfig.class
)
public interface PaymentFeignClient {

    @PostMapping(value = "/payments/{paymentId}/instant", consumes = "application/json")
    PaymentResponse requestInstantPayment(
            @PathVariable("paymentId") String paymentId,
            @RequestBody InstantPaymentRequest request
    );

    @PostMapping("/payments/{paymentId}/cancel")
    PaymentResponse cancelPayment(@PathVariable("paymentId") String paymentId);

    @GetMapping("/payments/{paymentId}")
    PaymentResponse getPayment(@PathVariable("paymentId") String paymentId);
}
