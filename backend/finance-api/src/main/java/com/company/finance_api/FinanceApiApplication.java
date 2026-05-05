package com.company.finance_api;

import com.company.finance_api.config.ProfileAvatarProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ProfileAvatarProperties.class)
public class FinanceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceApiApplication.class, args);
	}

}
