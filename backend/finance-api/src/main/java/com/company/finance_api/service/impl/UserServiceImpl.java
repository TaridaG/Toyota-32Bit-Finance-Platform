package com.company.finance_api.service.impl;

import com.company.finance_api.domain.User;
import com.company.finance_api.dto.BalanceResponse;
import com.company.finance_api.dto.ResponseUser;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;


    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public User createUser(String email, String username) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email already exists");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Username already exists");
        }

        User user = new User(email, username);
        User savedUser = userRepository.save(user);

        return savedUser;
    }

    private ResponseUser mapToResponse(User user) {
        return new ResponseUser(
                user.getId(),
                user.getEmail(),
                user.getUsername()
        );
    }

    @Override
    public ResponseUser getUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return mapToResponse(user);
    }

    @Override
    public BalanceResponse getUserBalance(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return new BalanceResponse(
                user.getId(),
                BigDecimal.ZERO
        );
    }


}
