package com.company.marketdataservice.fund;

import com.company.marketdataservice.config.FundMarketProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TefasFundPriceProvider implements FundProvider {

    private static final String SRC = "TEFAS";
    private static final Duration BLOCK = Duration.ofSeconds(25);
    private static final DateTimeFormatter TEFAS_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd.MM.uuuu");
    private static final ZoneId TURKEY = ZoneId.of("Europe/Istanbul");

    private final FundMarketProperties fundMarketProperties;
    private final ObjectMapper objectMapper;

    @Qualifier("tefasWebClient")
    private final WebClient tefasWebClient;

    @Override
    public String source() {
        return SRC;
    }

    @Override
    public List<FundSnapshot> fetchLatestNavs(List<String> fundCodes) {
        if (fundCodes == null || fundCodes.isEmpty()) {
            return List.of();
        }
        List<FundSnapshot> out = new ArrayList<>();
        for (String raw : fundCodes) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String code = raw.trim().toUpperCase(Locale.ROOT);
            try {
                Optional<FundSnapshot> snap = fetchOne(code);
                if (snap.isEmpty()) {
                    continue;
                }
                out.add(snap.get());
            } catch (Exception ex) {
                log.warn(
                        "TEFAS_BIND_HISTORY_FAILED fundCode={} reason={}",
                        code,
                        ex.toString(),
                        ex
                );
            }
        }
        return out;
    }

    private Optional<FundSnapshot> fetchOne(String normalizedCode) throws Exception {
        String url = fundMarketProperties.getTefasBindHistoryUrl();
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }

        LocalDate overallEnd = LocalDate.now(TURKEY);
        LocalDate overallStart = overallEnd.minusDays(fundMarketProperties.getTefasHistoryLookbackDays());

        int maxInclusiveDays = Math.min(89, Math.max(1, fundMarketProperties.getTefasHistoryChunkInclusiveDays()));

        ArrayNode merged = objectMapper.createArrayNode();

        LocalDate chunkStart = overallStart;
        while (!chunkStart.isAfter(overallEnd)) {
            LocalDate chunkEnd = chunkStart.plusDays(maxInclusiveDays - 1L);
            if (chunkEnd.isAfter(overallEnd)) {
                chunkEnd = overallEnd;
            }
            MultiValueMap<String, String> form = bindHistoryForm(normalizedCode, chunkStart, chunkEnd);
            String body = postBindHistory(form, url);
            if (body != null && !body.isBlank()) {
                JsonNode root = objectMapper.readTree(body);
                JsonNode chunkData = root.get("data");
                if (chunkData != null && chunkData.isArray()) {
                    for (JsonNode row : chunkData) {
                        merged.add(row);
                    }
                }
            }
            chunkStart = chunkEnd.plusDays(1L);
        }

        if (merged.isEmpty()) {
            return Optional.empty();
        }
        ObjectNode synthetic = objectMapper.createObjectNode();
        synthetic.set("data", merged);
        Optional<TefasBindHistoryParsing.ParsedLatest> parsed =
                TefasBindHistoryParsing.latestNav(synthetic, normalizedCode);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        TefasBindHistoryParsing.ParsedLatest p = parsed.get();
        return Optional.of(new FundSnapshot(null, p.fundCode(), p.nav(), p.timestamp(), SRC));
    }

    private String postBindHistory(MultiValueMap<String, String> form, String url) {
        return tefasWebClient
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .header(
                        "User-Agent",
                        "Mozilla/5.0 (compatible; MarketDataService/1.0; +https://localhost)"
                )
                .header("Origin", "https://www.tefas.gov.tr")
                .header("Referer", "https://www.tefas.gov.tr/")
                .header("X-Requested-With", "XMLHttpRequest")
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(String.class)
                .block(BLOCK);
    }

    private MultiValueMap<String, String> bindHistoryForm(
            String normalizedCode,
            LocalDate chunkStart,
            LocalDate chunkEnd
    ) {
        MultiValueMap<String, String> m = new LinkedMultiValueMap<>();
        m.add("fontip", fundMarketProperties.getTefasFontip());
        m.add("fonkod", normalizedCode);
        m.add("bastarih", TEFAS_DD_MM_YYYY.format(chunkStart));
        m.add("bittarih", TEFAS_DD_MM_YYYY.format(chunkEnd));
        return m;
    }
}
