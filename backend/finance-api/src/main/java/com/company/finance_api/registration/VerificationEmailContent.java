package com.company.finance_api.registration;

public record VerificationEmailContent(
        String subject,
        String htmlBody,
        String plainBody
) {
}
