package cgv_23rd.ceos.global.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class PaymentFeignConfig {

    @Bean
    public RequestInterceptor paymentRequestInterceptor(PaymentProperties paymentProperties) {
        return requestTemplate -> requestTemplate.header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + paymentProperties.apiSecret()
        );
    }
}
