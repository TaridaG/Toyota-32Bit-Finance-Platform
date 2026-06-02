package com.company.marketdataservice.catalog.infrastructure.http;

import com.company.marketdataservice.catalog.infrastructure.http.dto.IngestCatalogItemDto;
import com.company.marketdataservice.catalog.infrastructure.service.IngestCatalogAdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/market/ingest")
@RequiredArgsConstructor
public class IngestCatalogAdminController {

    private static final Logger log = LoggerFactory.getLogger(IngestCatalogAdminController.class);

    private final IngestCatalogAdminService service;

    @GetMapping("/catalog")
    public List<IngestCatalogItemDto> getCatalog() {
        return service.getCatalog();
    }

    @PostMapping("/config/enable")
    public void enable(@RequestBody IngestConfigCommand command) {
        service.enable(command.instrumentId(), command.segment());
    }

    @PostMapping("/config/disable")
    public void disable(@RequestBody IngestConfigCommand command) {
        service.disable(command.instrumentId(), command.segment());
    }

    @PostMapping("/actions/history-pull")
    public Mono<Void> triggerHistoryPull(@RequestBody IngestConfigCommand command) {
        Long instrumentId = command.instrumentId();
        String segment = command.segment();
        Schedulers.boundedElastic().schedule(() -> {
            try {
                service.triggerHistoryPull(instrumentId, segment);
            } catch (Exception ex) {
                log.warn(
                        "HISTORY_PULL_ASYNC_FAILED instrumentId={} segment={} reason={}",
                        instrumentId,
                        segment,
                        ex.getMessage());
            }
        });
        return Mono.empty();
    }

    @PostMapping("/actions/live-pull")
    public Mono<Void> triggerLivePull(@RequestBody IngestConfigCommand command) {
        return Mono.fromRunnable(() -> service.triggerLivePull(command.instrumentId(), command.segment()))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @PostMapping("/actions/delete")
    public Mono<Void> deleteFromIngest(@RequestBody IngestConfigCommand command) {
        return Mono.fromRunnable(() -> service.deleteInstrumentFromIngest(command.instrumentId(), command.segment()))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public record IngestConfigCommand(Long instrumentId, String segment) {
    }
}

