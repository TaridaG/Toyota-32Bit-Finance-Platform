package com.company.marketdataservice.rates.infrastructure.provider.bank;
import com.company.marketdataservice.bootstrap.config.DovizBankRatesProperties;
import com.company.marketdataservice.rates.infrastructure.http.dto.BankRatesResponseDto;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * `makro oran` infrastructure katmanı adaptörü.
 */
@Service
public class DovizBankRatesService {

    private static final Logger log = LoggerFactory.getLogger(DovizBankRatesService.class);

    private final DovizBankRatesProperties properties;
    private final RestClient restClient;
    private final Map<BankRatesAsset, AtomicReference<CacheEntry>> cache = new EnumMap<>(BankRatesAsset.class);

    public DovizBankRatesService(DovizBankRatesProperties properties) {
        this.properties = properties;
        this.restClient =
                RestClient.builder()
                        .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                        .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml")
                        .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "tr-TR,tr;q=0.9,en;q=0.5")
                        .build();
        for (BankRatesAsset asset : BankRatesAsset.values()) {
            cache.put(asset, new AtomicReference<>());
        }
    }

    /**
     * Veriyi yükler.
         * @param asset girdi parametresi
         * @return işlem sonucu
         */
    public Mono<BankRatesResponseDto> load(BankRatesAsset asset) {
        long nowMs = System.currentTimeMillis();
        AtomicReference<CacheEntry> slot = cache.get(asset);
        CacheEntry existing = slot.get();
        if (existing != null && existing.expiresAtMs > nowMs) {
            return Mono.just(existing.response);
        }

        return Mono.fromCallable(() -> fetchFresh(asset))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(fresh -> slot.set(new CacheEntry(fresh, nowMs + properties.getCacheTtlMs())));
    }

    private BankRatesResponseDto fetchFresh(BankRatesAsset asset) {
        String sourceUrl = properties.urlFor(asset);
        try {
            String html =
                    restClient
                            .get()
                            .uri(sourceUrl)
                            .retrieve()
                            .body(String.class);

            if (html == null || html.isBlank()) {
                throw new IllegalStateException("Empty HTML from " + sourceUrl);
            }

            DovizBankRatesHtmlParser.ParsedBankRatesTable table = DovizBankRatesHtmlParser.parse(html);
            BankRatesResponseDto dto = new BankRatesResponseDto();
            dto.setAsset(asset.name());
            dto.setTitle(table.title());
            dto.setSourceUrl(sourceUrl);
            dto.setFetchedAt(Instant.now());
            dto.setRows(table.rows());
            return dto;
        } catch (RestClientResponseException ex) {
            log.warn("Doviz bank rates HTTP {} for asset {} url {}", ex.getStatusCode(), asset, sourceUrl);
            throw new IllegalStateException("Failed to fetch bank rates from source", ex);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Doviz bank rates fetch failed for asset {} url {}: {}", asset, sourceUrl, ex.getMessage());
            throw new IllegalStateException("Failed to parse bank rates from source", ex);
        }
    }

    private record CacheEntry(BankRatesResponseDto response, long expiresAtMs) {}
}
