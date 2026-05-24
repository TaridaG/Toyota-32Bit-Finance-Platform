package com.company.newsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * News microservice için Spring Boot giriş noktası.
 * RSS ingestion scheduler'ını ve haber sorgu API'sini çalıştırır.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class NewsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NewsServiceApplication.class, args);
	}
}