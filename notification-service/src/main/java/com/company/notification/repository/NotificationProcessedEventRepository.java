package com.company.notification.repository;

import com.company.notification.domain.NotificationProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationProcessedEventRepository extends JpaRepository<NotificationProcessedEvent, UUID> {
}
