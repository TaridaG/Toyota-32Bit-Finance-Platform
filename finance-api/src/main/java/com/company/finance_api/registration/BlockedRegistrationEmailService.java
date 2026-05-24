package com.company.finance_api.registration;

import com.company.finance_api.domain.BlockedRegistrationEmail;
import com.company.finance_api.repository.BlockedRegistrationEmailRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin silinen hesaplardan sonra engellenen kayıt e-posta adresleri. */
@Service
public class BlockedRegistrationEmailService {

  private final BlockedRegistrationEmailRepository repository;

  public BlockedRegistrationEmailService(BlockedRegistrationEmailRepository repository) {
    this.repository = repository;
  }

  /** E-posta engelli değilse sessizce döner; engelliyse exception fırlatır. */
  @Transactional(readOnly = true)
  public void ensureNotBlocked(String email) {
    if (!isBlocked(email)) {
      return;
    }
    throw EmailAvailabilityException.blocked(email);
  }

  /** E-postanın kayıt engel listesinde olup olmadığını kontrol eder. */
  @Transactional(readOnly = true)
  public boolean isBlocked(String email) {
    if (email == null || email.isBlank()) {
      return false;
    }
    return repository.existsByEmail(normalize(email));
  }

  /** Silinen kullanıcının e-postasını yeni kayıtlara kapatır. */
  @Transactional
  public void blockFromDeletedUser(String email, UUID sourceUserId) {
    if (email == null || email.isBlank()) {
      return;
    }
    String normalized = normalize(email);
    if (repository.existsByEmail(normalized)) {
      return;
    }
    repository.save(new BlockedRegistrationEmail(normalized, Instant.now(), sourceUserId));
  }

  private static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
