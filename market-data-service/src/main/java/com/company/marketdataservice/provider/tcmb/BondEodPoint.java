package com.company.marketdataservice.provider.tcmb;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One TCMB EVDS daily observation for a government-bond yield series. */
public record BondEodPoint(LocalDate date, BigDecimal value) {}
