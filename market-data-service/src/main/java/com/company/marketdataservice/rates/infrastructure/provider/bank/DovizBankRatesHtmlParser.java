package com.company.marketdataservice.rates.infrastructure.provider.bank;
import com.company.marketdataservice.rates.infrastructure.http.dto.BankRatesRowDto;
import com.company.marketdataservice.shared.provider.tcmb.EvdsTurkishNumberParser;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

/**
 * `makro oran` infrastructure katmanı adaptörü.
 */
final class DovizBankRatesHtmlParser {

    private DovizBankRatesHtmlParser() {
    }

    static ParsedBankRatesTable parse(String html) {
        Document doc = Jsoup.parse(html);
        Element heading =
                doc.select("h2.section-title").stream()
                        .filter(h -> h.text().contains("Banka Kurları"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Bank rates heading not found"));

        Element table = heading.nextElementSibling();
        while (table != null && table.select("table tbody tr").isEmpty()) {
            table = table.nextElementSibling();
        }
        if (table == null) {
            throw new IllegalStateException("Bank rates table not found");
        }

        Elements rows = table.select("table tbody tr");
        List<BankRatesRowDto> parsed = new ArrayList<>();
        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < 5) {
                continue;
            }
            Element link = cells.get(0).selectFirst("a[href]");
            if (link == null) {
                continue;
            }

            BankRatesRowDto dto = new BankRatesRowDto();
            dto.setDetailUrl(link.attr("abs:href"));
            dto.setSlug(extractSlug(dto.getDetailUrl()));
            dto.setName(link.text().trim());
            Element img = link.selectFirst("img[src]");
            if (img != null) {
                dto.setLogoUrl(img.attr("abs:src"));
            }
            dto.setBuy(parseRate(cells.get(1).text()));
            dto.setSell(parseRate(cells.get(2).text()));
            dto.setSpread(parseRate(cells.get(3).text()));
            dto.setSpreadPercent(parsePercent(cells.get(4).text()));
            parsed.add(dto);
        }

        if (parsed.isEmpty()) {
            throw new IllegalStateException("Bank rates table has no rows");
        }

        applyBestFlags(parsed);
        return new ParsedBankRatesTable(heading.text().trim(), parsed);
    }

    private static void applyBestFlags(List<BankRatesRowDto> rows) {
        BigDecimal maxBuy = null;
        BigDecimal minSell = null;
        BigDecimal minSpread = null;
        BigDecimal maxSpread = null;
        BigDecimal minSpreadPct = null;
        BigDecimal maxSpreadPct = null;
        for (BankRatesRowDto row : rows) {
            if (row.getBuy() != null && (maxBuy == null || row.getBuy().compareTo(maxBuy) > 0)) {
                maxBuy = row.getBuy();
            }
            if (row.getSell() != null && (minSell == null || row.getSell().compareTo(minSell) < 0)) {
                minSell = row.getSell();
            }
            if (row.getSpread() != null && (minSpread == null || row.getSpread().compareTo(minSpread) < 0)) {
                minSpread = row.getSpread();
            }
            if (row.getSpread() != null && (maxSpread == null || row.getSpread().compareTo(maxSpread) > 0)) {
                maxSpread = row.getSpread();
            }
            if (row.getSpreadPercent() != null
                    && (minSpreadPct == null || row.getSpreadPercent().compareTo(minSpreadPct) < 0)) {
                minSpreadPct = row.getSpreadPercent();
            }
            if (row.getSpreadPercent() != null
                    && (maxSpreadPct == null || row.getSpreadPercent().compareTo(maxSpreadPct) > 0)) {
                maxSpreadPct = row.getSpreadPercent();
            }
        }
        for (BankRatesRowDto row : rows) {
            if (maxBuy != null && row.getBuy() != null && row.getBuy().compareTo(maxBuy) == 0) {
                row.setBestBuy(true);
            }
            if (minSell != null && row.getSell() != null && row.getSell().compareTo(minSell) == 0) {
                row.setBestSell(true);
            }
            if (minSpread != null && row.getSpread() != null && row.getSpread().compareTo(minSpread) == 0) {
                row.setBestSpreadMin(true);
            }
            if (maxSpread != null && row.getSpread() != null && row.getSpread().compareTo(maxSpread) == 0) {
                row.setBestSpreadMax(true);
            }
            if (minSpreadPct != null
                    && row.getSpreadPercent() != null
                    && row.getSpreadPercent().compareTo(minSpreadPct) == 0) {
                row.setBestSpreadPctMin(true);
            }
            if (maxSpreadPct != null
                    && row.getSpreadPercent() != null
                    && row.getSpreadPercent().compareTo(maxSpreadPct) == 0) {
                row.setBestSpreadPctMax(true);
            }
        }
    }

    private static BigDecimal parseRate(String raw) {
        return EvdsTurkishNumberParser.parsePercentLike(raw);
    }

    private static BigDecimal parsePercent(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return EvdsTurkishNumberParser.parsePercentLike(raw.replace("%", "").trim());
    }

    private static String extractSlug(String detailUrl) {
        if (!StringUtils.hasText(detailUrl)) {
            return "";
        }
        try {
            URI uri = URI.create(detailUrl.trim());
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return "";
            }
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].isBlank()) {
                    continue;
                }
                String next = parts[i + 1];
                if (StringUtils.hasText(next) && !next.contains(".")) {
                    return parts[i].toLowerCase(Locale.ROOT);
                }
            }
            return parts.length > 0 ? parts[parts.length - 2].toLowerCase(Locale.ROOT) : "";
        } catch (Exception ex) {
            return "";
        }
    }

    record ParsedBankRatesTable(String title, List<BankRatesRowDto> rows) {}
}
