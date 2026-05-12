package com.company.finance_api.admin;

import com.company.finance_api.admin.dto.AdminHoldingLineDto;
import com.company.finance_api.admin.dto.AdminPortfolioDetailDto;
import com.company.finance_api.admin.dto.AdminUserPortfolioTreeDto;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.User;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioSummaryResponse;
import com.company.finance_api.portfolio.external.dto.ExternalPositionSummary;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.external.service.ExternalPortfolioValuationService;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sensitive portfolio internals for the admin portal only. Called exclusively from
 * {@code /api/admin/metrics/**} (same security boundary as other admin metrics).
 */
@Service
public class AdminUserPortfolioDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserPortfolioDetailsService.class);
    private static final DateTimeFormatter CREATED_AT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserRepository userRepository;
    private final ExternalPortfolioRepository portfolioRepository;
    private final ExternalPortfolioValuationService valuationService;
    private final InstrumentRepository instrumentRepository;

    public AdminUserPortfolioDetailsService(
            UserRepository userRepository,
            ExternalPortfolioRepository portfolioRepository,
            ExternalPortfolioValuationService valuationService,
            InstrumentRepository instrumentRepository) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.valuationService = valuationService;
        this.instrumentRepository = instrumentRepository;
    }

    @Transactional(readOnly = true)
    public AdminUserPortfolioTreeDto loadTree(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getDeletionRequestedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not in roster");
        }

        List<ExternalPortfolio> portfolios = portfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        List<AdminPortfolioDetailDto> rows = new ArrayList<>();
        for (ExternalPortfolio p : portfolios) {
            List<AdminHoldingLineDto> holdings = new ArrayList<>();
            try {
                ExternalPortfolioSummaryResponse summary =
                        valuationService.calculateSummary(userId, p.getId());
                BigDecimal totalMarket = summary.getTotalMarketValue();
                if (summary.getPositions() != null) {
                    for (ExternalPositionSummary pos : summary.getPositions()) {
                        String name = instrumentRepository.findById(pos.getInstrumentId())
                                .map(Instrument::getName)
                                .orElse("");
                        BigDecimal pct = totalMarket == null || totalMarket.compareTo(BigDecimal.ZERO) == 0
                                ? BigDecimal.ZERO
                                : pos.getMarketValue()
                                        .divide(totalMarket, 6, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100));
                        holdings.add(new AdminHoldingLineDto(
                                pos.getSymbol(),
                                name,
                                pos.getMarketValue(),
                                pct));
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("Admin portfolio valuation failed user={} portfolioId={}", userId, p.getId(), ex);
            }
            rows.add(new AdminPortfolioDetailDto(
                    p.getId(),
                    p.getName(),
                    CREATED_AT_FMT.format(p.getCreatedAt()),
                    p.getBaseCurrency(),
                    holdings));
        }
        return new AdminUserPortfolioTreeDto(rows);
    }
}
