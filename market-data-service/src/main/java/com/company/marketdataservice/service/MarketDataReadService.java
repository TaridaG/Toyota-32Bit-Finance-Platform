package com.company.marketdataservice.service;

import com.company.marketdataservice.dto.FundDto;
import com.company.marketdataservice.dto.FxRateDto;
import com.company.marketdataservice.dto.MarketPriceDto;

import java.util.List;

public interface MarketDataReadService {

    List<MarketPriceDto> getLatestPrices();

    List<FxRateDto> getFxRates();

    List<FundDto> getFunds();
}
