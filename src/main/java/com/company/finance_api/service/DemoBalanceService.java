package com.company.finance_api.service;

import com.company.finance_api.domain.DemoBalance;
import com.company.finance_api.domain.User;

public interface DemoBalanceService {

    DemoBalance createForUser(User user);

    DemoBalance getByUser(User user);
}
