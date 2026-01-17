package com.company.finance_api.config;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.Exchange;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.repository.InstrumentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class InstrumentDataInitializer {

    private final InstrumentRepository instrumentRepository;

    public InstrumentDataInitializer(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    @PostConstruct
    public void init() {

        if (instrumentRepository.count() > 0) {
            return; // tekrar tekrar eklemesin
        }

        instrumentRepository.save(
                new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE)
        );

        instrumentRepository.save(
                new Instrument("ETHUSDT", "Ethereum", InstrumentType.CRYPTO, Exchange.BINANCE)
        );

        instrumentRepository.save(
                new Instrument("ASELS", "Aselsan", InstrumentType.STOCK, Exchange.BIST)
        );

        instrumentRepository.save(
                new Instrument("USDTRY", "US Dollar", InstrumentType.FX, Exchange.TCMB)
        );
    }
}
