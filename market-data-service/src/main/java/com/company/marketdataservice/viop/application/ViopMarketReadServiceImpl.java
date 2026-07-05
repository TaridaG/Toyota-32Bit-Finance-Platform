package com.company.marketdataservice.viop.application;

import com.company.marketdataservice.viop.domain.ViopContractSegment;
import com.company.marketdataservice.viop.infrastructure.http.dto.ViopActiveContractDto;
import com.company.marketdataservice.viop.infrastructure.http.dto.ViopSettlementHistoryPointDto;
import com.company.marketdataservice.viop.infrastructure.persistence.ViopMarketReadJdbcRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * VIOP aktif sözleşme ve settlement geçmişi sorgularını JDBC persistence üzerinden sunan
 * application katmanı use-case servisi.
 */
@Service
@RequiredArgsConstructor
public class ViopMarketReadServiceImpl implements ViopMarketReadService {

    private final ViopMarketReadJdbcRepository readJdbcRepository;

    @Override
    public List<ViopActiveContractDto> getActiveContracts(ViopContractSegment segment) {
        return readJdbcRepository.findActiveContracts(segment);
    }

    @Override
    public List<ViopSettlementHistoryPointDto> getContractHistory(String contractCode, LocalDate from, LocalDate to) {
        return readJdbcRepository.findHistory(contractCode, from, to);
    }
}

