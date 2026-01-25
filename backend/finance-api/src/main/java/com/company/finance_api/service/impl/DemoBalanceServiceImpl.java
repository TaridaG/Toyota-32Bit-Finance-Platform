package com.company.finance_api.service.impl;

import com.company.finance_api.domain.DemoBalance;
import com.company.finance_api.domain.User;
import com.company.finance_api.repository.DemoBalanceRepository;
import com.company.finance_api.service.DemoBalanceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DemoBalanceServiceImpl implements DemoBalanceService {

    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(100_000);
    private static final String DEFAULT_CURRENCY = "USD";

    private final DemoBalanceRepository demoBalanceRepository;

    public DemoBalanceServiceImpl(DemoBalanceRepository demoBalanceRepository) {
        this.demoBalanceRepository = demoBalanceRepository;
    }

    @Override
    public DemoBalance createForUser(User user) {
        DemoBalance balance = new DemoBalance(user, INITIAL_BALANCE, DEFAULT_CURRENCY);
        return demoBalanceRepository.save(balance);
    }

    @Override
    public DemoBalance getByUser(User user) {
        return demoBalanceRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Demo balance not found for user"));
    }
}
