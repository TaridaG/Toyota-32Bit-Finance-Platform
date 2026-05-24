package com.company.finance_api.profile.infrastructure.http;

import com.company.finance_api.domain.User;
import com.company.finance_api.dto.BalanceResponse;
import com.company.finance_api.dto.CreateUserRequest;
import com.company.finance_api.dto.ResponseUser;
import com.company.finance_api.service.UserService;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/** Dahili kullanıcı oluşturma ve bakiye sorgulama endpoint'leri. */
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /** Yeni kullanıcı kaydı oluşturur. */
  @PostMapping
  public ApiResponse<ResponseUser> createUser(@Valid @RequestBody CreateUserRequest request) {
    User user = userService.createUser(request.getEmail(), request.getUsername());

    ResponseUser response = new ResponseUser(user.getId(), user.getEmail(), user.getUsername());

    return ApiResponse.success(response);
  }

  /** Kullanıcı bilgisini id ile döner. */
  @GetMapping("/{id}")
  public ApiResponse<ResponseUser> getUser(@PathVariable UUID id) {
    return ApiResponse.success(userService.getUser(id));
  }

  /** Kullanıcı nakit bakiyesini döner. */
  @GetMapping("/{id}/balance")
  public ApiResponse<BalanceResponse> getUserBalance(@PathVariable UUID id) {
    return ApiResponse.success(userService.getUserBalance(id));
  }
}
