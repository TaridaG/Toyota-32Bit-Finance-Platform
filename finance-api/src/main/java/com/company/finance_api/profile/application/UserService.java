package com.company.finance_api.profile.application;

import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.http.dto.BalanceResponse;
import com.company.finance_api.profile.infrastructure.http.dto.ResponseUser;
import java.util.UUID;

/** UserService iş mantığını uygular (user service). */
public interface UserService {

  /** createUser sözleşmesi. */
  User createUser(String email, String username);

  /** getUser sözleşmesi. */
  ResponseUser getUser(UUID userId);

  BalanceResponse getUserBalance(UUID userId);
}
