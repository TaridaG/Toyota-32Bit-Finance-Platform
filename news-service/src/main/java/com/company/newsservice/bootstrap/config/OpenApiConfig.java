package com.company.newsservice.bootstrap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 3 spec and Swagger UI for news-service. */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI newsServiceOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("news-service")
                .version("1.0")
                .description("News feed, chart and admin metrics"))
        .servers(List.of(new Server().url("/api/v1").description("API gateway (canonical)")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
