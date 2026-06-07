package com.company.marketdataservice.fund.infrastructure.provider;
import com.company.marketdataservice.bootstrap.config.FundMarketProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;


/**
 * `fon (TEFAS NAV)` harici HTTP/API client adaptörü.
 */
@Component
@RequiredArgsConstructor
public class TefasBindHistoryClient {

    private static final Logger log = LoggerFactory.getLogger(TefasBindHistoryClient.class);
    private static final Duration BLOCK = Duration.ofSeconds(25);
    private static final DateTimeFormatter TEFAS_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd.MM.uuuu");
    private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final ZoneId TURKEY = ZoneId.of("Europe/Istanbul");

    private final FundMarketProperties fundMarketProperties;
    private final ObjectMapper objectMapper;

    @Qualifier("tefasWebClient")
    private final WebClient tefasWebClient;

    /**
     * Merges daily rows into one synthetic {@code {"data":[...]}} tree compatible with {@link TefasBindHistoryParsing}.
     */
    public JsonNode fetchMergedBindHistory(String rawFundCode, LocalDate startInclusive, LocalDate endInclusive) {
        String normalizedCode = normalizeCode(rawFundCode);
        if (normalizedCode.isEmpty()
                || startInclusive == null
                || endInclusive == null
                || startInclusive.isAfter(endInclusive)) {
            return emptyDataRoot();
        }
        if (StringUtils.hasText(fundMarketProperties.getTefasFonGnlBlgUrl())) {
            JsonNode modern = fetchMergedViaFonGnlJson(normalizedCode, startInclusive, endInclusive);
            if (modern.path("data").isArray() && !modern.path("data").isEmpty()) {
                return modern;
            }
            log.debug(
                    "TEFAS_FON_GNL_EMPTY fundCode={} window={}..{}",
                    normalizedCode,
                    startInclusive,
                    endInclusive
            );
        }
        return fetchMergedViaLegacyForm(normalizedCode, startInclusive, endInclusive);
    }

    /**
     * TEFAS iş günü ve tarih penceresi hesapları için Türkiye saat diliminde
     * ({@code Europe/Istanbul}) bugünün {@link LocalDate} değerini döner.
     */
    public LocalDate todayTurkey() {
        return LocalDate.now(TURKEY);
    }

    private static String normalizeCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private JsonNode fetchMergedViaFonGnlJson(String normalizedCode, LocalDate startInclusive, LocalDate endInclusive) {
        String url = fundMarketProperties.getTefasFonGnlBlgUrl().trim();
        int chunkDays = Math.min(28, Math.max(1, fundMarketProperties.getTefasFonGnlChunkDays()));
        ArrayNode merged = objectMapper.createArrayNode();
        LocalDate chunkStart = startInclusive;
        while (!chunkStart.isAfter(endInclusive)) {
            LocalDate chunkEnd = chunkStart.plusDays(chunkDays - 1L);
            if (chunkEnd.isAfter(endInclusive)) {
                chunkEnd = endInclusive;
            }
            try {
                ObjectNode body = fonGnlRequestBody(normalizedCode, chunkStart, chunkEnd);
                JsonNode root = postJson(url, body);
                appendFonGnlRows(merged, root);
            } catch (Exception ex) {
                log.warn(
                        "TEFAS_FON_GNL_CHUNK_FAILED fundCode={} {}..{} reason={}",
                        normalizedCode,
                        chunkStart,
                        chunkEnd,
                        ex.toString()
                );
            }
            pauseBetweenFonGnlChunks();
            chunkStart = chunkEnd.plusDays(1L);
        }
        ObjectNode synthetic = objectMapper.createObjectNode();
        synthetic.set("data", merged);
        log.info("TEFAS_FON_GNL_MERGED fundCode={} rows={}", normalizedCode, merged.size());
        return synthetic;
    }

