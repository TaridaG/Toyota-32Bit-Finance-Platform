package com.company.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification microservice için Spring Boot giriş noktası.
 * Kafka consumer'ları, scheduled job'ları ve giden e-posta teslimatını çalıştırır.
 */
@SpringBootApplication
@EnableScheduling
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
