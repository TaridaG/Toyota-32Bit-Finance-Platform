package com.company.marketdataservice.api;

import com.company.marketdataservice.provider.health.ProviderHealthTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
