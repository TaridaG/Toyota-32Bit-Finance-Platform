package com.company.reporting.client;

import com.company.reporting.dto.FinancePortfolioAssetDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FinanceServiceClient {

    private final RestClient analyticsRestClient;

    @Value("${clients.finance.base-url}")
    private String financeBaseUrl;

    public List<FinancePortfolioAssetDto> getPortfolioAssets() {
        Map<String, Object> response = analyticsRestClient.get()
                .uri(financeBaseUrl + "/api/portfolio")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        return JsonMapperUtil.convertList(response.get("data"), FinancePortfolioAssetDto.class);
    }
}