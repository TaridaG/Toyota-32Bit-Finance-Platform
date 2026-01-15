package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.BalanceResponse;
import com.company.finance_api.dto.CreateUserRequest;
import com.company.finance_api.dto.ResponseUser;
import com.company.finance_api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<ResponseUser> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        User user = userService.createUser(
                request.getEmail(),
                request.getUsername()
        );

        ResponseUser response = new ResponseUser(
                user.getId(),
                user.getEmail(),
                user.getUsername()
        );

        return ApiResponse.success(response);
    }
    @GetMapping("/{id}")
    public ApiResponse<ResponseUser> getUser(@PathVariable UUID id) {
        return ApiResponse.success(
                userService.getUser(id)
        );
    }

    @GetMapping("/{id}/balance")
    public ApiResponse<BalanceResponse> getUserBalance(@PathVariable UUID id) {
        return ApiResponse.success(userService.getUserBalance(id));
    }
}
