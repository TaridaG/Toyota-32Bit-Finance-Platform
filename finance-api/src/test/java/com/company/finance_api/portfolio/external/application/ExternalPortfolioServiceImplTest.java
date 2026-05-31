package com.company.finance_api.portfolio.external.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.portfolio.external.application.ExternalPortfolioServiceImpl;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.infrastructure.http.dto.CreateExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.infrastructure.http.dto.PatchExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPositionLotRepository;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import com.company.finance_api.portfolio.infrastructure.persistence.PortfolioSnapshotRepository;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionRepository;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ExternalPortfolioServiceImplTest {

  @Mock private ExternalPortfolioRepository portfolioRepository;
  @Mock private ExternalPositionLotRepository lotRepository;
  @Mock private UserRepository userRepository;
  @Mock private InstrumentRepository instrumentRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private PortfolioSnapshotRepository portfolioSnapshotRepository;

  private ExternalPortfolioServiceImpl service;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new ExternalPortfolioServiceImpl(
            portfolioRepository,
            lotRepository,
            userRepository,
            instrumentRepository,
            transactionRepository,
            portfolioSnapshotRepository);
  }

  @Test
  void createPortfolio_rejectsWhenLimitReached() {
    when(portfolioRepository.countByUserId(userId)).thenReturn(5L);

    CreateExternalPortfolioRequest request = new CreateExternalPortfolioRequest();
    request.setName("Extra");

    assertThrows(IllegalStateException.class, () -> service.createPortfolio(userId, request));
    verify(portfolioRepository, never()).save(any());
  }

  @Test
  void createPortfolio_normalizesBlankBaseCurrencyToMixed() {
    User user = new User("user@example.com", "trader");
    CreateExternalPortfolioRequest request = new CreateExternalPortfolioRequest();
    request.setName("Main");

    when(portfolioRepository.countByUserId(userId)).thenReturn(0L);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    var response = service.createPortfolio(userId, request);

    ArgumentCaptor<ExternalPortfolio> captor = ArgumentCaptor.forClass(ExternalPortfolio.class);
    verify(portfolioRepository).save(captor.capture());
    assertEquals("MIXED", captor.getValue().getBaseCurrency());
    assertEquals("MIXED", response.getBaseCurrency());
  }

  @Test
  void createPortfolio_rejectsUnsupportedBaseCurrency() {
    User user = new User("user@example.com", "trader");
    CreateExternalPortfolioRequest request = new CreateExternalPortfolioRequest();
    request.setName("Main");
    request.setBaseCurrency("EUR");

    when(portfolioRepository.countByUserId(userId)).thenReturn(0L);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThrows(ResponseStatusException.class, () -> service.createPortfolio(userId, request));
  }

  @Test
  void getUserPortfolios_mapsResponses() {
    User user = new User("user@example.com", "trader");
    ExternalPortfolio portfolio = new ExternalPortfolio(user, "Main", "USD");
    when(portfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
        .thenReturn(List.of(portfolio));

    var responses = service.getUserPortfolios(userId);

    assertEquals(1, responses.size());
    assertEquals("Main", responses.get(0).getName());
  }

  @Test
  void patchPortfolio_updatesAmountsHidden() {
    User user = new User("user@example.com", "trader");
    ExternalPortfolio portfolio = new ExternalPortfolio(user, "Main", "USD");
    when(portfolioRepository.findByIdAndUserId(2L, userId)).thenReturn(Optional.of(portfolio));

    PatchExternalPortfolioRequest request = new PatchExternalPortfolioRequest();
    request.setAmountsHidden(true);

    var response = service.patchPortfolio(userId, 2L, request);

    assertTrue(portfolio.isAmountsHidden());
    assertTrue(response.isAmountsHidden());
    verify(portfolioRepository).save(portfolio);
  }

  @Test
  void deletePortfolio_cascadesRelatedData() {
    User user = new User("user@example.com", "trader");
    ExternalPortfolio portfolio = new ExternalPortfolio(user, "Main", "USD");
    when(portfolioRepository.findByIdAndUserId(4L, userId)).thenReturn(Optional.of(portfolio));

    service.deletePortfolio(userId, 4L);

    verify(transactionRepository).deleteAllByExternalPortfolioId(4L);
    verify(portfolioSnapshotRepository).deleteAllByExternalPortfolioId(4L);
    verify(lotRepository).deleteAllByPortfolioId(4L);
    verify(portfolioRepository).delete(portfolio);
  }

  @Test
  void getPortfolio_throwsWhenMissing() {
    when(portfolioRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getPortfolio(userId, 99L));
  }

  @Test
  void addPosition_persistsLot() {
    User user = new User("user@example.com", "trader");
    ExternalPortfolio portfolio = new ExternalPortfolio(user, "Main", "USD");
    Instrument instrument =
        new Instrument("ASELS", "Aselsan", InstrumentType.STOCK, Exchange.BIST);
    var request = new com.company.finance_api.portfolio.external.infrastructure.http.dto.CreateExternalPositionRequest();
    request.setInstrumentId(10L);
    request.setQuantity(new java.math.BigDecimal("5"));
    request.setUnitPrice(new java.math.BigDecimal("100"));
    request.setAcquiredAt(java.time.LocalDateTime.parse("2026-05-20T09:00:00"));

    when(portfolioRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(portfolio));
    when(instrumentRepository.findById(10L)).thenReturn(Optional.of(instrument));

    service.addPosition(userId, 1L, request);

    verify(lotRepository).save(any());
  }
}
