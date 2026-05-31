package com.company.finance_api.pricing.infrastructure.provider;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.pricing.domain.enums.PriceType;

/** PriceProvider iş mantığını uygular (price provider). */
public interface PriceProvider {

  /** supports sözleşmesi. */
  PriceType supports();

  InstrumentPrice fetchLatestPrice(Instrument instrument);
}
