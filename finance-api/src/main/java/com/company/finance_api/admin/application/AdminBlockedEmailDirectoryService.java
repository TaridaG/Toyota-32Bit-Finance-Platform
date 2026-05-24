package com.company.finance_api.admin.application;

import com.company.finance_api.admin.infrastructure.http.dto.AdminBlockedEmailRowDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminBlockedEmailsPageDto;
import com.company.finance_api.auth.LoginSecurityNotificationService;
import com.company.finance_api.domain.BlockedRegistrationEmail;
import com.company.finance_api.domain.User;
import com.company.finance_api.repository.BlockedRegistrationEmailRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Admin panelinde kayıt engelli e-posta listesini sayfalar ve engeli kaldırır. */
@Service
public class AdminBlockedEmailDirectoryService {

  private static final int MAX_PAGE_SIZE = 50;

  private final BlockedRegistrationEmailRepository blockedRegistrationEmailRepository;
  private final UserRepository userRepository;
  private final LoginSecurityNotificationService loginSecurityNotificationService;

  public AdminBlockedEmailDirectoryService(
      BlockedRegistrationEmailRepository blockedRegistrationEmailRepository,
      UserRepository userRepository,
      LoginSecurityNotificationService loginSecurityNotificationService) {
    this.blockedRegistrationEmailRepository = blockedRegistrationEmailRepository;
    this.userRepository = userRepository;
    this.loginSecurityNotificationService = loginSecurityNotificationService;
  }

  /** Engelli e-postaları en yeniden eskiye sayfalı döner. */
  @Transactional(readOnly = true)
  public AdminBlockedEmailsPageDto list(int page, int size) {
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    int safePage = Math.max(page, 0);
    Page<BlockedRegistrationEmail> p =
        blockedRegistrationEmailRepository.findAllByOrderByBlockedAtDesc(
            PageRequest.of(safePage, safeSize));
    return new AdminBlockedEmailsPageDto(
        p.getContent().stream()
            .map(
                row ->
                    new AdminBlockedEmailRowDto(
                        row.getId(), row.getEmail(), row.getBlockedAt(), row.getSourceUserId()))
            .toList(),
        p.getTotalElements(),
        p.getTotalPages(),
        p.getNumber(),
        p.getSize());
  }

  /** Engeli kaldırır ve kullanıcıya bildirim gönderir. */
  @Transactional
  public void unblock(long id) {
    BlockedRegistrationEmail row =
        blockedRegistrationEmailRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Blocked email not found"));
    String email = row.getEmail();
    String locale = resolveLocale(row.getSourceUserId());
    blockedRegistrationEmailRepository.delete(row);
    loginSecurityNotificationService.notifyRegistrationEmailUnblocked(email, locale);
  }

  private String resolveLocale(UUID sourceUserId) {
    if (sourceUserId == null) {
      return "tr";
    }
    return userRepository
        .findById(sourceUserId)
        .map(User::getPreferredLocale)
        .filter(StringUtils::hasText)
        .orElse("tr");
  }
}
