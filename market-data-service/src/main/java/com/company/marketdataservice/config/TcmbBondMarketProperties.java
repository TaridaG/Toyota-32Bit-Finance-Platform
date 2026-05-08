package com.company.marketdataservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "market.bond")
public class TcmbBondMarketProperties {

    private boolean schedulerEnabled = true;
    private List<TcmbBondSeries> tracked = new ArrayList<>();

    @Getter
    @Setter
    public static class TcmbBondSeries {
        private String symbol;
        private String evdsSeries;
    }
}
