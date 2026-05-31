package com.company.finance_api.admin.application;

import com.company.finance_api.admin.domain.AdminPortalRosterCounters;
import com.company.finance_api.admin.infrastructure.persistence.AdminPortalRosterCountersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kalıcı hesap silme sayacını monoton olarak tutar (admin + kullanıcı saga). */
@Service
public class AdminPortalRosterCounterService {

  private final AdminPortalRosterCountersRepository repository;

  public AdminPortalRosterCounterService(AdminPortalRosterCountersRepository repository) {
    this.repository = repository;
  }

  /** Silinen hesap sayacını bir artırır. */
  @Transactional
  public void recordPermanentAccountDeletion() {
    AdminPortalRosterCounters counters =
        repository
            .findSingletonForUpdate()
            .orElseGet(() -> repository.save(AdminPortalRosterCounters.initial()));
    counters.incrementDeletedAccounts();
    repository.save(counters);
  }

  /** Toplam kalıcı silme sayısını döner. */
  @Transactional(readOnly = true)
  public long deletedAccountsTotal() {
    return repository
        .findById(AdminPortalRosterCounters.SINGLETON_ID)
        .map(AdminPortalRosterCounters::getDeletedAccountsTotal)
        .orElse(0L);
  }
}
