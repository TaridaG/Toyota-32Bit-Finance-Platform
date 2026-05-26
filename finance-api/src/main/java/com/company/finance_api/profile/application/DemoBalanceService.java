package com.company.finance_api.profile.application;

import com.company.finance_api.domain.DemoBalance;
import com.company.finance_api.domain.User;

/** DemoBalanceService iş mantığını uygular (demo balance service). */
public interface DemoBalanceService {

  /** createForUser sözleşmesi. */
  DemoBalance createForUser(User user);

  /** getByUser sözleşmesi. */
  DemoBalance getByUser(User user);
}
