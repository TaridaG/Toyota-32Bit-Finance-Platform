package com.company.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Finance platform API Gateway Spring Boot giriş noktası.
 * Downstream microservice'lere route, JWT security ve cross-cutting filter'ları bootstrap eder.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /** Uygulama context'ini başlatır ve embedded reactive HTTP server'ı ayağa kaldırır. */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}