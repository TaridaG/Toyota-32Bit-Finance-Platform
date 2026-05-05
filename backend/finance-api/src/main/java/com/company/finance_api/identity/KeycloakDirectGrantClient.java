package com.company.finance_api.identity;

import com.company.finance_api.config.KeycloakAdminProperties;
import com.company.finance_api.dto.PublicLoginResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resource Owner Password Credentials against Keycloak, invoked only from server-side code.
 */
@Component
public class KeycloakDirectGrantClient {

    private static final String TOKEN_PATH = "/realms/{realm}/protocol/openid-connect/token";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KeycloakAdminProperties props;

    public KeycloakDirectGrantClient(RestClient keycloakRestClient, ObjectMapper objectMapper, KeycloakAdminProperties props) {
        this.restClient = keycloakRestClient;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    public PublicLoginResponse exchangePassword(String username, String password) {
        KeycloakAdminProperties.Portal portal = props.getPortal();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", portal.getClientId());
        form.add("client_secret", portal.getClientSecret());
        form.add("username", username);
        form.add("password", password);
        form.add("scope", "openid profile email");
        try {
            String body = restClient.post()
                    .uri(TOKEN_PATH, props.getRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            if (root.hasNonNull("error")) {
                throw unauthorizedFromOAuthBody(body);
            }
            return mapSuccessfulTokenResponse(root);
        } catch (RestClientResponseException ex) {
            throw mapTokenError(ex);
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity provider unreachable", ex);
        } catch (Exception ex) {
            if (ex instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sign-in service error", ex);
        }
    }

    public PublicLoginResponse exchangeRefreshToken(String refreshToken) {
        KeycloakAdminProperties.Portal portal = props.getPortal();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", portal.getClientId());
        form.add("client_secret", portal.getClientSecret());
        form.add("refresh_token", refreshToken);
        try {
            String body = restClient.post()
                    .uri(TOKEN_PATH, props.getRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            if (root.hasNonNull("error")) {
                throw sessionExpired();
            }
            return mapSuccessfulTokenResponse(root);
        } catch (RestClientResponseException ex) {
            throw mapRefreshTokenError(ex);
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity provider unreachable", ex);
        } catch (Exception ex) {
            if (ex instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sign-in service error", ex);
        }
    }

    private PublicLoginResponse mapSuccessfulTokenResponse(JsonNode root) {
        String access = root.path("access_token").asText(null);
        if (access == null || access.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Token response incomplete");
        }
        long expiresIn = root.path("expires_in").asLong(300L);
        String tokenType = root.path("token_type").asText("Bearer");
        String refresh = root.path("refresh_token").asText(null);
        String refreshToken = (refresh != null && !refresh.isBlank()) ? refresh : null;
        Long refreshExpiresIn = root.hasNonNull("refresh_expires_in")
                ? root.get("refresh_expires_in").longValue()
                : null;
        return new PublicLoginResponse(access, expiresIn, tokenType, refreshToken, refreshExpiresIn);
    }

    private static ResponseStatusException sessionExpired() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired. Please sign in again.");
    }

    /**
     * Surfaces Keycloak {@code error_description} (e.g. "Invalid user credentials") when present.
     */
    private ResponseStatusException unauthorizedFromOAuthBody(String oauthBody) {
        String detail = parseKeycloakErrorDescription(oauthBody);
        String reason = StringUtils.hasText(detail) ? detail : "E-posta veya parola hatalı";
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }

    private String parseKeycloakErrorDescription(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode n = objectMapper.readTree(body);
            String desc = n.path("error_description").asText(null);
            if (StringUtils.hasText(desc)) {
                return desc.trim();
            }
            String err = n.path("error").asText(null);
            if (StringUtils.hasText(err)) {
                return err.trim();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private static ResponseStatusException mapRefreshTokenError(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String body = ex.getResponseBodyAsString();
        boolean invalidGrant = body != null && body.contains("invalid_grant");
        if (status == HttpStatus.UNAUTHORIZED.value() || (status == HttpStatus.BAD_REQUEST.value() && invalidGrant)) {
            return sessionExpired();
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sign-in service temporarily unavailable");
    }

    private ResponseStatusException mapTokenError(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String body = ex.getResponseBodyAsString();
        boolean invalidGrant = body != null && body.contains("invalid_grant");
        if (status == HttpStatus.UNAUTHORIZED.value() || (status == HttpStatus.BAD_REQUEST.value() && invalidGrant)) {
            return unauthorizedFromOAuthBody(body);
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sign-in service temporarily unavailable");
    }
}
