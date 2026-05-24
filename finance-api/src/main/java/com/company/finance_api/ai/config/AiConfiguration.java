package com.company.finance_api.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** AI modülü Spring bean yapılandırması. */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {}
