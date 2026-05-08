package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.enums.PurchaseMode;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.dto.TransactionHistoryPageResponse;
import com.company.finance_api.dto.TransactionHistoryResponse;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private final TransactionRepository transactionRepository;
    private final CurrentUserResolver currentUserResolver;
    private final UserRepository userRepository;
    private final ExternalPortfolioRepository externalPortfolioRepository;

    @Override
    public List<TransactionHistoryResponse> getMyHistory(Long portfolioId) {
        var user = resolveCurrentUser();
        if (portfolioId != null) {
            var portfolio = externalPortfolioRepository.findByIdAndUserId(portfolioId, user.getId())
                    .orElse(null);
            if (portfolio == null) {
                return List.of();
            }
            return transactionRepository
                    .findByUserAndExternalPortfolioOrderByCreatedAtDesc(user, portfolio)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        return transactionRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TransactionHistoryPageResponse getMyHistoryPage(
            int page,
            int size,
            Long portfolioId,
            String symbol,
            String type,
            String purchaseMode,
            String inputCurrency,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        var user = resolveCurrentUser();
        if (portfolioId != null && externalPortfolioRepository.findByIdAndUserId(portfolioId, user.getId()).isEmpty()) {
            return new TransactionHistoryPageResponse(List.of(), Math.max(page, 0), Math.max(size, 1), 0, 0);
        }
        var pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Transaction> spec = (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("instrument", JoinType.LEFT);
                query.distinct(true);
            }

            var predicate = cb.equal(root.get("user"), user);
            if (portfolioId != null) {
                predicate = cb.and(predicate, cb.equal(root.join("externalPortfolio", JoinType.LEFT).get("id"), portfolioId));
            }

            if (symbol != null && !symbol.isBlank()) {
                predicate = cb.and(predicate, cb.like(
                        cb.lower(root.join("instrument", JoinType.LEFT).get("symbol")),
                        "%" + symbol.toLowerCase(Locale.ROOT) + "%"
                ));
            }
            if (type != null && !type.isBlank()) {
                var parsedType = safeTransactionType(type);
                if (parsedType != null) {
                    predicate = cb.and(predicate, cb.equal(root.get("type"), parsedType));
                }
            }
            if (purchaseMode != null && !purchaseMode.isBlank()) {
                var parsedPurchaseMode = safePurchaseMode(purchaseMode);
                if (parsedPurchaseMode != null) {
                    predicate = cb.and(predicate, cb.equal(root.get("purchaseMode"), parsedPurchaseMode));
                }
            }
            if (inputCurrency != null && !inputCurrency.isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.upper(root.get("inputCurrency")), inputCurrency.toUpperCase(Locale.ROOT)));
            }
            if (fromDate != null) {
                Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (toDate != null) {
                Instant to = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
                predicate = cb.and(predicate, cb.lessThan(root.get("createdAt"), to));
            }
            return predicate;
        };

        var result = transactionRepository.findAll(spec, pageable);
        return new TransactionHistoryPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private com.company.finance_api.domain.User resolveCurrentUser() {
        UUID userId = currentUserResolver.getCurrentUserId();
        return userRepository.findById(userId).orElseThrow();
    }

    private TransactionHistoryResponse toResponse(Transaction tx) {
        return new TransactionHistoryResponse(
                tx.getId(),
                tx.getExternalPortfolio() == null ? null : tx.getExternalPortfolio().getId(),
                tx.getExternalPortfolio() == null ? null : tx.getExternalPortfolio().getName(),
                tx.getInstrument().getSymbol(),
                tx.getType().name(),
                tx.getPurchaseMode() == null ? null : tx.getPurchaseMode().name(),
                tx.getSourceLabel(),
                tx.getQuantity(),
                tx.getPrice(),
                tx.getTotalAmount(),
                tx.getInputCurrency(),
                tx.getInputAmount(),
                tx.getFxRateUsed(),
                tx.getAcquiredAt(),
                tx.getCreatedAt()
        );
    }

    private TransactionType safeTransactionType(String raw) {
        try {
            return TransactionType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private PurchaseMode safePurchaseMode(String raw) {
        try {
            return PurchaseMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}