package com.company.finance_api.admin.infrastructure.persistence;

import com.company.finance_api.admin.domain.AdminPortalRosterCounters;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** AdminPortalRosterCounters entity persistence için Spring Data repository. */
public interface AdminPortalRosterCountersRepository
    extends JpaRepository<AdminPortalRosterCounters, Short> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from AdminPortalRosterCounters c where c.id = 1")
  Optional<AdminPortalRosterCounters> findSingletonForUpdate();
}
