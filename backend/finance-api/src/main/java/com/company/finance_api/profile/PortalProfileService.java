package com.company.finance_api.profile;

import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PortalChangePasswordRequest;
import com.company.finance_api.dto.PortalChangeUsernameRequest;
import com.company.finance_api.dto.PortalProfileResponse;
import com.company.finance_api.dto.PortalUpdateNotificationsRequest;
import com.company.finance_api.dto.PortalUpdatePreferencesRequest;
import com.company.finance_api.dto.PortalUpdatePhoneRequest;
import com.company.finance_api.dto.PublicLoginResponse;
import com.company.finance_api.exception.ResourceNotFoundException;
import com.company.finance_api.identity.KeycloakDirectGrantClient;
import com.company.finance_api.identity.KeycloakRealmAdminClient;
import com.company.finance_api.profile.avatar.ProfileAvatarImageProcessor;
import com.company.finance_api.profile.avatar.ProfileAvatarStorage;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.security.CurrentUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class PortalProfileService {
    private static final String DEFAULT_PREFERRED_LOCALE = "en";
    private static final String DEFAULT_PREFERRED_CURRENCY = "USD";

    private final CurrentUserResolver currentUserResolver;
    private final UserRepository userRepository;
    private final KeycloakRealmAdminClient keycloakRealmAdminClient;
    private final KeycloakDirectGrantClient keycloakDirectGrantClient;
    private final ProfileAvatarStorage profileAvatarStorage;
    private final ProfileAvatarImageProcessor profileAvatarImageProcessor;

    public PortalProfileService(
            CurrentUserResolver currentUserResolver,
            UserRepository userRepository,
            KeycloakRealmAdminClient keycloakRealmAdminClient,
            KeycloakDirectGrantClient keycloakDirectGrantClient,
            ProfileAvatarStorage profileAvatarStorage,
            ProfileAvatarImageProcessor profileAvatarImageProcessor
    ) {
        this.currentUserResolver = currentUserResolver;
        this.userRepository = userRepository;
        this.keycloakRealmAdminClient = keycloakRealmAdminClient;
        this.keycloakDirectGrantClient = keycloakDirectGrantClient;
        this.profileAvatarStorage = profileAvatarStorage;
        this.profileAvatarImageProcessor = profileAvatarImageProcessor;
    }

    @Transactional(readOnly = true)
    public PortalProfileResponse getProfile() {
        User user = loadCurrentUser();
        return mapProfile(user);
    }

    @Transactional(readOnly = true)
    public byte[] readAvatarForCurrentUser() {
        User user = loadCurrentUser();
        if (user.getProfileAvatarUpdatedAt() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return profileAvatarStorage.load(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public PortalProfileResponse uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be an image");
        }
        User user = loadCurrentUser();
        byte[] raw;
        try {
            raw = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file", e);
        }
        byte[] processed = profileAvatarImageProcessor.toOptimizedJpeg(raw);
        try {
            profileAvatarStorage.save(user.getId(), processed);
        } catch (UncheckedIOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store avatar", ex);
        }
        user.setProfileAvatarUpdatedAt(Instant.now());
        userRepository.save(user);
        return mapProfile(user);
    }

    @Transactional
    public PortalProfileResponse deleteAvatar() {
        User user = loadCurrentUser();
        if (user.getProfileAvatarUpdatedAt() != null) {
            profileAvatarStorage.delete(user.getId());
        }
        user.setProfileAvatarUpdatedAt(null);
        userRepository.save(user);
        return mapProfile(user);
    }

    public void changePassword(PortalChangePasswordRequest request) {
        User user = loadCurrentUser();
        verifyCurrentPassword(user.getUsername(), request.getCurrentPassword());
        String kcId = keycloakRealmAdminClient.findUserIdByExactUsername(user.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Identity account not found for user"));
        keycloakRealmAdminClient.resetUserPassword(kcId, request.getNewPassword(), false);
    }

    public PublicLoginResponse changeUsername(PortalChangeUsernameRequest request) {
        User user = loadCurrentUser();
        String oldUsername = user.getUsername();
        String newUsername = request.getNewUsername().trim().toLowerCase(Locale.ROOT);
        if (oldUsername.equals(newUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username unchanged");
        }
        if (userRepository.findByUsername(newUsername).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
        verifyCurrentPassword(oldUsername, request.getCurrentPassword());

        String kcId = keycloakRealmAdminClient.findUserIdByExactUsername(oldUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Identity account not found for user"));

        try {
            keycloakRealmAdminClient.updateRealmUsername(kcId, newUsername);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }

        try {
            user.changeUsername(newUsername);
            userRepository.save(user);
        } catch (RuntimeException ex) {
            try {
                keycloakRealmAdminClient.updateRealmUsername(kcId, oldUsername);
            } catch (RuntimeException ignored) {
                // best-effort rollback of identity provider username
            }
            throw ex;
        }

        return keycloakDirectGrantClient.exchangePassword(newUsername, request.getCurrentPassword());
    }

    @Transactional
    public PortalProfileResponse updatePhone(PortalUpdatePhoneRequest request) {
        User user = loadCurrentUser();
        if (request.getPhone() == null) {
            return mapProfile(user);
        }
        user.setPhone(normalizePhone(request.getPhone()));
        userRepository.save(user);
        return mapProfile(user);
    }

    @Transactional
    public PortalProfileResponse updateNotifications(PortalUpdateNotificationsRequest request) {
        User user = loadCurrentUser();
        user.setNotifySecurityAlerts(Boolean.TRUE.equals(request.getNotifySecurityAlerts()));
        user.setNotifyProductUpdates(Boolean.TRUE.equals(request.getNotifyProductUpdates()));
        userRepository.save(user);
        return mapProfile(user);
    }

    @Transactional
    public PortalProfileResponse updatePreferences(PortalUpdatePreferencesRequest request) {
        User user = loadCurrentUser();
        user.setPreferredLocale(normalizePreferredLocale(request.getPreferredLocale()));
        user.setPreferredCurrency(normalizePreferredCurrency(request.getPreferredCurrency()));
        userRepository.save(user);
        return mapProfile(user);
    }

    private PortalProfileResponse mapProfile(User user) {
        return new PortalProfileResponse(
                user.getEmail(),
                user.getUsername(),
                user.getPhone(),
                user.isNotifySecurityAlerts(),
                user.isNotifyProductUpdates(),
                user.getProfileAvatarUpdatedAt(),
                normalizePreferredLocale(user.getPreferredLocale()),
                normalizePreferredCurrency(user.getPreferredCurrency())
        );
    }

    private User loadCurrentUser() {
        UUID userId = currentUserResolver.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void verifyCurrentPassword(String username, String currentPassword) {
        try {
            keycloakDirectGrantClient.exchangePassword(username, currentPassword);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect", ex);
            }
            throw ex;
        }
    }

    static String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }
        boolean leadingPlus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone must contain digits");
        }
        String compact = (leadingPlus ? "+" : "") + digits;
        if (compact.length() > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone is too long");
        }
        return compact;
    }

    private String normalizePreferredLocale(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT_PREFERRED_LOCALE;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("-")) {
            normalized = normalized.substring(0, normalized.indexOf('-'));
        }
        if (normalized.contains("_")) {
            normalized = normalized.substring(0, normalized.indexOf('_'));
        }
        return switch (normalized) {
            case "en", "tr", "de" -> normalized;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported preferred locale");
        };
    }

    private String normalizePreferredCurrency(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT_PREFERRED_CURRENCY;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "USD", "EUR", "TRY", "GBP", "JPY", "AED" -> normalized;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported preferred currency");
        };
    }
}
