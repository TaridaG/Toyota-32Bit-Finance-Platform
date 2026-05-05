package com.company.finance_api.profile.avatar;

import java.util.Optional;
import java.util.UUID;

public interface ProfileAvatarStorage {

    Optional<byte[]> load(UUID userId);

    void save(UUID userId, byte[] jpegBytes);

    void delete(UUID userId);
}
