package com.company.marketdataservice.bootstrap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 3 spec and Swagger UI for market-data-service. */
@Configuration
public class OpenApiConfig {

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
