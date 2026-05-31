package com.company.finance_api.ai.application;

import com.company.finance_api.ai.domain.AiInteractionLogEntity;
import com.company.finance_api.ai.infrastructure.persistence.AiInteractionLogRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** AI etkileşimlerini audit ve kota takibi için veritabanına loglar. */
@Service
public class AiUsageLogService {

  private final AiInteractionLogRepository repository;

  public AiUsageLogService(AiInteractionLogRepository repository) {
    this.repository = repository;
  }

  /** Başarılı AI çağrısını audit tablosuna yazar. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logSuccess(
      String taskType,
      String language,
      String sourceLanguage,
      String targetLanguage,
      String model,
      String createdBy,
      String inputFingerprint,
      int responseChars) {
    AiInteractionLogEntity log = AiInteractionLogEntity.create();
    log.setTaskType(taskType);
    log.setLanguage(language);
    log.setSourceLanguage(sourceLanguage);
    log.setTargetLanguage(targetLanguage);
    log.setModel(model);
    log.setStatus("SUCCESS");
    log.setCreatedBy(createdBy);
    log.setInputHash(hashInput(inputFingerprint));
    log.setResponseChars(responseChars);
    repository.save(log);
  }

  /** Başarısız AI çağrısını hata mesajıyla audit tablosuna yazar. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logFailure(
      String taskType,
      String language,
      String sourceLanguage,
      String targetLanguage,
      String model,
      String createdBy,
      String inputFingerprint,
      String errorMessage) {
    AiInteractionLogEntity log = AiInteractionLogEntity.create();
    log.setTaskType(taskType);
    log.setLanguage(language);
    log.setSourceLanguage(sourceLanguage);
    log.setTargetLanguage(targetLanguage);
    log.setModel(model);
    log.setStatus("FAILED");
    log.setCreatedBy(createdBy);
    log.setInputHash(hashInput(inputFingerprint));
    log.setErrorMessage(sanitizeError(errorMessage));
    repository.save(log);
  }

  static String hashInput(String input) {
    if (input == null || input.isBlank()) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException ex) {
      return null;
    }
  }

  private static String sanitizeError(String message) {
    if (message == null) {
      return null;
    }
    String trimmed = message.trim();
    return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
  }
}
