package com.company.marketdataservice.bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * market-data-service OpenAPI 3 tanımı ve Swagger UI ({@code /swagger-ui}).
 * Gateway üzerinden erişim için canonical server {@code /api/v1} olarak işaretlenir.
 */
@Configuration
public class OpenApiConfig {

  /** Swagger UI ve gateway uyumlu OpenAPI kök belgesi. */
  @Bean
  public OpenAPI marketDataOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("market-data-service")
                .version("1.0")
                .description("Market prices, rates, fundamentals and history"))
        .servers(List.of(new Server().url("/api/v1").description("API gateway (canonical)")));
  }
}
