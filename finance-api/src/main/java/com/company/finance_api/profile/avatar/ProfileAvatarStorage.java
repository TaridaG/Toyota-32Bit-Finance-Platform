package com.company.finance_api.profile.avatar;

import java.util.Optional;
import java.util.UUID;

/** Profil avatar dosyalarının okunması, yazılması ve silinmesi. */
public interface ProfileAvatarStorage {

  /** Kullanıcı avatar JPEG baytlarını döner. */
  Optional<byte[]> load(UUID userId);

  /** Optimize edilmiş JPEG avatar kaydeder. */
  void save(UUID userId, byte[] jpegBytes);

  /** Avatar dosyasını siler. */
  void delete(UUID userId);
}
