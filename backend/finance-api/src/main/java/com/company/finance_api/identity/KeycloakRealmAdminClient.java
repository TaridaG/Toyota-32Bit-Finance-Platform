package com.company.finance_api.identity;

import com.company.finance_api.config.KeycloakAdminProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Creates realm users and assigns {@code USER} via Keycloak Admin REST (master admin-cli token).
 */
@Component
public class KeycloakRealmAdminClient {

    private static final String MASTER_TOKEN_PATH = "/realms/master/protocol/openid-connect/token";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KeycloakAdminProperties props;

    public KeycloakRealmAdminClient(RestClient keycloakRestClient, ObjectMapper objectMapper, KeycloakAdminProperties props) {
        this.restClient = keycloakRestClient;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    public String obtainAdminAccessToken() {
        KeycloakAdminProperties.Admin admin = props.getAdmin();
        if (admin.getPassword() == null || admin.getPassword().isBlank()) {
            throw new IllegalStateException("Keycloak admin password is not configured");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", admin.getClientId());
        form.add("username", admin.getUsername());
        form.add("password", admin.getPassword());
        String body = restClient.post()
                .uri(MASTER_TOKEN_PATH)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.hasNonNull("access_token")) {
                throw new IllegalStateException("Keycloak token response missing access_token");
            }
            return root.get("access_token").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Keycloak token response", e);
        }
    }

    /**
     * @return Keycloak user id
     */
    public String createUser(String username, String email, String password) {
        String token = obtainAdminAccessToken();
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("enabled", true);
        // Keycloak 24+ declarative user profile: first/last name required for password grant ("Account is not fully set up").
        payload.put("firstName", username);
        payload.put("lastName", "Member");
        payload.put("emailVerified", true);
        payload.put("credentials", List.of(Map.of("type", "password", "value", password, "temporary", false)));

        try {
            var response = restClient.post()
                    .uri("/admin/realms/{realm}/users", props.getRealm())
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            if (location == null || location.isBlank()) {
                throw new IllegalStateException("Keycloak did not return Location header for new user");
            }
            String id = location.substring(location.lastIndexOf('/') + 1);
            if (id.isBlank()) {
                throw new IllegalStateException("Could not parse Keycloak user id from Location");
            }
            return id;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new IllegalStateException("Username or email is already registered in identity provider");
            }
            throw new IllegalStateException("Keycloak user creation failed: HTTP " + ex.getStatusCode().value());
        }
    }

    public void assignRealmRole(String keycloakUserId, String roleName) {
        String token = obtainAdminAccessToken();
        String roleId = findRealmRoleId(token, roleName);
        List<Map<String, String>> body = List.of(Map.of("id", roleId, "name", roleName));
        restClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", props.getRealm(), keycloakUserId)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteUser(String keycloakUserId) {
        String token = obtainAdminAccessToken();
        restClient.delete()
                .uri("/admin/realms/{realm}/users/{userId}", props.getRealm(), keycloakUserId)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .toBodilessEntity();
    }

    private String findRealmRoleId(String adminToken, String roleName) {
        String json = restClient.get()
                .uri("/admin/realms/{realm}/roles", props.getRealm())
                .headers(h -> h.setBearerAuth(adminToken))
                .retrieve()
                .body(String.class);
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (!arr.isArray()) {
                throw new IllegalStateException("Unexpected roles response from Keycloak");
            }
            for (JsonNode n : arr) {
                if (roleName.equals(n.path("name").asText())) {
                    return n.path("id").asText();
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Keycloak roles", e);
        }
        throw new IllegalStateException("Realm role not found in Keycloak: " + roleName);
    }

    /** Best-effort cleanup if DB provisioning fails after Keycloak user was created. */
    public void deleteUserQuietly(String keycloakUserId) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            return;
        }
        try {
            deleteUser(keycloakUserId);
        } catch (RuntimeException ignored) {
            // compensate only
        }
    }

    /**
     * Keycloak admin API: exact username match within the configured realm.
     */
    public Optional<String> findUserIdByExactUsername(String realmUsername) {
        String token = obtainAdminAccessToken();
        String body = restClient.get()
                .uri("/admin/realms/{realm}/users?username={username}&exact=true", props.getRealm(), realmUsername)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .body(String.class);
        try {
            JsonNode arr = objectMapper.readTree(body);
            if (!arr.isArray() || arr.isEmpty()) {
                return Optional.empty();
            }
            String id = arr.get(0).path("id").asText(null);
            return Optional.ofNullable(id).filter(s -> !s.isBlank());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Keycloak user search response", e);
        }
    }

    /**
     * Keycloak admin API: exact email match; returns the realm username used for password grant.
     */
    public Optional<String> findRealmUsernameByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String token = obtainAdminAccessToken();
        String body = restClient.get()
                .uri("/admin/realms/{realm}/users?email={email}&exact=true", props.getRealm(), email)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .body(String.class);
        try {
            JsonNode arr = objectMapper.readTree(body);
            if (!arr.isArray() || arr.isEmpty()) {
                return Optional.empty();
            }
            String username = arr.get(0).path("username").asText(null);
            return Optional.ofNullable(username).filter(s -> !s.isBlank());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Keycloak user search by email", e);
        }
    }

    public void resetUserPassword(String keycloakUserId, String newPassword, boolean temporary) {
        String token = obtainAdminAccessToken();
        Map<String, Object> payload = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", temporary
        );
        restClient.put()
                .uri("/admin/realms/{realm}/users/{userId}/reset-password", props.getRealm(), keycloakUserId)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    public void updateUserEmail(String keycloakUserId, String newEmail) {
        String token = obtainAdminAccessToken();
        String json = restClient.get()
                .uri("/admin/realms/{realm}/users/{userId}", props.getRealm(), keycloakUserId)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .body(String.class);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new IllegalStateException("Unexpected Keycloak user JSON shape");
            }
            ObjectNode objectNode = (ObjectNode) root;
            objectNode.put("email", newEmail);
            objectNode.put("emailVerified", true);
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{userId}", props.getRealm(), keycloakUserId)
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectNode.toString())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new IllegalStateException("Email is already registered in identity provider");
            }
            throw new IllegalStateException("Keycloak email update failed: HTTP " + ex.getStatusCode().value());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Keycloak email update failed", e);
        }
    }

    public void updateRealmUsername(String keycloakUserId, String newUsername) {
        String token = obtainAdminAccessToken();
        String json = restClient.get()
                .uri("/admin/realms/{realm}/users/{userId}", props.getRealm(), keycloakUserId)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .body(String.class);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new IllegalStateException("Unexpected Keycloak user JSON shape");
            }
            ObjectNode objectNode = (ObjectNode) root;
            objectNode.put("username", newUsername);
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{userId}", props.getRealm(), keycloakUserId)
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectNode.toString())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new IllegalStateException("Username is already taken in identity provider");
            }
            throw new IllegalStateException("Keycloak user update failed: HTTP " + ex.getStatusCode().value());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Keycloak user update failed", e);
        }
    }
}
