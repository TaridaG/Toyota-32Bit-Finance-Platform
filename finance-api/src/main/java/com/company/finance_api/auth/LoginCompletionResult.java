package com.company.finance_api.auth;

import com.company.finance_api.dto.PublicLoginResponse;
import java.util.Optional;

/** Login sonucu: token response ve isteğe bağlı Set-Cookie (trusted device). */
public record LoginCompletionResult(
    PublicLoginResponse response, Optional<String> setCookieHeader) {
  /** Cookie olmadan tamamlanmış login sonucu. */
  public static LoginCompletionResult of(PublicLoginResponse response) {
    return new LoginCompletionResult(response, Optional.empty());
  }

  /** Trusted device cookie ile birlikte login sonucu. */
  public static LoginCompletionResult withCookie(
      PublicLoginResponse response, String setCookieHeader) {
    return new LoginCompletionResult(response, Optional.of(setCookieHeader));
  }
}
