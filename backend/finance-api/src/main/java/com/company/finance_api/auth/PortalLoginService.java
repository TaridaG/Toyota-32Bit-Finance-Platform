package com.company.finance_api.auth;

import com.company.finance_api.config.KeycloakAdminProperties;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PublicLoginRequest;
import com.company.finance_api.dto.PublicLoginResponse;
import com.company.finance_api.dto.PublicRefreshRequest;
import com.company.finance_api.identity.KeycloakDirectGrantClient;
import com.company.finance_api.identity.KeycloakRealmAdminClient;
import com.company.finance_api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@Service
public class PortalLoginService {

    private final KeycloakDirectGrantClient keycloakDirectGrantClient;
    private final KeycloakAdminProperties keycloakAdminProperties;
    private final UserRepository userRepository;
    private final KeycloakRealmAdminClient keycloakRealmAdminClient;

    public PortalLoginService(
            KeycloakDirectGrantClient keycloakDirectGrantClient,
            KeycloakAdminProperties keycloakAdminProperties,
            UserRepository userRepository,
            KeycloakRealmAdminClient keycloakRealmAdminClient
    ) {
        this.keycloakDirectGrantClient = keycloakDirectGrantClient;
        this.keycloakAdminProperties = keycloakAdminProperties;
        this.userRepository = userRepository;
        this.keycloakRealmAdminClient = keycloakRealmAdminClient;
    }

    public PublicLoginResponse login(PublicLoginRequest request) {
        KeycloakAdminProperties.Portal portal = keycloakAdminProperties.getPortal();
        if (!StringUtils.hasText(portal.getClientSecret())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Sign-in is not configured");
        }
        // Match PortalRegistrationService: Keycloak stores email/username lowercased (ROOT).
        String identity = request.getUsername().trim().toLowerCase(Locale.ROOT);
        Optional<User> portalUser = resolvePortalUser(identity);
        if (portalUser.isPresent() && !portalUser.get().isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is pending deletion");
        }
        String keycloakUsername = resolveKeycloakUsername(identity, portalUser);

        try {
            return keycloakDirectGrantClient.exchangePassword(keycloakUsername, request.getPassword());
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() != HttpStatus.UNAUTHORIZED.value()) {
                throw ex;
            }
            if (portalUser.isPresent()) {
                String canonical = portalUser.get().getUsername().toLowerCase(Locale.ROOT);
                if (keycloakRealmAdminClient.findUserIdByExactUsername(canonical).isEmpty()) {
                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Portal kaydın var ama kimlik sunucusunda (Keycloak) bu kullanıcı yok — "
                                    + "genelde Keycloak sıfırlandığında olur. Kayıt ol sayfasına git; "
                                    + "aynı e-posta ve kullanıcı adıyla tekrar kayıt ol, parolanı yeniden belirle."
                    );
                }
            }
            throw ex;
        }
    }

    /**
     * Match by email first, then by username (case-insensitive). Same order for every request so
     * both "me@x.com" and "myname" resolve without special-casing {@code @}.
     */
    private Optional<User> resolvePortalUser(String normalizedIdentity) {
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(normalizedIdentity);
        if (byEmail.isPresent()) {
            return byEmail;
        }
        return userRepository.findByUsernameIgnoreCase(normalizedIdentity);
    }

    private String resolveKeycloakUsername(String normalizedIdentity, Optional<User> portalUser) {
        if (portalUser.isPresent()) {
            return portalUser.get().getUsername().toLowerCase(Locale.ROOT);
        }
        if (normalizedIdentity.indexOf('@') >= 0) {
            try {
                return keycloakRealmAdminClient
                        .findRealmUsernameByEmail(normalizedIdentity)
                        .map(u -> u.toLowerCase(Locale.ROOT))
                        .orElse(normalizedIdentity);
            } catch (RuntimeException ignored) {
                return normalizedIdentity;
            }
        }
        return normalizedIdentity;
    }

    public PublicLoginResponse refresh(PublicRefreshRequest request) {
        KeycloakAdminProperties.Portal portal = keycloakAdminProperties.getPortal();
        if (!StringUtils.hasText(portal.getClientSecret())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Sign-in is not configured");
        }
        String submitted = request.getRefreshToken().trim();
        PublicLoginResponse resp = keycloakDirectGrantClient.exchangeRefreshToken(submitted);
        if (resp.refreshToken() == null && StringUtils.hasText(submitted)) {
            return new PublicLoginResponse(
                    resp.accessToken(),
                    resp.expiresIn(),
                    resp.tokenType(),
                    submitted,
                    resp.refreshExpiresIn()
            );
        }
        return resp;
    }
}
