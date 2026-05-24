package com.company.finance_api.bootstrap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Keycloak kimlik ve kayıt/MFA/trusted-device config property bean'lerini ve outbound REST client'ı
 * bağlar.
 */
@Configuration
@EnableConfigurationProperties({
  KeycloakAdminProperties.class,
  RegistrationProperties.class,
  RegistrationVerificationProperties.class,
  MfaProperties.class,
  TrustedDeviceProperties.class
})
public class KeycloakConfiguration {

  /** Keycloak server base URL'ine bağlı {@link RestClient} bean'i. */
  @Bean
  public RestClient keycloakRestClient(KeycloakAdminProperties props) {
    String base = props.getServerUrl().replaceAll("/+$", "");
    return RestClient.builder().baseUrl(base).build();
  }
}
