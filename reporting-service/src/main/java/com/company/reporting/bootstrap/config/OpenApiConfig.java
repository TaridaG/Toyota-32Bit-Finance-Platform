package com.company.reporting.bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 3 spec and Swagger UI for reporting-service. */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI reportingOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("reporting-service")
                .version("1.0")
                .description("Report generation and export"));
  }
}
