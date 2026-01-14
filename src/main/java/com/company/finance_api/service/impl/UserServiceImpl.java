package com.company.finance_api.service.impl;

import com.company.finance_api.domain.User;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(String email, String username) {
        User user = new User(email, username);
        return userRepository.save(user);
    }
}
