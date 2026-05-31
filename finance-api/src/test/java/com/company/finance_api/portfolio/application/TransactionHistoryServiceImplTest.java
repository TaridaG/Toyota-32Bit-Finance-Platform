package com.company.finance_api.portfolio.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.portfolio.domain.enums.TransactionType;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.infrastructure.http.dto.TransactionHistoryResponse;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionRepository;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionHistoryServiceImplTest {

  @Mock private TransactionRepository transactionRepository;
  @Mock private CurrentUserResolver currentUserResolver;
  @Mock private UserRepository userRepository;
  @Mock private ExternalPortfolioRepository externalPortfolioRepository;

  @InjectMocks private TransactionHistoryServiceImpl service;

  @Test
  void getMyHistory_shouldReturnEmpty_whenPortfolioNotOwned() {
    UUID userId = UUID.randomUUID();
    User user = new User("trader@example.com", "trader");
    ReflectionTestUtils.setField(user, "id", userId);

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(externalPortfolioRepository.findByIdAndUserId(55L, userId)).thenReturn(Optional.empty());

    List<TransactionHistoryResponse> history = service.getMyHistory(55L);

    assertTrue(history.isEmpty());
  }

  @Test
  void getMyHistory_shouldMapTransactionsForOwnedPortfolio() {
    UUID userId = UUID.randomUUID();
    User user = new User("trader@example.com", "trader");
    ReflectionTestUtils.setField(user, "id", userId);
    ExternalPortfolio portfolio = new ExternalPortfolio(user, "Growth", "USD");
    ReflectionTestUtils.setField(portfolio, "id", 10L);
    Instrument instrument =
        new Instrument("AAPL", "Apple", InstrumentType.STOCK, Exchange.NASDAQ);
    ReflectionTestUtils.setField(instrument, "id", 1L);
    Transaction tx =
        Transaction.buy(user, instrument, BigDecimal.TEN, BigDecimal.ONE);
    ReflectionTestUtils.setField(tx, "id", 99L);
    ReflectionTestUtils.setField(tx, "createdAt", Instant.parse("2026-01-15T12:00:00Z"));

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(externalPortfolioRepository.findByIdAndUserId(10L, userId))
        .thenReturn(Optional.of(portfolio));
    when(transactionRepository.findByUserAndExternalPortfolioOrderByCreatedAtDesc(user, portfolio))
        .thenReturn(List.of(tx));

    List<TransactionHistoryResponse> history = service.getMyHistory(10L);

    assertEquals(1, history.size());
    assertEquals(TransactionType.BUY.name(), history.get(0).type());
    assertEquals("AAPL", history.get(0).instrumentSymbol());
  }
}