    private ObjectNode fonGnlRequestBody(String fonKodu, LocalDate bas, LocalDate bit) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("fonTipi", fundMarketProperties.getTefasFontip());
        body.put("fonKodu", fonKodu);
        body.putNull("aramaMetni");
        body.putNull("fonTurKod");
        body.putNull("fonGrubu");
        body.putNull("sfonTurKod");
        body.putNull("fonTurAciklama");
        body.putNull("kurucuKod");
        body.put("basTarih", bas.format(BASIC_ISO_DATE));
        body.put("bitTarih", bit.format(BASIC_ISO_DATE));
        body.put("basSira", 1);
        body.put("bitSira", 100_000);
        body.put("dil", "TR");
        body.put("sFonTurKod", "");
        body.put("fonKod", "");
        body.put("fonGrup", "");
        body.put("fonUnvanTip", "");
        return body;
    }

    private JsonNode postJson(String url, ObjectNode body) throws Exception {
        // Send raw JSON: WebClient's Jackson encoder may apply NON_NULL globally and drop keys TEFAS expects → 400.
        String json = objectMapper.writeValueAsString(body);
        String raw =
                tefasWebClient
                        .post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                        .header(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                        + "(KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"
                        )
                        .header("Origin", "https://www.tefas.gov.tr")
                        .header("Referer", "https://www.tefas.gov.tr/tr/fon-verileri")
                        .bodyValue(json)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(BLOCK);
        if (raw == null || raw.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(raw);
    }

    private void appendFonGnlRows(ArrayNode merged, JsonNode root) {
        if (root == null || root.isNull()) {
            return;
        }
        JsonNode err = root.get("errorMessage");
        if (err != null && err.isTextual()) {
            String msg = err.asText("").trim();
            if (!msg.isEmpty()) {
                String lower = msg.toLowerCase(Locale.ROOT);
                if (!lower.contains("out of bounds") && !lower.contains("veri bulunamadı")) {
                    log.warn("TEFAS_FON_GNL_API_MSG errorMessage={}", msg);
                }
            }
        }
        JsonNode list = root.get("resultList");
        if (list == null || !list.isArray()) {
            return;
        }
        for (JsonNode row : list) {
            toLegacyBindRow(row).ifPresent(merged::add);
        }
    }

    /**
     * Maps {@code fonGnlBlgSiraliGetir} rows to the legacy BindHistory row shape expected by {@link TefasBindHistoryParsing}.
     */
    private Optional<ObjectNode> toLegacyBindRow(JsonNode row) {
        if (row == null || !row.isObject()) {
            return Optional.empty();
        }
        JsonNode kod = row.get("fonKodu");
        if (kod == null || kod.asText("").isBlank()) {
            return Optional.empty();
        }
        Optional<BigDecimal> nav = TefasBindHistoryParsing.decimalField(row.get("fiyat"));
        if (nav.isEmpty()) {
            nav = TefasBindHistoryParsing.decimalField(row.get("FIYAT"));
        }
        if (nav.isEmpty()) {
            return Optional.empty();
        }
        long millis = rowTarihMillis(row);
        if (millis == Long.MIN_VALUE) {
            return Optional.empty();
        }
        ObjectNode o = objectMapper.createObjectNode();
        o.put("FONKODU", kod.asText("").trim().toUpperCase(Locale.ROOT));
        o.set("FIYAT", objectMapper.valueToTree(nav.get()));
        o.put("TARIH", millis);
        return Optional.of(o);
    }

    private long rowTarihMillis(JsonNode row) {
        JsonNode legacy = row.get("TARIH");
        if (legacy != null && legacy.isNumber()) {
            return legacy.longValue();
        }
        if (legacy != null && legacy.isTextual()) {
            try {
                return Long.parseLong(legacy.asText("").trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        JsonNode dt = row.get("tarih");
        if (dt != null && dt.isTextual()) {
            try {
                LocalDate d = LocalDate.parse(dt.asText("").trim(), ISO_LOCAL_DATE);
                return d.atStartOfDay(TURKEY).toInstant().toEpochMilli();
            } catch (Exception ignored) {
                return Long.MIN_VALUE;
            }
        }
        return Long.MIN_VALUE;
    }

    private void pauseBetweenFonGnlChunks() {
        long ms = Math.max(0L, fundMarketProperties.getTefasFonGnlRequestSpacingMs());
        if (ms <= 0L) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private JsonNode fetchMergedViaLegacyForm(String normalizedCode, LocalDate startInclusive, LocalDate endInclusive) {
        String url = fundMarketProperties.getTefasBindHistoryUrl();
        if (url == null || url.isBlank()) {
            return emptyDataRoot();
        }
        int maxInclusiveDays = Math.min(89, Math.max(1, fundMarketProperties.getTefasHistoryChunkInclusiveDays()));
        ArrayNode merged = objectMapper.createArrayNode();
        LocalDate chunkStart = startInclusive;
        while (!chunkStart.isAfter(endInclusive)) {
            LocalDate chunkEnd = chunkStart.plusDays(maxInclusiveDays - 1L);
            if (chunkEnd.isAfter(endInclusive)) {
                chunkEnd = endInclusive;
            }
            MultiValueMap<String, String> form = bindHistoryForm(normalizedCode, chunkStart, chunkEnd);
            String body = postBindHistoryForm(form, url);
            if (body != null && !body.isBlank()) {
                try {
                    JsonNode root = objectMapper.readTree(body);
                    JsonNode chunkData = root.get("data");
                    if (chunkData != null && chunkData.isArray()) {
                        for (JsonNode row : chunkData) {
                            merged.add(row);
                        }
                    }
                } catch (Exception ignored) {
                    // skip malformed chunk
                }
            }
            chunkStart = chunkEnd.plusDays(1L);
        }
        ObjectNode synthetic = objectMapper.createObjectNode();
        synthetic.set("data", merged);
        return synthetic;
    }

    private JsonNode emptyDataRoot() {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("data", objectMapper.createArrayNode());
        return root;
    }

    private String postBindHistoryForm(MultiValueMap<String, String> form, String url) {
        return tefasWebClient
                .post()
                .uri(url)
                .contentType(MediaType.parseMediaType("application/x-www-form-urlencoded; charset=UTF-8"))
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
        m.add("sfontur", "");
        m.add("fonkod", normalizedCode);
        m.add("fongrup", "");
        m.add("bastarih", TEFAS_DD_MM_YYYY.format(chunkStart));
        m.add("bittarih", TEFAS_DD_MM_YYYY.format(chunkEnd));
        m.add("fonturkod", "");
        m.add("fonunvantip", "");
        m.add("kurucukod", "");
        return m;
    }
}
