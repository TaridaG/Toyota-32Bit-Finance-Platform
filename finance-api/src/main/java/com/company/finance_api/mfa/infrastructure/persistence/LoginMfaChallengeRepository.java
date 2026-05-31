package com.company.finance_api.mfa.infrastructure.persistence;

import com.company.finance_api.mfa.domain.LoginMfaChallenge;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** LoginMfaChallenge entity persistence için Spring Data repository. */
public interface LoginMfaChallengeRepository extends JpaRepository<LoginMfaChallenge, UUID> {

  @Modifying
  @Query("DELETE FROM LoginMfaChallenge c WHERE c.expiresAt < :cutoff")
  int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
