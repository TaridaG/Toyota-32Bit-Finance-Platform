package com.company.finance_api.admin.infrastructure.http.dto;

import java.time.Instant;
import java.util.UUID;

/** Kullanıcı analytics'te son kayıt satırı (maskelenmiş e-posta). */
public record AdminRecentRegistrationRowDto(
    UUID id,
    String displayName,
    String maskedEmail,
    Instant createdAt,
    /** İzleniyorsa kayıt kanalı; bilinmiyorsa boş (UI em dash gösterir). */
    String source,
    /** İzleniyorsa istemci cihaz sınıfı; bilinmiyorsa boş. */
    String device,
    /** Roster hesapları için {@code ACTIVE} veya {@code INACTIVE}. */
    String status) {}
