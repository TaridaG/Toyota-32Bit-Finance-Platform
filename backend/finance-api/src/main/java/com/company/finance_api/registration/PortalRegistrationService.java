package com.company.finance_api.registration;

import com.company.finance_api.config.RegistrationProperties;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PublicRegisterRequest;
import com.company.finance_api.dto.PublicRegisterResponse;
import com.company.finance_api.dto.PublicUsernameAvailabilityResponse;
import com.company.finance_api.identity.KeycloakRealmAdminClient;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PortalRegistrationService {

    private static final String REALM_USER_ROLE = "USER";

    private final RegistrationProperties registrationProperties;
    private final KeycloakRealmAdminClient keycloakRealmAdminClient;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RegistrationEmailVerificationService registrationEmailVerificationService;

    public PortalRegistrationService(
            RegistrationProperties registrationProperties,
            KeycloakRealmAdminClient keycloakRealmAdminClient,
            UserRepository userRepository,
            UserService userService,
            RegistrationEmailVerificationService registrationEmailVerificationService
    ) {
        this.registrationProperties = registrationProperties;
        this.keycloakRealmAdminClient = keycloakRealmAdminClient;
        this.userRepository = userRepository;
        this.userService = userService;
        this.registrationEmailVerificationService = registrationEmailVerificationService;
    }

    public PublicRegisterResponse register(PublicRegisterRequest request) {
        if (!registrationProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Self-service registration is disabled");
        }
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String password = request.getPassword();
        registrationEmailVerificationService.verifyCodeOrThrow(email, request.getVerificationCode());

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

    public PublicUsernameAvailabilityResponse checkUsernameAvailability(String usernameInput) {
        String normalized = normalizeUsernameCandidate(usernameInput);
        boolean available = isUsernameAvailable(normalized);
        if (available) {
            return new PublicUsernameAvailabilityResponse(normalized, true, List.of());
        }
        return new PublicUsernameAvailabilityResponse(normalized, false, suggestAvailableUsernames(normalized, 3));
    }

    private boolean isUsernameAvailable(String normalizedUsername) {
        if (userRepository.findByUsernameIgnoreCase(normalizedUsername).isPresent()) {
            return false;
        }
        return keycloakRealmAdminClient.findUserIdByExactUsername(normalizedUsername).isEmpty();
    }

    private List<String> suggestAvailableUsernames(String normalizedBase, int limit) {
        Set<String> candidatePool = new LinkedHashSet<>();
        String base = normalizedBase.length() < 3 ? normalizedBase + "user" : normalizedBase;
        candidatePool.add(base + "_1");
        candidatePool.add(base + "_01");
        candidatePool.add(base + ".trader");
        for (int i = 2; i <= 99 && candidatePool.size() < 64; i++) {
            candidatePool.add(base + "_" + i);
            candidatePool.add(base + i);
            candidatePool.add(base + "." + i);
        }
        List<String> out = new ArrayList<>();
        for (String candidate : candidatePool) {
            if (!isUsernameAvailable(candidate)) {
                continue;
            }
            out.add(candidate);
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private String normalizeUsernameCandidate(String raw) {
        String base = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        base = base.replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) {
            return "user";
        }
        return base;
    }
}
