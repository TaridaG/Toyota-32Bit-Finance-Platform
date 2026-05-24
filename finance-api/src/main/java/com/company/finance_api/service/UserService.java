package com.company.finance_api.service;

import com.company.finance_api.domain.User;
import com.company.finance_api.dto.BalanceResponse;
import com.company.finance_api.dto.ResponseUser;
import java.util.UUID;

/** UserService iş mantığını uygular (user service). */
public interface UserService {

  /** createUser sözleşmesi. */
  User createUser(String email, String username);

  /** getUser sözleşmesi. */
  ResponseUser getUser(UUID userId);

  BalanceResponse getUserBalance(UUID userId);
}
