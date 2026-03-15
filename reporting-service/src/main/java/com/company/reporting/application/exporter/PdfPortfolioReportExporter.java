package com.company.reporting.application.exporter;

import com.company.reporting.domain.enums.ExportFormat;
import com.company.reporting.dto.PortfolioAssetRow;
import com.company.reporting.dto.PortfolioReportData;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class PdfPortfolioReportExporter {

    public ExportFormat supports() {
        return ExportFormat.PDF;
    }

    public byte[] export(PortfolioReportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();
            document.add(new Paragraph("Portfolio Report"));
            document.add(new Paragraph(" "));

            for (PortfolioAssetRow row : data.getAssets()) {
                document.add(new Paragraph(
                        row.getSymbol()
                                + " | qty=" + row.getQuantity()
                                + " | currentValue=" + row.getCurrentValue()
                                + " | allocation=" + row.getAllocation()
                                + " | pnl=" + row.getProfitLoss()
                ));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total Value: " + data.getTotalValue()));
            document.add(new Paragraph("Total Profit/Loss: " + data.getTotalProfitLoss()));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Portfolio PDF export failed", e);
        }
    }

    public String buildFileName() {
        return "portfolio-report.pdf";
    }
}