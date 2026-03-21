package com.company.reporting.dto;

import com.company.reporting.domain.enums.ExportFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateInstrumentReportRequest {

    @NotBlank
    private String symbol;

    @NotNull
    private LocalDate from;

    @NotNull
    private LocalDate to;

    @NotNull
    private ExportFormat exportFormat;

    private UUID userId;
    private String userEmail;
}