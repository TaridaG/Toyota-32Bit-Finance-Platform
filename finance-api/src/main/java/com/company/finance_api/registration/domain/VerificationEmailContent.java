package com.company.finance_api.registration.domain;

/** Doğrulama e-postasının subject, HTML ve plain metin gövdesi. */
public record VerificationEmailContent(String subject, String htmlBody, String plainBody) {}
