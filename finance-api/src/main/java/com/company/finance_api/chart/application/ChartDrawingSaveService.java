package com.company.finance_api.chart.application;

import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSaveDetailDto;
import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSavePageResponse;
import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSaveSummaryDto;
import com.company.finance_api.chart.infrastructure.http.dto.CreateChartDrawingSaveRequest;
import java.util.List;

/** ChartDrawingSaveService iş mantığını uygular (chart drawing save service). */
public interface ChartDrawingSaveService {

  /** create sözleşmesi. */
  ChartDrawingSaveDetailDto create(CreateChartDrawingSaveRequest request);

  /** listForAsset sözleşmesi. */
  List<ChartDrawingSaveSummaryDto> listForAsset(String assetKey);

  /** listPage sözleşmesi. */
  ChartDrawingSavePageResponse listPage(int page, int size);

  /** getById sözleşmesi. */
  ChartDrawingSaveDetailDto getById(Long id);

  /** delete sözleşmesi. */
  void delete(Long id);
}
