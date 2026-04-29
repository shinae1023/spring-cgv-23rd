package cgv_23rd.ceos.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        Server server,
        String storeId,
        String apiSecret
) {
    public record Server(String url) {}
}
