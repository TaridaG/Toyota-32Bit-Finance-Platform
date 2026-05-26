package com.company.marketdataservice.rates.infrastructure.http.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TlDepositIndexPointDto(
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
    BigDecimal indexValue,
    BigDecimal annualRatePercent,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate sourceObservationDate) {}
