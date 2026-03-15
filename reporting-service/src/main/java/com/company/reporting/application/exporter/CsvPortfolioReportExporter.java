package com.company.reporting.application.exporter;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.dto.PortfolioAssetRow;
import com.company.reporting.dto.PortfolioReportData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Component
public class CsvPortfolioReportExporter {

    public ExportFormat supports() {
        return ExportFormat.CSV;
    }

    public byte[] export(PortfolioReportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("symbol", "quantity", "avgBuyPrice", "currentPrice",
                             "currentValue", "allocation", "profitLoss"))) {

            for (PortfolioAssetRow row : data.getAssets()) {
                printer.printRecord(
                        row.getSymbol(),
                        row.getQuantity(),
                        row.getAvgBuyPrice(),
                        row.getCurrentPrice(),
                        row.getCurrentValue(),
                        row.getAllocation(),
                        row.getProfitLoss()
                );
            }

            printer.printRecord();
            printer.printRecord("TOTAL", "", "", "",
                    data.getTotalValue(), "", data.getTotalProfitLoss());

            printer.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Portfolio CSV export failed", e);
        }
    }

    public String buildFileName() {
        return "portfolio-report.csv";
    }
}