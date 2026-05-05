package com.company.finance_api.profile.avatar;

import com.company.finance_api.config.ProfileAvatarProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Component
public class LocalProfileAvatarStorage implements ProfileAvatarStorage {

    private final Path root;

    public LocalProfileAvatarStorage(ProfileAvatarProperties properties) {
        this.root = Paths.get(properties.getStorageRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create profile avatar storage directory: " + this.root, e);
        }
    }

    private Path filePath(UUID userId) {
        return root.resolve(userId.toString()).resolve("avatar.jpg");
    }

    @Override
    public Optional<byte[]> load(UUID userId) {
        Path p = filePath(userId);
        if (!Files.isRegularFile(p)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(p));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public void save(UUID userId, byte[] jpegBytes) {
        Path file = filePath(userId);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, jpegBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not persist profile avatar", e);
        }
    }

    @Override
    public void delete(UUID userId) {
        Path file = filePath(userId);
        try {
            Files.deleteIfExists(file);
            Path dir = root.resolve(userId.toString());
            if (Files.isDirectory(dir)) {
                Files.deleteIfExists(dir);
            }
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
