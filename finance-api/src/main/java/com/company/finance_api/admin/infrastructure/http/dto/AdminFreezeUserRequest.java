package com.company.finance_api.admin.infrastructure.http.dto;

import jakarta.validation.constraints.Size;

/** Kullanıcı hesabını dondurma isteği (isteğe bağlı gerekçe). */
public record AdminFreezeUserRequest(@Size(max = 500) String reason) {}
