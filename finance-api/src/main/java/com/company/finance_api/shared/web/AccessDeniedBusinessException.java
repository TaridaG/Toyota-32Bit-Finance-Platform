package com.company.finance_api.shared.web;

/** İş kuralı kaynaklı erişim reddi; HTTP 403 ile eşlenir. */
public class AccessDeniedBusinessException extends RuntimeException {

  /** Kullanıcıya yansıyacak mesajla exception oluşturur. */
  public AccessDeniedBusinessException(String message) {
    super(message);
  }
}
