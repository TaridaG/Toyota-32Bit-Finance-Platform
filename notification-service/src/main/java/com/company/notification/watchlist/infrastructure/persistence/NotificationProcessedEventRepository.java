package com.company.notification.watchlist.infrastructure.persistence;

import com.company.notification.watchlist.domain.NotificationProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Idempotent Kafka consumption için işlenmiş watchlist event id'lerini saklar.
 */
public interface NotificationProcessedEventRepository extends JpaRepository<NotificationProcessedEvent, UUID> {
}
