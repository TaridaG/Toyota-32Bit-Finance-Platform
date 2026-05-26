package com.company.finance_api.registration;

import com.company.finance_api.bootstrap.config.RegistrationProperties;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PublicEmailAvailabilityResponse;
import com.company.finance_api.dto.PublicRegisterRequest;
import com.company.finance_api.dto.PublicRegisterResponse;
import com.company.finance_api.dto.PublicUsernameAvailabilityResponse;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.profile.application.UserService;
import com.company.finance_api.shared.identity.KeycloakRealmAdminClient;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Self-service kayıt: e-posta doğrulama, Keycloak user oluşturma ve müsaitlik kontrolleri. */
@Service
public class PortalRegistrationService {

  private static final String REALM_USER_ROLE = "USER";

  private final RegistrationProperties registrationProperties;
  private final KeycloakRealmAdminClient keycloakRealmAdminClient;
  private final UserRepository userRepository;
  private final UserService userService;
  private final RegistrationEmailVerificationService registrationEmailVerificationService;
  private final RegistrationEmailAvailabilityService registrationEmailAvailabilityService;

  public PortalRegistrationService(
      RegistrationProperties registrationProperties,
      KeycloakRealmAdminClient keycloakRealmAdminClient,
      UserRepository userRepository,
      UserService userService,
      RegistrationEmailVerificationService registrationEmailVerificationService,
      RegistrationEmailAvailabilityService registrationEmailAvailabilityService) {
    this.registrationProperties = registrationProperties;
    this.keycloakRealmAdminClient = keycloakRealmAdminClient;
    this.userRepository = userRepository;
    this.userService = userService;
    this.registrationEmailVerificationService = registrationEmailVerificationService;
    this.registrationEmailAvailabilityService = registrationEmailAvailabilityService;
  }

  /** Doğrulanmış e-posta ile yeni portal hesabı oluşturur veya Keycloak kaydını onarır. */
  public PublicRegisterResponse register(PublicRegisterRequest request) {
    if (!registrationProperties.isEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Self-service registration is disabled");
    }
    String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
    String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
    String password = request.getPassword();
    registrationEmailAvailabilityService.assertAvailableForRegistration(email);
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
        return new PublicRegisterResponse(
            existing.getId(), existing.getUsername(), existing.getEmail());
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

  /** Kayıt için e-posta adresinin müsait olup olmadığını kontrol eder. */
  public PublicEmailAvailabilityResponse checkEmailAvailability(String emailInput) {
    return registrationEmailAvailabilityService.check(emailInput, null);
  }

  /** Profil e-posta değişikliği için müsaitlik kontrolü (mevcut kullanıcı hariç). */
  public PublicEmailAvailabilityResponse checkEmailAvailabilityForUser(
      String emailInput, UUID excludeUserId) {
    return registrationEmailAvailabilityService.check(emailInput, excludeUserId);
  }

  /** Kayıt için kullanıcı adı müsaitliğini ve alternatif önerileri döner. */
  public PublicUsernameAvailabilityResponse checkUsernameAvailability(String usernameInput) {
    String normalized = normalizeUsernameCandidate(usernameInput);
    boolean available = isUsernameAvailable(normalized);
    if (available) {
      return new PublicUsernameAvailabilityResponse(normalized, true, List.of());
    }
    return new PublicUsernameAvailabilityResponse(
        normalized, false, suggestAvailableUsernames(normalized, 3));
  }

  /** Portal görünen kullanıcı adı müsaitliği (yalnızca DB); Keycloak login username değişmez. */
  public PublicUsernameAvailabilityResponse checkPortalDisplayUsernameAvailability(
      String usernameInput, UUID excludeUserId) {
    String normalized = normalizeUsernameCandidate(usernameInput);
    boolean available = isPortalDisplayUsernameAvailable(normalized, excludeUserId);
    if (available) {
      return new PublicUsernameAvailabilityResponse(normalized, true, List.of());
    }
    return new PublicUsernameAvailabilityResponse(
        normalized, false, suggestAvailablePortalDisplayUsernames(normalized, 3, excludeUserId));
  }

  private boolean isUsernameAvailable(String normalizedUsername) {
    if (userRepository.findByUsernameIgnoreCase(normalizedUsername).isPresent()) {
      return false;
    }
    return keycloakRealmAdminClient.findUserIdByExactUsername(normalizedUsername).isEmpty();
  }

  private boolean isPortalDisplayUsernameAvailable(String normalizedUsername, UUID excludeUserId) {
    return userRepository
        .findByUsernameIgnoreCase(normalizedUsername)
        .map(existing -> existing.getId().equals(excludeUserId))
        .orElse(true);
  }

  private List<String> suggestAvailablePortalDisplayUsernames(
      String normalizedBase, int limit, UUID excludeUserId) {
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
      if (!isPortalDisplayUsernameAvailable(candidate, excludeUserId)) {
        continue;
      }
      out.add(candidate);
      if (out.size() >= limit) {
        break;
      }
    }
    return out;
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
