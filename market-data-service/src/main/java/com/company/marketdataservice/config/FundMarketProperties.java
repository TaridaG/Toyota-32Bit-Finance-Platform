package com.company.marketdataservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "market.fund")
public class FundMarketProperties {

    private boolean schedulerEnabled;

    private long delayMs = 3_600_000L;

    private List<String> trackedFundCodes = new ArrayList<>();

    private String httpUrlTemplate = "";

    private String tefasBindHistoryUrl = "https://www.tefas.gov.tr/api/DB/BindHistoryInfo";

    private String tefasFontip = "YAT";

    /** Total history window stitched from <=90-day API segments. */
    private int tefasHistoryLookbackDays = 370;

    /** BindHistory rejects requests when bastarih-bittarih span exceeds TEFAS policy (~90 calendar days). */
    private int tefasHistoryChunkInclusiveDays = 89;
}
