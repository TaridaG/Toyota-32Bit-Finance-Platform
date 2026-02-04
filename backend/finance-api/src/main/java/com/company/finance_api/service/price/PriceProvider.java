package com.company.finance_api.service.price;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;

public interface PriceProvider {

    PriceType supports();

    InstrumentPrice fetchLatestPrice(Instrument instrument);
}