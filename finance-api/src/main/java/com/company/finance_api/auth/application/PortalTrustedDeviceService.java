package com.company.finance_api.auth.application;

import com.company.finance_api.bootstrap.config.TrustedDeviceProperties;
import com.company.finance_api.auth.domain.TrustedLoginDevice;
import com.company.finance_api.auth.infrastructure.http.dto.PortalTrustedDeviceRowDto;
import com.company.finance_api.auth.infrastructure.http.dto.PortalTrustedDevicesResponseDto;
import com.company.finance_api.auth.infrastructure.persistence.TrustedLoginDeviceRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** "Bu cihaza güven" cookie'sini üretir, doğrular ve portal profilinden yönetilir. */
@Service
public class PortalTrustedDeviceService {

  private static final String COOKIE_VERSION = "v1";

  private final TrustedDeviceProperties properties;
  private final TrustedLoginDeviceRepository repository;
  private final SecureRandom secureRandom = new SecureRandom();

  public PortalTrustedDeviceService(
      TrustedDeviceProperties properties, TrustedLoginDeviceRepository repository) {
    this.properties = properties;
    this.repository = repository;
  }

  /** İstekteki cookie'nin kullanıcı için geçerli trusted device olup olmadığını kontrol eder. */
  @Transactional
  public boolean isTrustedForUser(HttpServletRequest request, UUID userId) {
    if (!properties.isEnabled() || userId == null) {
      return false;
    }
    Optional<UUID> deviceId = validateCookieDevice(request, userId);
    if (deviceId.isEmpty()) {
      return false;
    }
    touchLastUsed(deviceId.get(), Instant.now());
    return true;
  }

  /** Kullanıcının aktif trusted device listesini döner. */
  @Transactional(readOnly = true)
  public PortalTrustedDevicesResponseDto listForUser(UUID userId, HttpServletRequest request) {
    if (!properties.isEnabled() || userId == null) {
      return new PortalTrustedDevicesResponseDto(false, List.of());
    }
    // Do not purge here: read-only transaction; expired rows are excluded by query below.
    Optional<UUID> currentId = currentDeviceId(request, userId);
    Instant now = Instant.now();
    List<PortalTrustedDeviceRowDto> rows = new ArrayList<>();
    for (TrustedLoginDevice device :
        repository.findByUserIdAndExpiresAtAfterOrderByLastUsedAtDescCreatedAtDesc(userId, now)) {
      rows.add(
          new PortalTrustedDeviceRowDto(
              device.getId(),
              device.getCreatedAt(),
              device.getLastUsedAt(),
              device.getExpiresAt(),
              currentId.map(id -> id.equals(device.getId())).orElse(false)));
    }
    return new PortalTrustedDevicesResponseDto(true, rows);
  }

  /** Mevcut oturumdaki trusted device id'sini cookie'den çözümler. */
  public Optional<UUID> currentDeviceId(HttpServletRequest request, UUID userId) {
    if (!properties.isEnabled() || userId == null) {
      return Optional.empty();
    }
    return validateCookieDevice(request, userId);
  }

  /** Tek bir trusted device kaydını siler; mevcut cihazsa cookie temizleme header'ı döner. */
  @Transactional
  public Optional<String> revokeDeviceForUser(
      UUID userId, UUID deviceId, HttpServletRequest request) {
    if (userId == null || deviceId == null) {
      return Optional.empty();
    }
    purgeExpired();
    boolean wasCurrent = currentDeviceId(request, userId).map(deviceId::equals).orElse(false);
    repository.deleteByIdAndUserId(deviceId, userId);
    if (wasCurrent) {
      return clearTrustedDeviceCookieHeader();
    }
    return Optional.empty();
  }

  /** Tüm trusted device kayıtlarını siler ve oturum cookie'sini temizler. */
  @Transactional
  public Optional<String> revokeAllForUserSession(UUID userId) {
    if (userId == null) {
      return Optional.empty();
    }
    revokeAllForUser(userId);
    return clearTrustedDeviceCookieHeader();
  }

