package com.company.reporting.dto;

import com.company.reporting.domain.enums.ExportFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePortfolioReportRequest {

    @NotNull
    private ExportFormat exportFormat;
}