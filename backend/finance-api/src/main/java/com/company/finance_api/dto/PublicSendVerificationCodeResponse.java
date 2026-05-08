package com.company.finance_api.dto;

public class PublicSendVerificationCodeResponse {
    private final int expiresInSeconds;
    private final int resendInSeconds;

    public PublicSendVerificationCodeResponse(int expiresInSeconds, int resendInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
        this.resendInSeconds = resendInSeconds;
    }

    public int getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public int getResendInSeconds() {
        return resendInSeconds;
    }
}

