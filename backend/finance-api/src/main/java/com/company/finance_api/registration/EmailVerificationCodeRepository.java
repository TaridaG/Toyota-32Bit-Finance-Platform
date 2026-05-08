package com.company.finance_api.registration;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCodeEntry, String> {
}

