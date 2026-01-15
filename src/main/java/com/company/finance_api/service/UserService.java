package com.company.finance_api.service;

import com.company.finance_api.domain.User;
import com.company.finance_api.dto.BalanceResponse;
import com.company.finance_api.dto.ResponseUser;

import java.util.UUID;


public interface UserService {

    User createUser(String email, String username);

    ResponseUser getUser(UUID userId);

    BalanceResponse getUserBalance(UUID userId);

}
