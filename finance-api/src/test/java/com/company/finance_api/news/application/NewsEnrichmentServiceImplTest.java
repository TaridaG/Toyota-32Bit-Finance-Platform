package com.company.finance_api.news.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.application.InstrumentService;
import com.company.finance_api.pricing.infrastructure.persistence.InstrumentPriceRepository;
import com.company.finance_api.news.infrastructure.persistence.NewsFavoriteRepository;
import com.company.finance_api.shared.cache.JsonCacheService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsEnrichmentServiceImplTest {

  @Mock private InstrumentService instrumentService;
  @Mock private InstrumentPriceRepository instrumentPriceRepository;
  @Mock private NewsFavoriteRepository newsFavoriteRepository;
  @Mock private CurrentUserResolver currentUserResolver;
  @Mock private JsonCacheService jsonCacheService;

  private NewsEnrichmentServiceImpl newsEnrichmentService;

  @BeforeEach
  void setUp() {
    newsEnrichmentService =
        new NewsEnrichmentServiceImpl(
            instrumentService,
            instrumentPriceRepository,
            newsFavoriteRepository,
            currentUserResolver,
            jsonCacheService);
  }

  @Test
  void getEnrichedChartNews_returnsEmptyWhenSymbolMissing() {
    assertTrue(
        newsEnrichmentService
            .getEnrichedChartNews(null, "all", Instant.now().minusSeconds(60), Instant.now(), "en")
            .isEmpty());
  }

  @Test
  void getEnrichedChartNews_returnsEmptyWhenRangeInvalid() {
    Instant from = Instant.parse("2026-05-24T12:00:00Z");
    Instant to = Instant.parse("2026-05-24T10:00:00Z");

    assertTrue(
        newsEnrichmentService.getEnrichedChartNews("BTCUSDT", "all", from, to, "en").isEmpty());
  }

  @Test
  void getEnrichedFavoriteNews_returnsEmptyPageWhenNoFavorites() {
    UUID userId = UUID.randomUUID();
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(newsFavoriteRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());
    when(instrumentService.getAllActive()).thenReturn(List.of());

    var page = newsEnrichmentService.getEnrichedFavoriteNews(0, 10, "en", "all", null, null);

    assertEquals(0, page.totalElements());
    assertTrue(page.content().isEmpty());
  }
}
