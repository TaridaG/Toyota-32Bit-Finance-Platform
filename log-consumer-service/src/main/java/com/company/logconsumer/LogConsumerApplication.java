package com.company.logconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Log consumer servisi: Kafka app.logs → OpenSearch indexleme ve internal metrik API.
 */
@SpringBootApplication
public class LogConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogConsumerApplication.class, args);
    }
}