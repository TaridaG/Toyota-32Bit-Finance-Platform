package com.company.notification.bootstrap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Giden e-posta için {@link NotificationMailProperties} binding'ini etkinleştirir.
 */
@Configuration
@EnableConfigurationProperties(NotificationMailProperties.class)
public class NotificationMailConfig {
}
