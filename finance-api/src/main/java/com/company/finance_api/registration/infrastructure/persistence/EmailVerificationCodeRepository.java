package com.company.finance_api.registration.infrastructure.persistence;

import com.company.finance_api.registration.domain.EmailVerificationCodeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/** E-posta doğrulama kodu satırları için JPA repository. */
public interface EmailVerificationCodeRepository
    extends JpaRepository<EmailVerificationCodeEntry, String> {}
