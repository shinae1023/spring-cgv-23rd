package cgv_23rd.ceos;

import cgv_23rd.ceos.global.config.PaymentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@EnableConfigurationProperties(PaymentProperties.class)
@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
@EnableFeignClients(basePackages = "cgv_23rd.ceos")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
