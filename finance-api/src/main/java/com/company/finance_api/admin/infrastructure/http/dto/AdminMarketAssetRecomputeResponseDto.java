package com.company.finance_api.admin.infrastructure.http.dto;

/** Market-asset yeniden hesaplama isteği yanıtı (durum + başlatıldı mı). */
public record AdminMarketAssetRecomputeResponseDto(String status, boolean started) {}
