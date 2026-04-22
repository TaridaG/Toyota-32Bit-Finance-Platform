package com.company.analytics.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class FinanceInstrumentClient {

    private final RestClient financeRestClient;

    @Value("${clients.finance.base-url:http://finance-api:8080}")
    private String financeBaseUrl;

    private final Map<String, Long> symbolToIdCache = new ConcurrentHashMap<>();

    public Optional<Long> resolveInstrumentId(String symbol) {
        Long cachedId = symbolToIdCache.get(symbol);
        if (cachedId != null) {
            return Optional.of(cachedId);
        }

        try {
            Map<String, Object> response = financeRestClient.get()
                    .uri(financeBaseUrl + "/api/instruments")
                    .header("X-USERNAME", "analytics-service")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Object data = response.get("data");
            if (!(data instanceof List<?> instruments)) {
                return Optional.empty();
            }

            for (Object item : instruments) {
                if (!(item instanceof Map<?, ?> raw)) {
                    continue;
                }
                Object symbolValue = raw.get("symbol");
                Object idValue = raw.get("id");

                if (!(symbolValue instanceof String symbolKey) || !(idValue instanceof Number idNumber)) {
                    continue;
                }

                symbolToIdCache.put(symbolKey, idNumber.longValue());
            }

            return Optional.ofNullable(symbolToIdCache.get(symbol));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
