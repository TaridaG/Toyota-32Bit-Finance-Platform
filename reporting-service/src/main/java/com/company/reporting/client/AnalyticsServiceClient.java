package com.company.reporting.client;

import com.company.reporting.dto.AnalyticsCandleDto;
import com.company.reporting.dto.AnalyticsMovingAverageDto;
import com.company.reporting.dto.AnalyticsTrendMetricDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AnalyticsServiceClient {

    private final RestClient analyticsRestClient;

    @Value("${clients.analytics.base-url}")
    private String analyticsBaseUrl;

    public List<AnalyticsCandleDto> getCandles(String symbol, LocalDate from, LocalDate to) {
        Map<String, Object> response = analyticsRestClient.get()
                .uri(analyticsBaseUrl + "/api/analytics/instruments/{symbol}/candles?from={from}&to={to}",
                        symbol, from, to)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        return JsonMapperUtil.convertList(response.get("data"), AnalyticsCandleDto.class);
    }

    public List<AnalyticsMovingAverageDto> getMovingAverages(String symbol) {
        Map<String, Object> response = analyticsRestClient.get()
                .uri(analyticsBaseUrl + "/api/analytics/instruments/{symbol}/moving-average", symbol)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        return JsonMapperUtil.convertList(response.get("data"), AnalyticsMovingAverageDto.class);
    }

    public List<AnalyticsTrendMetricDto> getTrendMetrics(String symbol) {
        Map<String, Object> response = analyticsRestClient.get()
                .uri(analyticsBaseUrl + "/api/analytics/instruments/{symbol}/trend", symbol)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        return JsonMapperUtil.convertList(response.get("data"), AnalyticsTrendMetricDto.class);
    }
}