package com.company.finance_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PortalChangeUsernameRequest {

    @NotBlank
    @Size(min = 3, max = 36)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may contain letters, digits, dot, underscore, hyphen")
    private String newUsername;

    @NotBlank
    private String currentPassword;

    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
