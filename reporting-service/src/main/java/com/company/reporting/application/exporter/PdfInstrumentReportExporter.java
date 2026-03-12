package com.company.reporting.application.exporter;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.dto.AnalyticsMovingAverageDto;
import com.company.reporting.dto.AnalyticsTrendMetricDto;
import com.company.reporting.dto.InstrumentReportData;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class PdfInstrumentReportExporter implements ReportExporter {

    @Override
    public ExportFormat supports() {
        return ExportFormat.PDF;
    }

    @Override
    public byte[] export(InstrumentReportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();
            document.add(new Paragraph("Instrument Report"));
            document.add(new Paragraph("Symbol: " + data.getSymbol()));
            document.add(new Paragraph(" "));

            if (!data.getMovingAverages().isEmpty()) {
                AnalyticsMovingAverageDto lastMa = data.getMovingAverages().get(data.getMovingAverages().size() - 1);
                document.add(new Paragraph("MA7: " + lastMa.getMa7()));
                document.add(new Paragraph("MA30: " + lastMa.getMa30()));
                document.add(new Paragraph("MA90: " + lastMa.getMa90()));
                document.add(new Paragraph(" "));
            }

            if (!data.getTrendMetrics().isEmpty()) {
                AnalyticsTrendMetricDto lastTrend = data.getTrendMetrics().get(data.getTrendMetrics().size() - 1);
                document.add(new Paragraph("Trend: " + lastTrend.getTrendDirection()));
                document.add(new Paragraph("Momentum: " + lastTrend.getMomentum()));
                document.add(new Paragraph("Price Slope: " + lastTrend.getPriceSlope()));
                document.add(new Paragraph(" "));
            }

            document.add(new Paragraph("Candle Count: " + data.getCandles().size()));
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PDF export failed", e);
        }
    }

    @Override
    public String buildFileName(String symbol) {
        return symbol + "-instrument-report.pdf";
    }
}