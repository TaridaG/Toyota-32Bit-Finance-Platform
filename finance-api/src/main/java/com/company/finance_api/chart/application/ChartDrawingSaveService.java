package com.company.finance_api.chart.application;

import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSaveDetailDto;
import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSavePageResponse;
import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSaveSummaryDto;
import com.company.finance_api.chart.infrastructure.http.dto.CreateChartDrawingSaveRequest;
import java.util.List;

/** ChartDrawingSaveService iş mantığını uygular (chart drawing save service). */
public interface ChartDrawingSaveService {

  /** Yeni chart drawing save kaydı oluşturur. */
  ChartDrawingSaveDetailDto create(CreateChartDrawingSaveRequest request);

  /** Belirtilen asset key için kayıtlı çizimleri listeler. */
  List<ChartDrawingSaveSummaryDto> listForAsset(String assetKey);

  /** Kullanıcının tüm chart drawing save kayıtlarını sayfalı döner. */
  ChartDrawingSavePageResponse listPage(int page, int size);

  /** Kimliğe göre drawing save detayını döner. */
  ChartDrawingSaveDetailDto getById(Long id);

  /** Drawing save kaydını siler. */
  void delete(Long id);
}
