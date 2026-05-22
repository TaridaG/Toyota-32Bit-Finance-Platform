package com.company.finance_api.service;

import com.company.finance_api.dto.ChartDrawingSaveDetailDto;
import com.company.finance_api.dto.ChartDrawingSavePageResponse;
import com.company.finance_api.dto.ChartDrawingSaveSummaryDto;
import com.company.finance_api.dto.CreateChartDrawingSaveRequest;

import java.util.List;

public interface ChartDrawingSaveService {

    ChartDrawingSaveDetailDto create(CreateChartDrawingSaveRequest request);

    List<ChartDrawingSaveSummaryDto> listForAsset(String assetKey);

    ChartDrawingSavePageResponse listPage(int page, int size);

    ChartDrawingSaveDetailDto getById(Long id);

    void delete(Long id);
}
