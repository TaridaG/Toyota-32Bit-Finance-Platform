package com.company.finance_api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false)
    private boolean notifySecurityAlerts = true;

    @Column(nullable = false)
    private boolean notifyProductUpdates = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "profile_avatar_updated_at")
    private Instant profileAvatarUpdatedAt;

    protected User() {
        // JPA only
    }

    public User(String email, String username) {
        this.email = email;
        this.username = username;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isNotifySecurityAlerts() {
        return notifySecurityAlerts;
    }

    public boolean isNotifyProductUpdates() {
        return notifyProductUpdates;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProfileAvatarUpdatedAt() {
        return profileAvatarUpdatedAt;
    }

    public void setProfileAvatarUpdatedAt(Instant profileAvatarUpdatedAt) {
        this.profileAvatarUpdatedAt = profileAvatarUpdatedAt;
    }

    public void changeUsername(String newUsername) {
        this.username = newUsername.trim().toLowerCase(Locale.ROOT);
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setNotifySecurityAlerts(boolean notifySecurityAlerts) {
        this.notifySecurityAlerts = notifySecurityAlerts;
    }

    public void setNotifyProductUpdates(boolean notifyProductUpdates) {
        this.notifyProductUpdates = notifyProductUpdates;
    }
}
