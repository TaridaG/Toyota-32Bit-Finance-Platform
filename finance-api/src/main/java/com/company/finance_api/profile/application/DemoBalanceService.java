package com.company.finance_api.profile.application;

import com.company.finance_api.profile.domain.DemoBalance;
import com.company.finance_api.profile.domain.User;

/** DemoBalanceService iş mantığını uygular (demo balance service). */
public interface DemoBalanceService {

  /** Kullanıcı için başlangıç demo bakiyesi kaydı oluşturur. */
  DemoBalance createForUser(User user);

  /** Kullanıcının demo bakiye kaydını döner. */
  DemoBalance getByUser(User user);
}
