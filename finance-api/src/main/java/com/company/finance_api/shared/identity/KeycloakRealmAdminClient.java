package com.company.finance_api.shared.identity;

import com.company.finance_api.bootstrap.config.KeycloakAdminProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Keycloak Admin REST ile realm kullanıcı oluşturma, role atama ve profil yönetimi (master
 * admin-cli token).
 */
@Component
public class KeycloakRealmAdminClient {

  private static final String MASTER_TOKEN_PATH = "/realms/master/protocol/openid-connect/token";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final KeycloakAdminProperties props;

  public KeycloakRealmAdminClient(
      RestClient keycloakRestClient, ObjectMapper objectMapper, KeycloakAdminProperties props) {
    this.restClient = keycloakRestClient;
    this.objectMapper = objectMapper;
    this.props = props;
  }

  /** Master-realm admin-cli password grant ile admin access token alır. */
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
    String body =
        restClient
            .post()
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
   * Yeni realm kullanıcısı oluşturur.
   *
   * @return Keycloak user id
   */
  public String createUser(String username, String email, String password) {
    String token = obtainAdminAccessToken();
    Map<String, Object> payload = new HashMap<>();
    payload.put("username", username);
    payload.put("email", email);
    payload.put("enabled", true);
    // Keycloak 24+ declarative user profile: first/last name required for password grant ("Account
    // is not fully set up").
    payload.put("firstName", username);
    payload.put("lastName", "Member");
    payload.put("emailVerified", true);
    payload.put(
        "credentials", List.of(Map.of("type", "password", "value", password, "temporary", false)));

    try {
      var response =
          restClient
              .post()
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
        throw new IllegalStateException(
            "Username or email is already registered in identity provider");
      }
      throw new IllegalStateException(
          "Keycloak user creation failed: HTTP " + ex.getStatusCode().value());
    }
  }

  /** Realm role'ü kullanıcıya atar. */
  public void assignRealmRole(String keycloakUserId, String roleName) {
    String token = obtainAdminAccessToken();
    String roleId = findRealmRoleId(token, roleName);
    List<Map<String, String>> body = List.of(Map.of("id", roleId, "name", roleName));
    restClient
        .post()
        .uri(
            "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
            props.getRealm(),
            keycloakUserId)
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  /** Keycloak realm kullanıcısını siler. */
  public void deleteUser(String keycloakUserId) {
    String token = obtainAdminAccessToken();
    restClient
        .delete()
        .uri("/admin/realms/{realm}/users/{userId}", props.getRealm(), keycloakUserId)
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .toBodilessEntity();
  }

  private String findRealmRoleId(String adminToken, String roleName) {
    String json =
        restClient
            .get()
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

  /** DB provisioning başarısız olursa Keycloak kullanıcısını best-effort siler. */
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

  /** Portal satırı için Keycloak user id çözer (önce auth username, sonra e-posta araması). */
  public Optional<String> findUserIdForPortalUser(String authUsername, String email) {
    if (StringUtils.hasText(authUsername)) {
      Optional<String> byAuth =
          findUserIdByExactUsername(authUsername.trim().toLowerCase(java.util.Locale.ROOT));
      if (byAuth.isPresent()) {
        return byAuth;
      }
    }
    if (!StringUtils.hasText(email)) {
      return Optional.empty();
    }
    return findRealmUsernameByEmail(email.trim()).flatMap(this::findUserIdByExactUsername);
  }

  /** Yapılandırılmış realm'de tam username eşleşmesiyle Keycloak user id arar. */
  public Optional<String> findUserIdByExactUsername(String realmUsername) {
    String token = obtainAdminAccessToken();
    String body =
        restClient
            .get()
            .uri(
                "/admin/realms/{realm}/users?username={username}&exact=true",
                props.getRealm(),
                realmUsername)
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

  /** Tam e-posta eşleşmesiyle realm username döner (password grant için kullanılan username). */
  public Optional<String> findRealmUsernameByEmail(String email) {
    if (email == null || email.isBlank()) {
      return Optional.empty();
    }
    String token = obtainAdminAccessToken();
    String body =
        restClient
            .get()
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

  /** Kullanıcı parolasını sıfırlar. */
  public void resetUserPassword(String keycloakUserId, String newPassword, boolean temporary) {
    String token = obtainAdminAccessToken();
    Map<String, Object> payload =
        Map.of(
            "type", "password",
            "value", newPassword,
            "temporary", temporary);
    restClient
        .put()
        .uri(
            "/admin/realms/{realm}/users/{userId}/reset-password", props.getRealm(), keycloakUserId)
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .toBodilessEntity();
  }

  /** Kullanıcı e-posta adresini günceller. */
  public void updateUserEmail(String keycloakUserId, String newEmail) {
    String token = obtainAdminAccessToken();
    JsonNode root = fetchRealmUserJson(token, keycloakUserId);
    Map<String, Object> payload = buildSafeUserPutPayload(root);
    payload.put("email", newEmail);
    payload.put("emailVerified", true);
    putRealmUser(token, keycloakUserId, payload, "email");
  }

  /** Kullanıcı hesabını etkinleştirir veya devre dışı bırakır. */
  public void setUserEnabled(String keycloakUserId, boolean enabled) {
    String token = obtainAdminAccessToken();
    JsonNode root = fetchRealmUserJson(token, keycloakUserId);
    Map<String, Object> payload = buildSafeUserPutPayload(root);
    payload.put("enabled", enabled);
    putRealmUser(token, keycloakUserId, payload, "enabled");
  }

  /** Kullanıcının tüm aktif oturumlarını iptal eder (sonraki API çağrısında sign-out). */
  public void logoutAllSessions(String keycloakUserId) {
    String token = obtainAdminAccessToken();
    try {
      restClient
          .post()
          .uri("/admin/realms/{realm}/users/{userId}/logout", props.getRealm(), keycloakUserId)
          .headers(h -> h.setBearerAuth(token))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) {
        return;
      }
      throw new IllegalStateException("Could not revoke user sessions", ex);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not revoke user sessions", ex);
    }
  }

  /** Realm username ve firstName alanlarını günceller. */
  public void updateRealmUsername(String keycloakUserId, String newUsername) {
    String token = obtainAdminAccessToken();
    JsonNode root = fetchRealmUserJson(token, keycloakUserId);
    Map<String, Object> payload = buildSafeUserPutPayload(root);
    payload.put("username", newUsername);
    payload.put("firstName", newUsername);
    putRealmUser(token, keycloakUserId, payload, "username");
  }

  private JsonNode fetchRealmUserJson(String token, String keycloakUserId) {
    String json =
        restClient
            .get()
            .uri("/admin/realms/{realm}/users/{userId}", props.getRealm(), keycloakUserId)
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .body(String.class);
    try {
      JsonNode root = objectMapper.readTree(json);
      if (!root.isObject()) {
        throw new IllegalStateException("Unexpected Keycloak user JSON shape");
      }
      return root;
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to parse Keycloak user JSON", e);
    }
  }

  /**
   * Keycloak salt okunur alanları (örn. {@code id}) içeren tam GET→PUT round-trip'lerini reddeder —
   * HTTP 400.
   */
  private Map<String, Object> buildSafeUserPutPayload(JsonNode root) {
    Map<String, Object> payload = new HashMap<>();
    String username = root.path("username").asText("");
    payload.put("username", username);
    if (root.hasNonNull("email")) {
      payload.put("email", root.get("email").asText());
    }
    payload.put("enabled", root.path("enabled").asBoolean(true));
    payload.put("emailVerified", root.path("emailVerified").asBoolean(true));
    String firstName = root.path("firstName").asText("");
    payload.put("firstName", firstName.isBlank() ? username : firstName);
    String lastName = root.path("lastName").asText("");
    payload.put("lastName", lastName.isBlank() ? "Member" : lastName);
    return payload;
  }

  private void putRealmUser(
      String token, String keycloakUserId, Map<String, Object> payload, String fieldLabel) {
    try {
      restClient
          .put()
          .uri("/admin/realms/{realm}/users/{userId}", props.getRealm(), keycloakUserId)
          .headers(h -> h.setBearerAuth(token))
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException ex) {
      throw mapUserPutFailure(ex, fieldLabel);
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Keycloak " + fieldLabel + " update failed", e);
    }
  }

  private IllegalStateException mapUserPutFailure(
      RestClientResponseException ex, String fieldLabel) {
    int status = ex.getStatusCode().value();
    if (status == 409) {
      if ("username".equals(fieldLabel)) {
        return new IllegalStateException("Bu kullanıcı adı zaten kullanılıyor");
      }
      return new IllegalStateException("Bu e-posta adresi zaten kullanılıyor");
    }
    if (status == 400 && "username".equals(fieldLabel)) {
      if (keycloakErrorIndicatesDuplicate(ex)) {
        return new IllegalStateException("Bu kullanıcı adı zaten kullanılıyor");
      }
      return new IllegalStateException("Bu kullanıcı adı kullanılamıyor. Başka bir ad deneyin.");
    }
    String detail = parseKeycloakErrorMessage(ex);
    if (detail != null && !detail.isBlank()) {
      return new IllegalStateException("Güncelleme başarısız. Lütfen tekrar deneyin.");
    }
    return new IllegalStateException("Güncelleme başarısız. Lütfen tekrar deneyin.");
  }

  private static boolean keycloakErrorIndicatesDuplicate(RestClientResponseException ex) {
    String body = ex.getResponseBodyAsString();
    if (body == null) {
      return false;
    }
    String lower = body.toLowerCase();
    return lower.contains("exists")
        || lower.contains("duplicate")
        || lower.contains("already")
        || lower.contains("unique");
  }

  private String parseKeycloakErrorMessage(RestClientResponseException ex) {
    String body = ex.getResponseBodyAsString();
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      JsonNode n = objectMapper.readTree(body);
      String msg = n.path("errorMessage").asText(null);
      if (msg != null && !msg.isBlank()) {
        return msg.trim();
      }
      msg = n.path("error_description").asText(null);
      if (msg != null && !msg.isBlank()) {
        return msg.trim();
      }
      msg = n.path("error").asText(null);
      if (msg != null && !msg.isBlank()) {
        return msg.trim();
      }
    } catch (Exception ignored) {
      // fall through
    }
    return body.length() > 240 ? body.substring(0, 240) : body;
  }
}
