package com.company.finance_api.registration;

import com.company.finance_api.config.RegistrationProperties;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PublicRegisterRequest;
import com.company.finance_api.dto.PublicRegisterResponse;
import com.company.finance_api.identity.KeycloakRealmAdminClient;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class PortalRegistrationService {

    private static final String REALM_USER_ROLE = "USER";

    private final RegistrationProperties registrationProperties;
    private final KeycloakRealmAdminClient keycloakRealmAdminClient;
    private final UserRepository userRepository;
    private final UserService userService;

    public PortalRegistrationService(
            RegistrationProperties registrationProperties,
            KeycloakRealmAdminClient keycloakRealmAdminClient,
            UserRepository userRepository,
            UserService userService
    ) {
        this.registrationProperties = registrationProperties;
        this.keycloakRealmAdminClient = keycloakRealmAdminClient;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public PublicRegisterResponse register(PublicRegisterRequest request) {
        if (!registrationProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Self-service registration is disabled");
        }
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String password = request.getPassword();

        var existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();
            if (!username.equals(existing.getUsername())) {
                throw new IllegalStateException("This email is already registered to a different username");
            }
            if (keycloakRealmAdminClient.findUserIdByExactUsername(username).isPresent()) {
                throw new IllegalStateException("Email already exists");
            }
            /*
             * DB row exists (from an earlier successful registration) but Keycloak user is gone —
             * typical after Keycloak container/volume reset while Postgres survived.
             * Recreate only the identity account; do not insert another User row.
             */
            String recoverId = null;
            try {
                recoverId = keycloakRealmAdminClient.createUser(username, email, password);
                keycloakRealmAdminClient.assignRealmRole(recoverId, REALM_USER_ROLE);
                return new PublicRegisterResponse(existing.getId(), existing.getUsername(), existing.getEmail());
            } catch (RuntimeException ex) {
                keycloakRealmAdminClient.deleteUserQuietly(recoverId);
                throw ex;
            }
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Username already exists");
        }

        String keycloakUserId = null;
        try {
            keycloakUserId = keycloakRealmAdminClient.createUser(username, email, password);
            keycloakRealmAdminClient.assignRealmRole(keycloakUserId, REALM_USER_ROLE);
            User user = userService.createUser(email, username);
            return new PublicRegisterResponse(user.getId(), user.getUsername(), user.getEmail());
        } catch (RuntimeException ex) {
            keycloakRealmAdminClient.deleteUserQuietly(keycloakUserId);
            throw ex;
        }
    }
}
