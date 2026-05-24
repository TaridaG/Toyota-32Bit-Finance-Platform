package com.company.marketdataservice.rates.infrastructure.http.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * `makro oran` REST API için HTTP DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CpiLatestDto {

    private String metric;
    private BigDecimal value;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate observationMonth;
    /** Change in the same metric vs prior month (percentage points for % series). */
    private BigDecimal deltaVsPriorMonth;
    private String sourceProvider = "TCMB_EVDS";
    private String unit = "PERCENT";

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public LocalDate getObservationMonth() {
        return observationMonth;
    }

    public void setObservationMonth(LocalDate observationMonth) {
        this.observationMonth = observationMonth;
    }

    public BigDecimal getDeltaVsPriorMonth() {
        return deltaVsPriorMonth;
    }

    public void setDeltaVsPriorMonth(BigDecimal deltaVsPriorMonth) {
        this.deltaVsPriorMonth = deltaVsPriorMonth;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public void setSourceProvider(String sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
