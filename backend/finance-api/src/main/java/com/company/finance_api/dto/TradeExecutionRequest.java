package com.company.finance_api.dto;

import com.company.finance_api.domain.enums.PurchaseMode;
import com.company.finance_api.domain.enums.TradeInputMode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public class TradeExecutionRequest {
    @NotNull
    private Long instrumentId;

    private Long portfolioId;

    @NotNull
    private TradeInputMode inputMode;

    private BigDecimal lots;
    private BigDecimal amount;

    private String inputCurrency;

    @NotNull
    private PurchaseMode purchaseMode;

    private Instant acquiredAt;
    private BigDecimal unitPrice;

    public Long getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Long instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public TradeInputMode getInputMode() {
        return inputMode;
    }

    public void setInputMode(TradeInputMode inputMode) {
        this.inputMode = inputMode;
    }

    public BigDecimal getLots() {
        return lots;
    }

    public void setLots(BigDecimal lots) {
        this.lots = lots;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getInputCurrency() {
        return inputCurrency;
    }

    public void setInputCurrency(String inputCurrency) {
        this.inputCurrency = inputCurrency;
    }

    public PurchaseMode getPurchaseMode() {
        return purchaseMode;
    }

    public void setPurchaseMode(PurchaseMode purchaseMode) {
        this.purchaseMode = purchaseMode;
    }

    public Instant getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(Instant acquiredAt) {
        this.acquiredAt = acquiredAt;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}

