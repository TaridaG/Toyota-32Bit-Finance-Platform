package com.company.finance_api.shared.security;

import java.util.UUID;

/** Oturum açmış portal kullanıcısının UUID kimliğini JWT veya header fallback ile çözer. */
public interface CurrentUserResolver {

  /** Mevcut istek bağlamındaki portal kullanıcı UUID'sini döner. */
  UUID getCurrentUserId();
}
