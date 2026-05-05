package com.company.finance_api.dto;

import jakarta.validation.constraints.Size;

public class PortalUpdatePhoneRequest {

    /** Null leaves unchanged; empty string clears. Otherwise normalized to digits and optional leading +. */
    @Size(max = 32)
    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
