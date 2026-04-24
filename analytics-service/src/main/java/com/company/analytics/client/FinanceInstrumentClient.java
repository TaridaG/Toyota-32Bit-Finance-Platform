package com.company.analytics.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class FinanceInstrumentClient {

    private final RestClient financeRestClient;

    @Value("${clients.finance.base-url:http://finance-api:8080}")
    private String financeBaseUrl;

    @Value("${clients.finance.catalog-ttl-ms:90000}")
    private long catalogTtlMs;

    private final Map<String, Long> symbolToIdCache = new ConcurrentHashMap<>();
    private final Map<Long, String> idToSymbolCache = new ConcurrentHashMap<>();
    private final Set<Long> unknownInstrumentIds = ConcurrentHashMap.newKeySet();

    private volatile long catalogValidUntilEpochMs;

    public Optional<Long> resolveInstrumentId(String symbol) {
        Long cachedId = symbolToIdCache.get(symbol);
        if (cachedId != null) {
            return Optional.of(cachedId);
        }
        if (!isCatalogFresh()) {
            loadCatalogFromFinanceApi();
        }
        return Optional.ofNullable(symbolToIdCache.get(symbol));
    }

    public Optional<String> getSymbolForInstrumentId(long instrumentId) {
        String sym = idToSymbolCache.get(instrumentId);
        if (sym != null) {
            return Optional.of(sym);
        }
        if (unknownInstrumentIds.contains(instrumentId)) {
            return Optional.empty();
        }
        synchronized (this) {
            sym = idToSymbolCache.get(instrumentId);
            if (sym != null) {
                return Optional.of(sym);
            }
            boolean loaded = false;
            if (!isCatalogFresh()) {
                loaded = loadCatalogFromFinanceApi();
            }
            sym = idToSymbolCache.get(instrumentId);
            if (sym != null) {
                return Optional.of(sym);
            }
            if (loaded) {
                unknownInstrumentIds.add(instrumentId);
            }
            return Optional.empty();
        }
    }

    private boolean isCatalogFresh() {
        return System.currentTimeMillis() < catalogValidUntilEpochMs;
    }

    private boolean loadCatalogFromFinanceApi() {
        try {
            Map<String, Object> response = financeRestClient.get()
                    .uri(financeBaseUrl + "/api/instruments")
                    .header("X-USERNAME", "analytics-service")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Object data = response.get("data");
            if (!(data instanceof List<?> instruments)) {
                return false;
            }

            symbolToIdCache.clear();
            idToSymbolCache.clear();
            unknownInstrumentIds.clear();

            for (Object item : instruments) {
                if (!(item instanceof Map<?, ?> raw)) {
                    continue;
                }
                Object symbolValue = raw.get("symbol");
                Object idValue = raw.get("id");

                if (!(symbolValue instanceof String symbolKey) || !(idValue instanceof Number idNumber)) {
                    continue;
                }

                long id = idNumber.longValue();
                symbolToIdCache.put(symbolKey, id);
                idToSymbolCache.put(id, symbolKey);
            }
            catalogValidUntilEpochMs = System.currentTimeMillis() + Math.max(60_000L, catalogTtlMs);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
