package com.company.finance_api.registration;

/** Doğrulama e-postasının subject, HTML ve plain metin gövdesi. */
public record VerificationEmailContent(String subject, String htmlBody, String plainBody) {}
