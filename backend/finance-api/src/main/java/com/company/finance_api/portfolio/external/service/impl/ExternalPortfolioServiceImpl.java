package com.company.finance_api.portfolio.external.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.User;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.domain.ExternalPositionLot;
import com.company.finance_api.portfolio.external.domain.ExternalPositionSourceType;
import com.company.finance_api.portfolio.external.dto.CreateExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.dto.CreateExternalPositionRequest;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioResponse;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.external.repository.ExternalPositionLotRepository;
import com.company.finance_api.portfolio.external.service.ExternalPortfolioService;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExternalPortfolioServiceImpl implements ExternalPortfolioService {
    private static final int MAX_PORTFOLIOS_PER_USER = 5;

    private final ExternalPortfolioRepository portfolioRepository;
    private final ExternalPositionLotRepository lotRepository;
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;

    @Override
    public ExternalPortfolioResponse createPortfolio(UUID userId, CreateExternalPortfolioRequest request) {
        if (portfolioRepository.countByUserId(userId) >= MAX_PORTFOLIOS_PER_USER) {
            throw new IllegalStateException("Maximum portfolio limit reached (5)");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExternalPortfolio portfolio = new ExternalPortfolio(
                user,
                request.getName(),
                request.getBaseCurrency()
        );

        portfolioRepository.save(portfolio);

        return ExternalPortfolioResponse.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .baseCurrency(portfolio.getBaseCurrency())
                .build();
    }

    @Override
    public List<ExternalPortfolioResponse> getUserPortfolios(UUID userId) {

        return portfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(p -> ExternalPortfolioResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .baseCurrency(p.getBaseCurrency())
                        .build())
                .toList();
    }

    @Override
    public void addPosition(UUID userId, Long portfolioId, CreateExternalPositionRequest request) {

        ExternalPortfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new RuntimeException("Instrument not found"));

        ExternalPositionLot lot = new ExternalPositionLot(
                portfolio,
                instrument,
                request.getQuantity(),
                request.getUnitPrice(),
                request.getFeeAmount(),
                request.getFeeCurrency(),
                request.getAcquiredAt(),
                ExternalPositionSourceType.MANUAL,
                request.getSourceName(),
                request.getNotes()
        );

        lotRepository.save(lot);
    }

    @Override
    public void deletePosition(UUID userId, Long portfolioId, Long positionId) {

        ExternalPositionLot lot = lotRepository
                .findByIdAndPortfolioIdAndDeletedFalse(positionId, portfolioId)
                .orElseThrow(() -> new RuntimeException("Position not found"));

        if (!lot.getPortfolio().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        lot.softDelete();
    }
}

