package cn.sgnxotsmicf;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SuperAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuperAgentApplication.class, args);
    }


    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }
}
