package com.company.finance_api.profile.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.DemoBalance;
import com.company.finance_api.domain.User;
import com.company.finance_api.repository.DemoBalanceRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoBalanceServiceImplTest {

  @Mock private DemoBalanceRepository demoBalanceRepository;

  @InjectMocks private DemoBalanceServiceImpl demoBalanceService;

  @Test
  void createForUser_initializesDefaultBalance() {
    User user = new User("user@example.com", "trader");
    when(demoBalanceRepository.save(any(DemoBalance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DemoBalance created = demoBalanceService.createForUser(user);

    ArgumentCaptor<DemoBalance> captor = ArgumentCaptor.forClass(DemoBalance.class);
    verify(demoBalanceRepository).save(captor.capture());
    assertEquals(new BigDecimal("100000"), captor.getValue().getBalance());
    assertEquals("USD", captor.getValue().getCurrency());
    assertEquals(new BigDecimal("100000"), created.getBalance());
  }

  @Test
  void getByUser_returnsExistingBalance() {
    User user = new User("user@example.com", "trader");
    DemoBalance balance = new DemoBalance(user, BigDecimal.valueOf(5000), "USD");
    when(demoBalanceRepository.findByUser(user)).thenReturn(Optional.of(balance));

    DemoBalance found = demoBalanceService.getByUser(user);

    assertEquals(balance, found);
  }

  @Test
  void getByUser_throwsWhenMissing() {
    User user = new User("user@example.com", "trader");
    when(demoBalanceRepository.findByUser(user)).thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class, () -> demoBalanceService.getByUser(user));
  }
}
