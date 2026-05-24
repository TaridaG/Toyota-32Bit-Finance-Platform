package com.company.marketdataservice.shared.provider.tcmb;
import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * `ortak altyapı` harici provider adaptörü.
 */
public record BondEodPoint(LocalDate date, BigDecimal value) {}