  /** Yeni trusted device kaydı oluşturur ve Set-Cookie header değerini döner. */
  @Transactional
  public Optional<String> issueTrustedDeviceCookie(UUID userId) {
    if (!properties.isEnabled() || userId == null) {
      return Optional.empty();
    }
    purgeExpired();
    UUID deviceId = UUID.randomUUID();
    byte[] secret = new byte[32];
    secureRandom.nextBytes(secret);
    String secretB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    String payload = deviceId + "." + secretB64;
    String signature = sign(payload);
    String cookieValue = COOKIE_VERSION + "." + payload + "." + signature;

    Instant expiresAt = Instant.now().plus(properties.getMaxAgeDays(), ChronoUnit.DAYS);
    repository.save(new TrustedLoginDevice(deviceId, userId, hashSecret(secretB64), expiresAt));
    return Optional.of(buildSetCookieHeader(cookieValue, properties.getMaxAgeDays() * 86400L));
  }

  /** Kullanıcıya ait tüm trusted device kayıtlarını veritabanından siler. */
  @Transactional
  public void revokeAllForUser(UUID userId) {
    if (userId == null) {
      return;
    }
    repository.deleteAllForUser(userId);
  }

  /** Tarayıcıdaki trusted device cookie'sini silmek için Set-Cookie header üretir. */
  public Optional<String> clearTrustedDeviceCookieHeader() {
    if (!properties.isEnabled()) {
      return Optional.empty();
    }
    ResponseCookie cookie =
        ResponseCookie.from(properties.getCookieName(), "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();
    return Optional.of(cookie.toString());
  }

  private Optional<UUID> validateCookieDevice(HttpServletRequest request, UUID expectedUserId) {
    String raw = readCookie(request, properties.getCookieName());
    if (!StringUtils.hasText(raw)) {
      return Optional.empty();
    }
    String[] parts = raw.split("\\.");
    if (parts.length != 5 || !COOKIE_VERSION.equals(parts[0])) {
      return Optional.empty();
    }
    String payload = parts[1] + "." + parts[2];
    String signature = parts[4];
    if (!constantTimeEquals(sign(payload), signature)) {
      return Optional.empty();
    }
    UUID deviceId;
    String secretB64;
    try {
      deviceId = UUID.fromString(parts[1]);
      secretB64 = parts[2];
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
    Optional<TrustedLoginDevice> row = repository.findById(deviceId);
    if (row.isEmpty()) {
      return Optional.empty();
    }
    TrustedLoginDevice device = row.get();
    Instant now = Instant.now();
    if (!expectedUserId.equals(device.getUserId()) || device.getExpiresAt().isBefore(now)) {
      return Optional.empty();
    }
    if (!constantTimeEquals(device.getTokenHash(), hashSecret(secretB64))) {
      return Optional.empty();
    }
    return Optional.of(deviceId);
  }

  private void touchLastUsed(UUID deviceId, Instant at) {
    repository
        .findById(deviceId)
        .ifPresent(
            device -> {
              device.touchLastUsed(at);
              repository.save(device);
            });
  }

  private void purgeExpired() {
    repository.deleteExpiredBefore(Instant.now());
  }

  private static String readCookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
        return cookie.getValue().trim();
      }
    }
    return null;
  }

  private String sign(String payload) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] raw =
          digest.digest(
              (properties.getSigningSecret() + "|" + payload).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(raw);
    } catch (Exception ex) {
      throw new IllegalStateException("Trusted device signing failed", ex);
    }
  }

  private static String hashSecret(String secretB64) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(secretB64.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Trusted device hash failed", ex);
    }
  }

  private static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) {
      return false;
    }
    byte[] left = a.getBytes(StandardCharsets.UTF_8);
    byte[] right = b.getBytes(StandardCharsets.UTF_8);
    if (left.length != right.length) {
      return false;
    }
    int diff = 0;
    for (int i = 0; i < left.length; i++) {
      diff |= left[i] ^ right[i];
    }
    return diff == 0;
  }

  private String buildSetCookieHeader(String value, long maxAgeSeconds) {
    ResponseCookie cookie =
        ResponseCookie.from(properties.getCookieName(), value)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(maxAgeSeconds)
            .sameSite("Lax")
            .build();
    return cookie.toString();
  }
}
