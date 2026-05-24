package com.company.marketdataservice.shared.provider.health;
import com.company.marketdataservice.shared.provider.health.ProviderHealthTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * `ortak altyapı` REST endpoint'lerini expose eden HTTP controller.
 */
@RestController
@RequestMapping("/api/admin/providers")
@RequiredArgsConstructor
public class ProviderHealthController {

    private final ProviderHealthTracker tracker;

    @GetMapping("/{provider}")
    public ProviderHealthTracker.ProviderMetrics getMetrics(
            @PathVariable String provider) {
        return tracker.getMetrics(provider);
    }
}
