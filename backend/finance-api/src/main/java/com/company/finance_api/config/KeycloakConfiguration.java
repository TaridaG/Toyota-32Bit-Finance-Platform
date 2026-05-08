package com.company.finance_api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
        KeycloakAdminProperties.class,
        RegistrationProperties.class,
        RegistrationVerificationProperties.class
})
public class KeycloakConfiguration {

    @Bean
    public RestClient keycloakRestClient(KeycloakAdminProperties props) {
        String base = props.getServerUrl().replaceAll("/+$", "");
        return RestClient.builder().baseUrl(base).build();
    }
}
