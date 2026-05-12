package com.company.finance_api.portfolio.external.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.User;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.domain.ExternalPositionLot;
import com.company.finance_api.portfolio.external.domain.ExternalPositionSourceType;
import com.company.finance_api.portfolio.external.dto.CreateExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.dto.CreateExternalPositionRequest;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioResponse;
import com.company.finance_api.portfolio.external.dto.PatchExternalPortfolioRequest;
import com.company.finance_api.exception.ResourceNotFoundException;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.external.repository.ExternalPositionLotRepository;
import com.company.finance_api.portfolio.external.service.ExternalPortfolioService;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.PortfolioSnapshotRepository;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
    private final TransactionRepository transactionRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;

    private static ExternalPortfolioResponse toResponse(ExternalPortfolio p) {
        return ExternalPortfolioResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .baseCurrency(p.getBaseCurrency())
                .createdAt(p.getCreatedAt() == null ? null : p.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .amountsHidden(p.isAmountsHidden())
                .build();
    }

    @Override
    public ExternalPortfolioResponse createPortfolio(UUID userId, CreateExternalPortfolioRequest request) {
        if (portfolioRepository.countByUserId(userId) >= MAX_PORTFOLIOS_PER_USER) {
            throw new IllegalStateException("Maximum portfolio limit reached (5)");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String rawBc = request.getBaseCurrency();
        String normalizedBc;
        if (rawBc == null || rawBc.isBlank()) {
            normalizedBc = "TRY";
        } else {
            normalizedBc = rawBc.trim().toUpperCase(Locale.ROOT);
            if (!"TRY".equals(normalizedBc) && !"USD".equals(normalizedBc)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baseCurrency must be TRY or USD");
            }
        }

        ExternalPortfolio portfolio = new ExternalPortfolio(user, request.getName(), normalizedBc);

        portfolioRepository.save(portfolio);

        return toResponse(portfolio);
    }

    @Override
    public List<ExternalPortfolioResponse> getUserPortfolios(UUID userId) {

        return portfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ExternalPortfolioServiceImpl::toResponse)
                .toList();
    }

    @Override
    public ExternalPortfolioResponse getPortfolio(UUID userId, Long portfolioId) {
        ExternalPortfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
        return toResponse(portfolio);
    }

    @Override
    public ExternalPortfolioResponse patchPortfolio(UUID userId, Long portfolioId, PatchExternalPortfolioRequest request) {
        ExternalPortfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
        portfolio.setAmountsHidden(Boolean.TRUE.equals(request.getAmountsHidden()));
        portfolioRepository.save(portfolio);
        return toResponse(portfolio);
    }

    /**
     * Permanently removes one external portfolio for {@code userId}, plus dependent rows in
     * {@code transactions}, {@code portfolio_snapshots}, and {@code external_position_lots} for that portfolio id.
     * Other portfolios for the same user are not modified.
     */
    @Override
    public void deletePortfolio(UUID userId, Long portfolioId) {
        ExternalPortfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
        transactionRepository.deleteAllByExternalPortfolioId(portfolioId);
        portfolioSnapshotRepository.deleteAllByExternalPortfolioId(portfolioId);
        lotRepository.deleteAllByPortfolioId(portfolioId);
        portfolioRepository.delete(portfolio);
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
