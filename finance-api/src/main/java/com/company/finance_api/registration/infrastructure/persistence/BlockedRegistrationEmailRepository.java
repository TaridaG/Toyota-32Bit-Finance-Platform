package com.company.finance_api.registration.infrastructure.persistence;

import com.company.finance_api.registration.domain.BlockedRegistrationEmail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** BlockedRegistrationEmail entity persistence için Spring Data repository. */
public interface BlockedRegistrationEmailRepository
    extends JpaRepository<BlockedRegistrationEmail, Long> {

  boolean existsByEmail(String email);

  Page<BlockedRegistrationEmail> findAllByOrderByBlockedAtDesc(Pageable pageable);
}
