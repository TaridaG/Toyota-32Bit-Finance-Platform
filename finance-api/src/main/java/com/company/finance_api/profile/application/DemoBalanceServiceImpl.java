package com.company.finance_api.profile.application;

import com.company.finance_api.domain.DemoBalance;
import com.company.finance_api.domain.User;
import com.company.finance_api.repository.DemoBalanceRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** DemoBalanceServiceImpl iş mantığını uygular (demo balance service). */
@Service
public class DemoBalanceServiceImpl implements DemoBalanceService {

  private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(100_000);
  private static final String DEFAULT_CURRENCY = "USD";

  private final DemoBalanceRepository demoBalanceRepository;

  public DemoBalanceServiceImpl(DemoBalanceRepository demoBalanceRepository) {
    this.demoBalanceRepository = demoBalanceRepository;
  }

  /** Yeni ForUser kaydı oluşturur. */
  @Override
  public DemoBalance createForUser(User user) {
    DemoBalance balance = new DemoBalance(user, INITIAL_BALANCE, DEFAULT_CURRENCY);
    return demoBalanceRepository.save(balance);
  }

  /** ByUser sorgusunu döner. */
  @Override
  public DemoBalance getByUser(User user) {
    return demoBalanceRepository
        .findByUser(user)
        .orElseThrow(() -> new IllegalStateException("Demo balance not found for user"));
  }
}
