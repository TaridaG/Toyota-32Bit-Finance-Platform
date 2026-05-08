package com.company.finance_api.scheduler;

import com.company.finance_api.domain.OutboxEvent;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.OutboxStatus;
import com.company.finance_api.event.UserDeletionRequestedEvent;
import com.company.finance_api.event.kafka.KafkaTopics;
import com.company.finance_api.identity.KeycloakRealmAdminClient;
import com.company.finance_api.profile.avatar.ProfileAvatarStorage;
import com.company.finance_api.registration.EmailVerificationCodeRepository;
import com.company.finance_api.repository.OutboxEventRepository;
import com.company.finance_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionSagaScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final UserRepository userRepository;
    private final KeycloakRealmAdminClient keycloakRealmAdminClient;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final ProfileAvatarStorage profileAvatarStorage;
    private final ObjectMapper objectMapper;

    private final int batchSize = 20;
    private final int maxAttempts = 12;
    private final long backoffSeconds = 10;

    @Scheduled(fixedDelayString = "${saga.user-delete.delay-ms:3000}")
    @Transactional
    public void process() {
        List<OutboxEvent> batch = outboxEventRepository.lockBatchByTopic(
                OutboxStatus.NEW,
                OutboxStatus.RETRY,
                Instant.now(),
                KafkaTopics.INTERNAL_USER_DELETE_REQUESTED,
                PageRequest.of(0, batchSize)
        );
        if (batch.isEmpty()) {
            return;
        }
        for (OutboxEvent event : batch) {
            try {
                UserDeletionRequestedEvent payload = objectMapper.readValue(event.getPayloadJson(), UserDeletionRequestedEvent.class);
                processSingle(payload);
                event.markSent();
                log.info("USER_DELETE_SAGA_SUCCESS outboxId={} userId={}", event.getId(), payload.userId());
            } catch (Exception ex) {
                String err = safeError(ex);
                if (event.getAttempts() + 1 >= maxAttempts) {
                    event.markDead(err);
                    log.error("USER_DELETE_SAGA_DEAD outboxId={} error={}", event.getId(), err);
                } else {
                    event.markRetry(err, Instant.now().plus(backoffSeconds, ChronoUnit.SECONDS));
                    log.warn("USER_DELETE_SAGA_RETRY outboxId={} attempt={} error={}", event.getId(), event.getAttempts(), err);
                }
            }
        }
    }

    private void processSingle(UserDeletionRequestedEvent payload) {
        if (payload == null || payload.userId() == null) {
            return;
        }
        Optional<User> maybeUser = userRepository.findById(payload.userId());
        if (maybeUser.isEmpty()) {
            // Idempotent success: account already removed in DB.
            return;
        }
        User user = maybeUser.get();
        if (user.isActive() && !user.isDeletionRequested()) {
            throw new IllegalStateException("User deletion requested event received for active user");
        }

        // Step 1: delete identity account in Keycloak (or treat as already deleted).
        Optional<String> keycloakUserId = keycloakRealmAdminClient.findUserIdByExactUsername(user.getUsername());
        keycloakUserId.ifPresent(keycloakRealmAdminClient::deleteUser);

        // Step 2: best-effort side data cleanup.
        profileAvatarStorage.delete(user.getId());
        emailVerificationCodeRepository.deleteById(user.getEmail());

        // Step 3: hard delete DB row; FK cascades remove user-owned records.
        userRepository.delete(user);
    }

    private String safeError(Exception ex) {
        String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return msg.length() > 2000 ? msg.substring(0, 2000) : msg;
    }
}

