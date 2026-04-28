package com.company.notification.client;

import com.company.notification.config.FinanceApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceInstrumentLookupService {

    private final RestTemplate restTemplate;
    private final FinanceApiProperties financeApiProperties;
    private final ObjectMapper objectMapper;

    public Optional<Long> resolveInstrumentId(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }
        String needle = symbol.trim().toUpperCase(Locale.ROOT);
        String base = financeApiProperties.getBaseUrl() == null ? "" : financeApiProperties.getBaseUrl().trim();
        if (base.isEmpty()) {
            return Optional.empty();
        }
        String url = base.endsWith("/") ? base + "api/instruments" : base + "/api/instruments";
        try {
            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                return Optional.empty();
            }
            for (JsonNode row : data) {
                if (row == null || !row.isObject()) {
                    continue;
                }
                JsonNode sym = row.get("symbol");
                if (sym != null && sym.isTextual() && needle.equals(sym.asText("").trim().toUpperCase(Locale.ROOT))) {
                    JsonNode id = row.get("id");
                    if (id != null && id.isIntegralNumber()) {
                        return Optional.of(id.longValue());
                    }
                }
            }
        } catch (RestClientException ex) {
            log.warn("FINANCE_INSTRUMENT_LOOKUP_FAILED symbol={} reason={}", symbol, ex.toString());
        } catch (Exception ex) {
            log.warn("FINANCE_INSTRUMENT_LOOKUP_PARSE_FAILED symbol={} reason={}", symbol, ex.toString());
        }
        return Optional.empty();
    }
}
