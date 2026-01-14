package com.company.finance_api.service;

import com.company.finance_api.domain.User;

public interface UserService {

    User createUser(String email, String username);
}
