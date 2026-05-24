package com.company.marketdataservice.rates.application;

import com.company.marketdataservice.bootstrap.config.MarketEvdsProperties;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryResponseDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.RepoRateLatestDto;
import com.company.marketdataservice.rates.infrastructure.persistence.TcmbRepoRatePointEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TcmbRepoRatePointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepoRateHistoryServiceTest {

    @Mock
    private TcmbRepoRatePointRepository pointRepository;
    @Mock
    private RepoRateSyncService repoRateSyncService;
    @Mock
    private MarketEvdsProperties evdsProperties;

    private RepoRateHistoryService service;

    @BeforeEach
    void setUp() {
        service = new RepoRateHistoryService(pointRepository, repoRateSyncService, evdsProperties);
    }

    @Test
    void loadLatest_returnsValueAndOneDayBasisPointChange() {
        when(evdsProperties.getRepoRateSeries()).thenReturn("TP_BISPOLFAIZ_TUR");
        TcmbRepoRatePointEntity today = row(LocalDate.of(2026, 5, 23), "37.00");
        TcmbRepoRatePointEntity yesterday = row(LocalDate.of(2026, 5, 22), "36.50");

        when(pointRepository.findTopByOrderByObservationDateDesc()).thenReturn(Optional.of(today));
        when(pointRepository.findFirstByObservationDateLessThanOrderByObservationDateDesc(today.getObservationDate()))
                .thenReturn(Optional.of(yesterday));

        RepoRateLatestDto dto = service.loadLatest();

        verify(repoRateSyncService).requestInitialSyncIfEmptyAsync();
        assertThat(dto.getValue()).isEqualByComparingTo("37.00");
        assertThat(dto.getObservationDate()).isEqualTo(LocalDate.of(2026, 5, 23));
        assertThat(dto.getChange1dBasisPoints()).isEqualTo(50);
        assertThat(dto.getEvdsSeries()).isEqualTo("TP_BISPOLFAIZ_TUR");
    }

    @Test
    void loadFiveYearWeeklyFromDb_aggregatesDailyRowsToWeeklyPoints() {
        when(pointRepository.findByObservationDateBetweenOrderByObservationDateAsc(any(), any()))
                .thenReturn(List.of(
                        row(LocalDate.of(2026, 5, 18), "37.00"),
                        row(LocalDate.of(2026, 5, 19), "37.00"),
                        row(LocalDate.of(2026, 5, 20), "37.00"),
                        row(LocalDate.of(2026, 5, 21), "37.00"),
                        row(LocalDate.of(2026, 5, 22), "37.00"),
                        row(LocalDate.of(2026, 5, 23), "37.00")
                ));

        PolicyRateHistoryResponseDto dto = service.loadFiveYearWeeklyFromDb();

        assertThat(dto.getSymbol()).isEqualTo("TR_REPO_RATE");
        assertThat(dto.getFrequency()).isEqualTo("WEEKLY");
        assertThat(dto.getPoints()).isNotEmpty();
        assertThat(dto.getPoints().getLast().getValue()).isEqualByComparingTo("37.00");
    }

    private static TcmbRepoRatePointEntity row(LocalDate date, String rate) {
        TcmbRepoRatePointEntity entity = new TcmbRepoRatePointEntity();
        entity.setObservationDate(date);
        entity.setRatePercent(new BigDecimal(rate));
        return entity;
    }
}
