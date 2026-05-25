package com.company.analytics.bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 3 spec and Swagger UI for analytics-service. */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI analyticsOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("analytics-service")
                .version("1.0")
                .description("Portfolio analytics queries"));
  }
}
