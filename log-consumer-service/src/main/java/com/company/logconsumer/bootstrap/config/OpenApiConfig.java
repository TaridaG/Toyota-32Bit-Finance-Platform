package com.company.logconsumer.bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 3 spec and Swagger UI for log-consumer-service. */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI logConsumerOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("log-consumer-service")
                .version("1.0")
                .description("Internal operations and system intelligence endpoints"));
  }
}
