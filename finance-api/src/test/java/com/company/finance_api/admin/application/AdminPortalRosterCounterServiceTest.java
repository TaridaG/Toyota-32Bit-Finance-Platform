package com.company.finance_api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.AdminPortalRosterCounters;
import com.company.finance_api.repository.AdminPortalRosterCountersRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminPortalRosterCounterServiceTest {

  @Mock AdminPortalRosterCountersRepository repository;

  @InjectMocks AdminPortalRosterCounterService service;

  @Test
  void recordPermanentAccountDeletion_incrementsExistingCounter() {
    AdminPortalRosterCounters counters = AdminPortalRosterCounters.initial();
    when(repository.findSingletonForUpdate()).thenReturn(Optional.of(counters));
    when(repository.save(counters)).thenReturn(counters);

    service.recordPermanentAccountDeletion();

    assertThat(counters.getDeletedAccountsTotal()).isEqualTo(1L);
    verify(repository).save(counters);
  }

  @Test
  void recordPermanentAccountDeletion_createsSingletonWhenMissing() {
    AdminPortalRosterCounters initial = AdminPortalRosterCounters.initial();
    when(repository.findSingletonForUpdate()).thenReturn(Optional.empty());
    when(repository.save(any(AdminPortalRosterCounters.class))).thenReturn(initial);

    service.recordPermanentAccountDeletion();

    verify(repository, times(2)).save(any(AdminPortalRosterCounters.class));
  }

  @Test
  void deletedAccountsTotal_returnsZeroWhenMissing() {
    when(repository.findById(AdminPortalRosterCounters.SINGLETON_ID)).thenReturn(Optional.empty());

    assertThat(service.deletedAccountsTotal()).isZero();
  }
}
