package com.company.finance_api.profile.application;

import com.company.finance_api.domain.User;
import com.company.finance_api.profile.infrastructure.http.dto.BalanceResponse;
import com.company.finance_api.profile.infrastructure.http.dto.ResponseUser;
import com.company.finance_api.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UserServiceImpl iş mantığını uygular (user service). */
@Service
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  public UserServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional
  /** Yeni User kaydı oluşturur. */
  public User createUser(String email, String username) {

    if (userRepository.findByEmail(email).isPresent()) {
      throw new IllegalStateException("Email already exists");
    }

    if (userRepository.findByUsername(username).isPresent()) {
      throw new IllegalStateException("Username already exists");
    }

    User user = new User(email, username);
    user.setEmailVerified(true);
    User savedUser = userRepository.save(user);

    return savedUser;
  }

  private ResponseUser mapToResponse(User user) {
    return new ResponseUser(user.getId(), user.getEmail(), user.getUsername());
  }

  /** User sorgusunu döner. */
  @Override
  public ResponseUser getUser(UUID userId) {

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found"));

    return mapToResponse(user);
  }

  /** UserBalance sorgusunu döner. */
  @Override
  public BalanceResponse getUserBalance(UUID userId) {

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found"));

    return new BalanceResponse(user.getId(), BigDecimal.ZERO);
  }
}
