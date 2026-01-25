package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.service.InstrumentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstrumentServiceImpl implements InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentServiceImpl(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    @Override
    public List<Instrument> getAllActive() {
        return instrumentRepository.findByActiveTrue();
    }

    @Override
    public List<Instrument> getByType(InstrumentType type) {
        return instrumentRepository.findByTypeAndActiveTrue(type);
    }
}
