package com.company.reporting.client;

import com.company.reporting.dto.FinancePortfolioAssetDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FinanceServiceClient {

    private final RestClient analyticsRestClient;

    @Value("${clients.finance.base-url}")
    private String financeBaseUrl;

    @Value("${clients.finance.auth.mode:header}")
    private String financeAuthMode;

    @Value("${clients.finance.auth.header-username:reporting-service}")
    private String financeHeaderUsername;

    @Value("${clients.finance.auth.bearer-token:}")
    private String financeBearerToken;

    public List<FinancePortfolioAssetDto> getPortfolioAssets() {
        RequestHeadersSpec<?> request = analyticsRestClient.get()
                .uri(financeBaseUrl + "/api/portfolio");
        Map<String, Object> response = applyAuth(request)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        return JsonMapperUtil.convertList(response.get("data"), FinancePortfolioAssetDto.class);
    }

    private RequestHeadersSpec<?> applyAuth(RequestHeadersSpec<?> request) {
        String mode = financeAuthMode == null ? "header" : financeAuthMode.trim().toLowerCase();
        if ("bearer".equals(mode) && StringUtils.hasText(financeBearerToken)) {
            return request
                    .header("Authorization", "Bearer " + financeBearerToken.trim())
                    .header("X-USERNAME", financeHeaderUsername);
        }
        if ("none".equals(mode)) {
            return request;
        }
        return request.header("X-USERNAME", financeHeaderUsername);
    }
}