package com.company.reporting.application.exporter;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.dto.AnalyticsCandleDto;
import com.company.reporting.dto.InstrumentReportData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Component
public class CsvInstrumentReportExporter implements ReportExporter {

    @Override
    public ExportFormat supports() {
        return ExportFormat.CSV;
    }

    @Override
    public byte[] export(InstrumentReportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("symbol", "date", "open", "high", "low", "close", "volume", "tradeCount"))) {

            for (AnalyticsCandleDto candle : data.getCandles()) {
                printer.printRecord(
                        candle.getSymbol(),
                        candle.getCandleDate(),
                        candle.getOpen(),
                        candle.getHigh(),
                        candle.getLow(),
                        candle.getClose(),
                        candle.getVolume(),
                        candle.getTradeCount()
                );
            }

            printer.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("CSV export failed", e);
        }
    }

    @Override
    public String buildFileName(String symbol) {
        return symbol + "-instrument-report.csv";
    }
}