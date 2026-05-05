package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.PortalChangePasswordRequest;
import com.company.finance_api.dto.PortalChangeUsernameRequest;
import com.company.finance_api.dto.PortalProfileResponse;
import com.company.finance_api.dto.PortalUpdateNotificationsRequest;
import com.company.finance_api.dto.PortalUpdatePhoneRequest;
import com.company.finance_api.dto.PublicLoginResponse;
import com.company.finance_api.profile.PortalProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/api/portal/profile")
public class PortalProfileController {

    private static final Logger log = LoggerFactory.getLogger(PortalProfileController.class);

    private final PortalProfileService portalProfileService;

    public PortalProfileController(PortalProfileService portalProfileService) {
        this.portalProfileService = portalProfileService;
    }

    @GetMapping
    public ApiResponse<PortalProfileResponse> getProfile() {
        return ApiResponse.success(portalProfileService.getProfile());
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PortalChangePasswordRequest request) {
        portalProfileService.changePassword(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/username")
    public ApiResponse<PublicLoginResponse> changeUsername(@Valid @RequestBody PortalChangeUsernameRequest request) {
        return ApiResponse.success(portalProfileService.changeUsername(request));
    }

    @PutMapping("/phone")
    public ApiResponse<PortalProfileResponse> updatePhone(@Valid @RequestBody PortalUpdatePhoneRequest request) {
        return ApiResponse.success(portalProfileService.updatePhone(request));
    }

    @PutMapping("/notifications")
    public ApiResponse<PortalProfileResponse> updateNotifications(
            @Valid @RequestBody PortalUpdateNotificationsRequest request
    ) {
        return ApiResponse.success(portalProfileService.updateNotifications(request));
    }

    @GetMapping(value = "/avatar", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getAvatar() {
        byte[] body = portalProfileService.readAvatarForCurrentUser();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .contentType(MediaType.IMAGE_JPEG)
                .body(body);
    }

    /**
     * Multipart uploads use POST (not PUT): some servers and proxies do not parse {@code multipart/form-data}
     * reliably for PUT, which surfaces as a non-Runtime {@code ServletException} and a generic 500 to the client.
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PortalProfileResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        try {
            return ApiResponse.success(portalProfileService.uploadAvatar(file));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Avatar upload failed (checked exception)", e);
            String detail = e.getClass().getSimpleName();
            if (StringUtils.hasText(e.getMessage())) {
                detail += ": " + e.getMessage();
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, detail, e);
        }
    }

    @DeleteMapping("/avatar")
    public ApiResponse<PortalProfileResponse> deleteAvatar() {
        return ApiResponse.success(portalProfileService.deleteAvatar());
    }
}
