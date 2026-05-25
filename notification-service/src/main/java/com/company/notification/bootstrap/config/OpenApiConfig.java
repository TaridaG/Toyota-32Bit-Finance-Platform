package com.company.notification.bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 3 spec and Swagger UI (actuator/health when exposed). */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI notificationOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("notification-service")
                .version("1.0")
                .description("Kafka-driven notification worker (no public REST API)"));
  }
}
