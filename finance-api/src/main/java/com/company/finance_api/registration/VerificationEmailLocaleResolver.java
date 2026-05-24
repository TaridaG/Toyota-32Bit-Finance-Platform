package com.company.finance_api.registration;

import com.company.finance_api.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Doğrulama e-postası için locale çözümlemesi (istemci veya kullanıcı tercihi). */
@Component
public class VerificationEmailLocaleResolver {

  private final UserRepository userRepository;

  public VerificationEmailLocaleResolver(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * İstemci locale'i veya kayıtlı kullanıcı tercihine göre mail dili seçer.
   *
   * @param explicitLocale kayıt/profil UI'dan gelen isteğe bağlı locale
   * @param email tercih yoksa kullanıcı kaydı için aranan adres
   */
  public String resolve(String explicitLocale, String email) {
    if (StringUtils.hasText(explicitLocale)) {
      return VerificationMailLocale.normalize(explicitLocale);
    }
    return userRepository
        .findByEmailIgnoreCase(email)
        .map(user -> VerificationMailLocale.normalize(user.getPreferredLocale()))
        .orElse("en");
  }
}
