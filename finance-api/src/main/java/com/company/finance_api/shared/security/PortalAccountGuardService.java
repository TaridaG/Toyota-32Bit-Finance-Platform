package com.company.finance_api.shared.security;

import com.company.finance_api.domain.User;
import com.company.finance_api.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Portal kullanıcı hesabının dondurulmuş/kaldırılmış olup olmadığını JWT claim ve DB kaydı
 * üzerinden doğrular.
 */
@Service
public class PortalAccountGuardService {

  public static final String ACCOUNT_FROZEN_ERROR_CODE = "ACCOUNT_FROZEN";
  public static final String ACCOUNT_FROZEN_MESSAGE =
      "Hesabınız donduruldu. Erişim için destek ekibiyle iletişime geçin.";

  public static final String ACCOUNT_REMOVED_ERROR_CODE = "ACCOUNT_REMOVED";
  public static final String ACCOUNT_REMOVED_MESSAGE =
      "Hesabınız kaldırıldı. Oturumunuz sonlandırıldı.";

  private final UserRepository userRepository;

  public PortalAccountGuardService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /** Authentication'da {@code ROLE_ADMIN} authority var mı kontrol eder. */
  public boolean isAdmin(Authentication authentication) {
    if (authentication == null) {
      return false;
    }
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if ("ROLE_ADMIN".equals(authority.getAuthority())) {
        return true;
      }
    }
    return false;
  }

  /** JWT authentication token'dan portal {@link User} kaydını çözer. */
  public Optional<User> resolvePortalUser(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      return Optional.empty();
    }
    return resolvePortalUser(jwtAuth.getToken());
  }

  /** JWT claim'lerinden portal {@link User} kaydını çözer. */
  public Optional<User> resolvePortalUser(Jwt jwt) {
    if (jwt == null) {
      return Optional.empty();
    }
    String preferred = jwt.getClaimAsString("preferred_username");
    if (StringUtils.hasText(preferred)) {
      Optional<User> user = findByIdentity(preferred);
      if (user.isPresent()) {
        return user;
      }
    }
    String subject = jwt.getSubject();
    if (StringUtils.hasText(subject)) {
      return findByIdentity(subject);
    }
    return Optional.empty();
  }

  /**
   * Portal kullanıcısı aktif değilse (frozen) {@link ResponseStatusException} fırlatır; admin'ler
   * muaf.
   */
  public void assertActivePortalAccount(Authentication authentication) {
    if (authentication == null || isAdmin(authentication)) {
      return;
    }
    resolvePortalUser(authentication).ifPresent(this::assertNotFrozen);
  }

  /** Kullanıcı frozen ise HTTP 403 fırlatır. */
  public void assertNotFrozen(User user) {
    if (user != null && user.isFrozen()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, ACCOUNT_FROZEN_MESSAGE);
    }
  }

  /**
   * JWT portal kimlik claim'i taşıyorsa {@code true} (silinen kullanıcılar DB'de artık çözülmez).
   */
  public boolean hasPortalIdentityClaims(Jwt jwt) {
    if (jwt == null) {
      return false;
    }
    return StringUtils.hasText(jwt.getClaimAsString("preferred_username"))
        || StringUtils.hasText(jwt.getSubject());
  }

  private Optional<User> findByIdentity(String identity) {
    String key = identity.trim().toLowerCase(Locale.ROOT);
    return userRepository
        .findByAuthUsernameIgnoreCase(key)
        .or(() -> userRepository.findByUsernameIgnoreCase(key))
        .or(() -> userRepository.findByEmailIgnoreCase(key));
  }
}
