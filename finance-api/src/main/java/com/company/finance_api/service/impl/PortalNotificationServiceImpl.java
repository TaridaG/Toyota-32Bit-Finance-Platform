package com.company.finance_api.service.impl;

import com.company.finance_api.alarm.domain.AlarmHistory;
import com.company.finance_api.dto.PortalNotificationPageResponse;
import com.company.finance_api.dto.PortalNotificationResponse;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmHistoryRepository;
import com.company.finance_api.service.PortalNotificationService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PortalNotificationServiceImpl iş mantığını uygular (portal notification service). */
@Service
@RequiredArgsConstructor
public class PortalNotificationServiceImpl implements PortalNotificationService {

  private final AlarmHistoryRepository repository;
  private final CurrentUserResolver currentUserResolver;

  @Override
  @Transactional(readOnly = true)
  /** MyPage sorgusunu döner. */
  public PortalNotificationPageResponse getMyPage(int page, int size) {
    UUID userId = currentUserResolver.getCurrentUserId();
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 50);
    Pageable pageable = PageRequest.of(safePage, safeSize);
    Page<AlarmHistory> result =
        repository.findByUserIdAndDeletedFalseOrderByTriggeredAtDesc(userId, pageable);
    long unread = repository.countByUserIdAndDeletedFalseAndReadAtIsNull(userId);
    return new PortalNotificationPageResponse(
        result.getContent().stream().map(this::toResponse).toList(),
        safePage,
        safeSize,
        result.getTotalElements(),
        result.getTotalPages(),
        unread);
  }

  @Override
  @Transactional(readOnly = true)
  /** ById sorgusunu döner. */
  public PortalNotificationResponse getById(long id) {
    return toResponse(requireOwned(id));
  }

  @Override
  @Transactional
  /** markRead işlemini gerçekleştirir. */
  public PortalNotificationResponse markRead(long id) {
    AlarmHistory history = requireOwned(id);
    history.markRead();
    repository.save(history);
    return toResponse(history);
  }

  @Override
  @Transactional
  /** markAllRead işlemini gerçekleştirir. */
  public void markAllRead() {
    UUID userId = currentUserResolver.getCurrentUserId();
    repository.findByUserIdOrderByTriggeredAtDesc(userId).stream()
        .filter(h -> !h.isDeleted() && h.getReadAt() == null)
        .forEach(
            h -> {
              h.markRead();
              repository.save(h);
            });
  }

  @Override
  @Transactional
  /** delete işlemini uygular. */
  public void delete(long id) {
    AlarmHistory history = requireOwned(id);
    history.markDeleted();
    repository.save(history);
  }

  @Override
  @Transactional(readOnly = true)
  /** UnreadCount sorgusunu döner. */
  public long getUnreadCount() {
    UUID userId = currentUserResolver.getCurrentUserId();
    return repository.countByUserIdAndDeletedFalseAndReadAtIsNull(userId);
  }

  private AlarmHistory requireOwned(long id) {
    UUID userId = currentUserResolver.getCurrentUserId();
    return repository
        .findByIdAndUserIdAndDeletedFalse(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
  }

  private PortalNotificationResponse toResponse(AlarmHistory history) {
    String condition = history.getCondition() != null ? history.getCondition().name() : null;
    return new PortalNotificationResponse(
        history.getId(),
        history.getNotificationType().name(),
        history.getTitle(),
        history.getBody(),
        history.getInstrumentSymbol(),
        condition,
        history.getThreshold(),
        history.getPrice(),
        history.getTriggeredAt(),
        history.getReadAt() != null);
  }
}
