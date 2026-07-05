package com.company.marketdataservice.viop.application;

import com.company.marketdataservice.viop.domain.ViopContractSegment;
import com.company.marketdataservice.viop.infrastructure.http.dto.ViopActiveContractDto;
import com.company.marketdataservice.viop.infrastructure.http.dto.ViopSettlementHistoryPointDto;
import java.time.LocalDate;
import java.util.List;

/**
 * VIOP piyasa verisini okuma use-case'lerini tanımlayan application katmanı port arayüzü.
 */
public interface ViopMarketReadService {

    List<ViopActiveContractDto> getActiveContracts(ViopContractSegment segment);

    List<ViopSettlementHistoryPointDto> getContractHistory(String contractCode, LocalDate from, LocalDate to);
}

