package com.company.finance_api.portfolio.domain;

import com.company.finance_api.domain.Instrument;
import java.math.BigDecimal;

/** Tek enstrüman için miktar, maliyet, güncel fiyat ve gerçekleşmemiş PnL özetini taşır. */
public record PortfolioPosition(
    Instrument instrument,
    BigDecimal quantity,
    BigDecimal totalCost,
    BigDecimal averageCost,
    BigDecimal currentPrice,
    BigDecimal currentValue,
    BigDecimal unrealizedPnl,
    boolean hasPrice) {}
