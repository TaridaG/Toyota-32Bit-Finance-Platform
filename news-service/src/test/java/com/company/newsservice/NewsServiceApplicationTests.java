package com.company.newsservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NewsServiceApplicationTests {

	@MockBean
	private JwtDecoder jwtDecoder;

	@MockBean
	private KafkaTemplate<String, Object> newsKafkaTemplate;

	@Test
	void contextLoads() {
	}

}
