package com.company.marketdataservice.viop.application;

import com.company.marketdataservice.viop.infrastructure.http.dto.ViopActiveContractDto;
import com.company.marketdataservice.viop.infrastructure.http.dto.ViopSettlementHistoryPointDto;
import java.time.LocalDate;
import java.util.List;

public interface ViopMarketReadService {
    List<ViopActiveContractDto> getActiveContracts();

    List<ViopSettlementHistoryPointDto> getContractHistory(String contractCode, LocalDate from, LocalDate to);
}

