package com.company.finance_api.admin.infrastructure.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin'den kullanıcıya mesaj gönderme isteği. */
public record AdminSendUserMessageRequest(@NotBlank @Size(min = 1, max = 2000) String message) {}
