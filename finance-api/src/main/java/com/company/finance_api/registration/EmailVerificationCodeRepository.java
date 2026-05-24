package com.company.finance_api.registration;

import org.springframework.data.jpa.repository.JpaRepository;

/** E-posta doğrulama kodu satırları için JPA repository. */
public interface EmailVerificationCodeRepository
    extends JpaRepository<EmailVerificationCodeEntry, String> {}
