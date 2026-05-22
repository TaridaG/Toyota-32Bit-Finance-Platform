package com.company.finance_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.registration.verification")
public class RegistrationVerificationProperties {
    private boolean enabled = true;
    private int codeLength = 6;
    private int ttlSeconds = 90;
    private int resendCooldownSeconds = 15;
    private int maxAttempts = 5;
    private String hashSecret = "change-me";
    private String from = "";
    private String subject = "Your Finance Portal verification code";
    /** Optional link shown in the email footer (e.g. https://portal.example.com). */
    private String portalPublicUrl = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public void setHashSecret(String hashSecret) {
        this.hashSecret = hashSecret;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPortalPublicUrl() {
        return portalPublicUrl;
    }

    public void setPortalPublicUrl(String portalPublicUrl) {
        this.portalPublicUrl = portalPublicUrl;
    }
}

