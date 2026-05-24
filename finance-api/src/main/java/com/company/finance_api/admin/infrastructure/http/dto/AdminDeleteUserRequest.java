package com.company.finance_api.admin.infrastructure.http.dto;

/** Admin tarafından başlatılan kalıcı hesap kaldırma isteği. */
public record AdminDeleteUserRequest(Boolean blockEmail) {}
