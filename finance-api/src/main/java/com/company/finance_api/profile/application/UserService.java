package com.company.finance_api.profile.application;

import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.http.dto.BalanceResponse;
import com.company.finance_api.profile.infrastructure.http.dto.ResponseUser;
import java.util.UUID;

/** UserService iş mantığını uygular (user service). */
public interface UserService {

  /** E-posta ve kullanıcı adı ile yeni portal kullanıcısı oluşturur. */
  User createUser(String email, String username);

  /** Kullanıcı kimliğine göre profil özet response'unu döner. */
  ResponseUser getUser(UUID userId);

  BalanceResponse getUserBalance(UUID userId);
}
