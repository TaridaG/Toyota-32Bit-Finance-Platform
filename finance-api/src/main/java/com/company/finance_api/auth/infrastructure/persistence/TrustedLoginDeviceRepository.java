package com.company.finance_api.auth.infrastructure.persistence;

import com.company.finance_api.auth.domain.TrustedLoginDevice;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** TrustedLoginDevice entity persistence için Spring Data repository. */
public interface TrustedLoginDeviceRepository extends JpaRepository<TrustedLoginDevice, UUID> {

  List<TrustedLoginDevice> findByUserIdAndExpiresAtAfterOrderByLastUsedAtDescCreatedAtDesc(
      UUID userId, Instant expiresAtAfter);

  @Modifying
  @Query("DELETE FROM TrustedLoginDevice d WHERE d.id = :id AND d.userId = :userId")
  int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

  @Modifying
  @Query("DELETE FROM TrustedLoginDevice d WHERE d.userId = :userId")
  int deleteAllForUser(@Param("userId") UUID userId);

  @Modifying
  @Query("DELETE FROM TrustedLoginDevice d WHERE d.expiresAt < :cutoff")
  int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
