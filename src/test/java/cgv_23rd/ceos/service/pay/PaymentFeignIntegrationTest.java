package cgv_23rd.ceos.service.pay;

import cgv_23rd.ceos.global.config.PaymentFeignConfig;
import cgv_23rd.ceos.global.config.PaymentProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PaymentFeignIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class PaymentFeignIntegrationTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private PaymentService paymentService;

    @BeforeAll
    static void setUp() throws Exception {
        ensureServerStarted();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        ensureServerStarted();
        registry.add("payment.server.url", () -> mockWebServer.url("/").toString());
        registry.add("payment.store-id", () -> "test-store");
        registry.add("payment.api-secret", () -> "test-secret");
    }

    private static void ensureServerStarted() {
        if (mockWebServer != null) {
            return;
        }

        try {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
        } catch (Exception e) {
            throw new IllegalStateException("MockWebServer를 시작할 수 없습니다.", e);
        }
    }

    @Test
    @DisplayName("즉시 결제 요청 시 Feign이 결제 서버로 올바른 헤더와 바디를 전송한다")
    void requestInstantPayment_sendsExpectedRequest() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "status": 201,
                          "message": "ok",
                          "payload": {
                            "paymentId": "RES_1_12345678",
                            "paymentStatus": "PAID",
                            "orderName": "인셉션 예매",
                            "pgProvider": "mock-pg",
                            "currency": "KRW",
                            "customData": "{\\"reservationId\\":1}",
                            "paidAt": "2026-04-25T14:30:00"
                          }
                        }
                        """));

        var response = paymentService.requestInstantPayment(
                "RES_1_12345678",
                "인셉션 예매",
                15000,
                "{\"reservationId\":1}"
        );

        RecordedRequest request = mockWebServer.takeRequest();

        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/payments/RES_1_12345678/instant");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-secret");
        assertThat(request.getHeader("Content-Type")).contains("application/json");
        assertThat(request.getBody().readUtf8())
                .contains("\"storeId\":\"test-store\"")
                .contains("\"orderName\":\"인셉션 예매\"")
                .contains("\"totalPayAmount\":15000")
                .contains("\"currency\":\"KRW\"")
                .contains("\"customData\":\"{\\\"reservationId\\\":1}\"");

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(201);
        assertThat(response.data().paymentStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("즉시 결제 응답 본문이 비어 있으면 결제 내역 조회로 상태를 보완한다")
    void requestInstantPayment_emptyBody_fallsBackToGetPayment() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200));
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "status": 200,
                          "message": "결제 내역 조회",
                          "payload": {
                            "paymentId": "RES_1_12345678",
                            "paymentStatus": "PAID",
                            "orderName": "인셉션 예매",
                            "pgProvider": "mock-pg",
                            "currency": "KRW",
                            "customData": "{\\"reservationId\\":1}",
                            "paidAt": "2026-04-25T14:30:00"
                          }
                        }
                        """));

        var response = paymentService.requestInstantPayment(
                "RES_1_12345678",
                "인셉션 예매",
                15000,
                "{\"reservationId\":1}"
        );

        RecordedRequest instantRequest = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
        RecordedRequest getRequest = mockWebServer.takeRequest(3, TimeUnit.SECONDS);

        assertThat(instantRequest).isNotNull();
        assertThat(getRequest).isNotNull();
        assertThat(instantRequest.getMethod()).isEqualTo("POST");
        assertThat(instantRequest.getPath()).isEqualTo("/payments/RES_1_12345678/instant");
        assertThat(getRequest.getMethod()).isEqualTo("GET");
        assertThat(getRequest.getPath()).isEqualTo("/payments/RES_1_12345678");
        assertThat(response).isNotNull();
        assertThat(response.data().paymentStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("결제 취소 요청 시 Feign이 올바른 취소 엔드포인트를 호출한다")
    void cancelPayment_callsExpectedEndpoint() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "status": 200,
                          "message": "ok",
                          "payload": {
                            "paymentId": "RES_1_12345678",
                            "paymentStatus": "CANCELLED",
                            "orderName": "인셉션 예매",
                            "pgProvider": "mock-pg",
                            "currency": "KRW",
                            "customData": "{\\"reservationId\\":1}",
                            "paidAt": "2026-04-25T14:30:00"
                          }
                        }
                        """));

        var response = paymentService.cancelPayment("RES_1_12345678");

        RecordedRequest request = mockWebServer.takeRequest();

        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/payments/RES_1_12345678/cancel");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-secret");
        assertThat(response.data().paymentStatus()).isEqualTo("CANCELLED");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class
    })
    @EnableFeignClients(clients = PaymentFeignClient.class)
    @EnableConfigurationProperties(PaymentProperties.class)
    @Import({PaymentFeignConfig.class, PaymentService.class})
    static class TestApplication {
    }
}
