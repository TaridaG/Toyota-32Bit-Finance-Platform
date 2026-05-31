package com.company.finance_api.registration.application;

import com.company.finance_api.auth.infrastructure.http.dto.PublicEmailAvailabilityResponse;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.registration.domain.EmailAvailabilityException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kayıt ve profil için e-posta müsaitliği, engel listesi ve alternatif öneriler. */
@Service
public class RegistrationEmailAvailabilityService {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

  private final UserRepository userRepository;
  private final BlockedRegistrationEmailService blockedRegistrationEmailService;

  public RegistrationEmailAvailabilityService(
      UserRepository userRepository,
      BlockedRegistrationEmailService blockedRegistrationEmailService) {
    this.userRepository = userRepository;
    this.blockedRegistrationEmailService = blockedRegistrationEmailService;
  }

  /** E-postanın kullanılabilir, engelli veya dolu olduğunu döner. */
  @Transactional(readOnly = true)
  public PublicEmailAvailabilityResponse check(String rawEmail, UUID excludeUserId) {
    String normalized = normalizeEmail(rawEmail);
    if (!isWellFormed(normalized)) {
      return new PublicEmailAvailabilityResponse(normalized, false, false, List.of());
    }
    if (blockedRegistrationEmailService.isBlocked(normalized)) {
      return new PublicEmailAvailabilityResponse(normalized, false, true, List.of());
    }
    if (isTakenByAnotherUser(normalized, excludeUserId)) {
      return new PublicEmailAvailabilityResponse(
          normalized, false, false, suggestAvailableEmails(normalized, 3, excludeUserId));
    }
    return new PublicEmailAvailabilityResponse(normalized, true, false, List.of());
  }

  /** Kayıt için e-postanın müsait olduğunu doğrular; değilse exception fırlatır. */
  @Transactional(readOnly = true)
  public void assertAvailableForRegistration(String rawEmail) {
    assertAvailable(rawEmail, null);
  }

  /** Profil e-posta güncellemesi için müsaitliği doğrular. */
  @Transactional(readOnly = true)
  public void assertAvailableForProfileChange(String rawEmail, UUID currentUserId) {
    assertAvailable(rawEmail, currentUserId);
  }

  private void assertAvailable(String rawEmail, UUID excludeUserId) {
    PublicEmailAvailabilityResponse result = check(rawEmail, excludeUserId);
    if (result.blocked()) {
      throw EmailAvailabilityException.blocked(result.normalizedEmail());
    }
    if (!result.available()) {
      throw EmailAvailabilityException.inUse(result.suggestions());
    }
  }

  private boolean isTakenByAnotherUser(String normalizedEmail, UUID excludeUserId) {
    return userRepository
        .findByEmailIgnoreCase(normalizedEmail)
        .map(existing -> excludeUserId == null || !existing.getId().equals(excludeUserId))
        .orElse(false);
  }

  private List<String> suggestAvailableEmails(
      String normalizedEmail, int limit, UUID excludeUserId) {
    int at = normalizedEmail.indexOf('@');
    if (at < 1) {
      return List.of();
    }
    String local = normalizedEmail.substring(0, at);
    String domain = normalizedEmail.substring(at + 1);
    if (domain.isBlank()) {
      return List.of();
    }

    Set<String> candidatePool = new LinkedHashSet<>();
    candidatePool.add(local + "+1@" + domain);
    candidatePool.add(local + ".01@" + domain);
    candidatePool.add(local + "01@" + domain);
    candidatePool.add(local + "_2@" + domain);
    for (int i = 2; i <= 99 && candidatePool.size() < 64; i++) {
      candidatePool.add(local + "+" + i + "@" + domain);
      candidatePool.add(local + "." + i + "@" + domain);
      candidatePool.add(local + i + "@" + domain);
    }

    List<String> out = new ArrayList<>();
    for (String candidate : candidatePool) {
      if (!isWellFormed(candidate)) {
        continue;
      }
      if (blockedRegistrationEmailService.isBlocked(candidate)) {
        continue;
      }
      if (isTakenByAnotherUser(candidate, excludeUserId)) {
        continue;
      }
      out.add(candidate);
      if (out.size() >= limit) {
        break;
      }
    }
    return out;
  }

  private static String normalizeEmail(String raw) {
    return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
  }

  private static boolean isWellFormed(String email) {
    return !email.isBlank() && EMAIL_PATTERN.matcher(email).matches();
  }
}
