package com.company.marketdataservice.viop.application;

import com.company.marketdataservice.viop.domain.ViopContractRow;
import com.company.marketdataservice.viop.domain.ViopSettlementRow;
import com.company.marketdataservice.viop.infrastructure.source.BistDerivativesFileClient.DownloadedFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * BIST türev sözleşme ve settlement dosyalarını (ZIP, HTML tablo, ayraçlı metin) parse eden yardımcı sınıf.
 */
public final class ViopFileParsers {

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9_]+");
    private static final DateTimeFormatter[] DATE_FORMATTERS =
            new DateTimeFormatter[] {
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd.MM.uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("dd-MM-uuuu"),
                DateTimeFormatter.ofPattern("dd.MM.uu"),
            };

    private ViopFileParsers() {}

    /**
     * İndirilen dosyadan VIOP sözleşme kataloğu satırlarını parse eder.
     */
    public static List<ViopContractRow> parseContracts(DownloadedFile file) {
        return parseContracts(file.fileName(), file.contentType(), file.body());
    }

    /**
     * İndirilen dosyadan VIOP günlük settlement satırlarını parse eder.
     */
    public static List<ViopSettlementRow> parseSettlements(DownloadedFile file, LocalDate fallbackTradeDate) {
        return parseSettlements(file.fileName(), file.contentType(), file.body(), fallbackTradeDate);
    }

    private static List<ViopContractRow> parseContracts(String sourceName, String contentType, byte[] body) {
        if (isZip(sourceName, contentType)) {
            List<ViopContractRow> out = new ArrayList<>();
            for (UnzippedFile f : unzipFiles(body)) {
                out.addAll(parseContracts(f.fileName(), f.contentType(), f.body()));
            }
            return out;
        }
        String text = asText(body);
        if (looksLikeHtml(contentType, sourceName, text)) {
            return parseContractsFromHtml(sourceName, text);
        }
        return parseContractsFromDelimited(sourceName, text);
    }

    private static List<ViopSettlementRow> parseSettlements(
            String sourceName, String contentType, byte[] body, LocalDate fallbackTradeDate) {
        if (isZip(sourceName, contentType)) {
            List<ViopSettlementRow> out = new ArrayList<>();
            for (UnzippedFile f : unzipFiles(body)) {
                out.addAll(parseSettlements(f.fileName(), f.contentType(), f.body(), fallbackTradeDate));
            }
            return out;
        }
        String text = asText(body);
        if (looksLikeHtml(contentType, sourceName, text)) {
            return parseSettlementsFromHtml(sourceName, text, fallbackTradeDate);
        }
        return parseSettlementsFromDelimited(sourceName, text, fallbackTradeDate);
    }

    private static List<ViopContractRow> parseContractsFromDelimited(String sourceFile, String text) {
        List<Map<String, String>> rows = parseDelimitedRows(text);
        List<ViopContractRow> out = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String contractCode = first(row, "sozlesmekodu", "contractcode", "contract_code", "code");
            if (contractCode == null || contractCode.isBlank()) {
                continue;
            }
            String underlying = first(row, "dayanakvarlik", "underlying", "underlyingasset", "underlying_asset");
            String expiryRaw = first(row, "vadetarihi", "expiry", "maturitydate", "maturity_date");
            LocalDate expiryDate = parseDate(expiryRaw);
            String marketType = inferMarketType(contractCode, first(row, "opsiyonturu", "optiontype"));
            String marketGroup = first(row, "grup", "marketgroup", "pazar", "market");
            String settlementType = first(row, "uzlasmatipi", "settlementtype");
            String currency = first(row, "parabirimi", "currency");
            out.add(
                    new ViopContractRow(
                            normalizeCode(contractCode),
                            trimOrNull(underlying),
                            marketType,
                            trimOrNull(marketGroup),
                            expiryDate,
                            trimOrNull(settlementType),
                            trimOrNull(currency),
                            sourceFile));
        }
        return out;
    }

    private static List<ViopSettlementRow> parseSettlementsFromDelimited(
            String sourceFile, String text, LocalDate fallbackTradeDate) {
        List<Map<String, String>> rows = parseDelimitedRows(text);
        List<ViopSettlementRow> out = new ArrayList<>();
        for (Map<String, String> row : rows) {
            ViopSettlementRow parsed = parseSettlementRow(row, fallbackTradeDate, sourceFile);
            if (parsed != null) {
                out.add(parsed);
            }
        }
        return out;
    }

    private static List<ViopContractRow> parseContractsFromHtml(String sourceFile, String html) {
        Document doc = Jsoup.parse(html);
        List<ViopContractRow> out = new ArrayList<>();
        for (Element table : doc.select("table")) {
            List<Map<String, String>> rows = parseHtmlTable(table);
            for (Map<String, String> row : rows) {
                String contractCode = first(row, "sozlesmekodu", "contractcode", "contract_code", "code");
                if (contractCode == null || contractCode.isBlank()) {
                    continue;
                }
                out.add(
                        new ViopContractRow(
                                normalizeCode(contractCode),
                                trimOrNull(first(row, "dayanakvarlik", "underlying")),
                                inferMarketType(contractCode, first(row, "opsiyonturu", "optiontype")),
                                trimOrNull(first(row, "grup", "marketgroup", "pazar", "market")),
                                parseDate(first(row, "vadetarihi", "expiry", "maturitydate")),
                                trimOrNull(first(row, "uzlasmatipi", "settlementtype")),
                                trimOrNull(first(row, "parabirimi", "currency")),
                                sourceFile));
            }
        }
        return out;
    }

    private static List<ViopSettlementRow> parseSettlementsFromHtml(
            String sourceFile, String html, LocalDate fallbackTradeDate) {
        Document doc = Jsoup.parse(html);
        List<ViopSettlementRow> out = new ArrayList<>();
        for (Element table : doc.select("table")) {
            List<Map<String, String>> rows = parseHtmlTable(table);
            for (Map<String, String> row : rows) {
                ViopSettlementRow parsed = parseSettlementRow(row, fallbackTradeDate, sourceFile);
                if (parsed != null) {
                    out.add(parsed);
                }
            }
        }
        return out;
    }

    private static List<Map<String, String>> parseDelimitedRows(String text) {
        String[] lines = text.replace("\r\n", "\n").split("\n");
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            String t = line == null ? "" : line.trim();
            if (!t.isBlank()) {
                cleaned.add(t);
            }
        }
        if (cleaned.size() < 2) {
            return List.of();
        }
        char delimiter = detectDelimiter(cleaned.getFirst());
        String[] headersRaw = splitLine(cleaned.getFirst(), delimiter);
        List<String> headers = new ArrayList<>();
        for (String h : headersRaw) {
            headers.add(normalizeHeader(h));
        }
        List<Map<String, String>> out = new ArrayList<>();
        for (int i = 1; i < cleaned.size(); i++) {
            String[] cols = splitLine(cleaned.get(i), delimiter);
            if (cols.length == 0) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < headers.size() && c < cols.length; c++) {
                row.put(headers.get(c), cols[c].trim());
            }
            out.add(row);
        }
        return out;
    }

    private static List<Map<String, String>> parseHtmlTable(Element table) {
        List<String> headers = new ArrayList<>();
        Elements headCells = table.select("thead tr th");
        if (headCells.isEmpty()) {
            headCells = table.select("tr").first() == null ? new Elements() : table.select("tr").first().select("th,td");
        }
        for (Element th : headCells) {
            headers.add(normalizeHeader(th.text()));
        }
        List<Map<String, String>> out = new ArrayList<>();
        Elements rows = table.select("tbody tr");
        if (rows.isEmpty()) {
            rows = table.select("tr");
            if (!rows.isEmpty()) {
                rows.remove(0);
            }
        }
        for (Element tr : rows) {
            Elements cells = tr.select("td");
            if (cells.isEmpty()) {
                continue;
            }
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < headers.size() && i < cells.size(); i++) {
                row.put(headers.get(i), cells.get(i).text().trim());
            }
            out.add(row);
        }
        return out;
    }

    private static List<UnzippedFile> unzipFiles(byte[] zipBytes) {
        List<UnzippedFile> out = new ArrayList<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                byte[] bytes = zin.readAllBytes();
                String name = entry.getName();
                String lc = name.toLowerCase(Locale.ROOT);
                String contentType = lc.endsWith(".html") || lc.endsWith(".htm") ? "text/html" : "text/plain";
                out.add(new UnzippedFile(name, contentType, bytes));
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return out;
    }

    private static boolean isZip(String sourceName, String contentType) {
        String n = sourceName == null ? "" : sourceName.toLowerCase(Locale.ROOT);
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return n.endsWith(".zip") || ct.contains("zip");
    }

    private static String asText(byte[] body) {
        return new String(body == null ? new byte[0] : body, StandardCharsets.UTF_8);
    }

    private static boolean looksLikeHtml(String contentType, String sourceName, String text) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String name = sourceName == null ? "" : sourceName.toLowerCase(Locale.ROOT);
        return ct.contains("html")
                || name.endsWith(".html")
                || name.endsWith(".htm")
                || text.contains("<table")
                || text.contains("<html");
    }

    private static char detectDelimiter(String headerLine) {
        if (headerLine.indexOf(';') >= 0) {
            return ';';
        }
        if (headerLine.indexOf('\t') >= 0) {
            return '\t';
        }
        return ',';
    }

    private static String[] splitLine(String line, char delimiter) {
        String escaped = Pattern.quote(String.valueOf(delimiter));
        return line.split(escaped, -1);
    }

    private static String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.toLowerCase(Locale.ROOT);
        s = s.replace('ı', 'i').replace('ş', 's').replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o').replace('ç', 'c');
        s = s.replace("%", "percent");
        s = s.replace("/", "_");
        s = s.replace(" ", "");
        s = s.replace("-", "");
        s = s.replace(".", "");
        return s;
    }

    private static String first(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String normalized = normalizeHeader(key);
            String value = row.get(normalized);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalizeCode(String raw) {
        String s = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (s.startsWith("F_") || s.startsWith("O_")) {
            return s;
        }
        Matcher m = Pattern.compile("([FO]_[A-Z0-9_./-]+)").matcher(s);
        if (m.find()) {
            return m.group(1);
        }
        return NON_ALNUM.matcher(s).replaceAll("_");
    }

    private static String inferMarketType(String contractCode, String optionType) {
        if (optionType != null && !optionType.isBlank()) {
            return "OPTION";
        }
        String c = contractCode == null ? "" : contractCode.trim().toUpperCase(Locale.ROOT);
        return c.startsWith("O_") ? "OPTION" : "FUTURE";
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(t, fmt);
            } catch (Exception ignored) {
                // continue
            }
        }
        if (t.matches("\\d{6}")) { // ddMMyy
            String d = t.substring(0, 2);
            String m = t.substring(2, 4);
            String y = "20" + t.substring(4, 6);
            return LocalDate.parse(y + "-" + m + "-" + d);
        }
        return null;
    }

    private static ViopSettlementRow parseSettlementRow(
            Map<String, String> row, LocalDate fallbackTradeDate, String sourceFile) {
        String tradeDateRaw = first(row, "tarih", "tradedate", "trade_date");
        String contractCode =
                first(
                        row,
                        "sozlesmekodu",
                        "contractcode",
                        "contract_code",
                        "code",
                        "kontrat",
                        "contract",
                        "instrumentseries");
        if (contractCode == null || contractCode.isBlank() || isDuplicateHeaderRow(contractCode, tradeDateRaw)) {
            return null;
        }
        LocalDate tradeDate = parseDate(tradeDateRaw);
        if (tradeDate == null) {
            tradeDate = fallbackTradeDate;
        }
        BigDecimal lastPrice = parseDecimal(settlementPriceRaw(row));
        if (tradeDate == null || lastPrice == null) {
            return null;
        }
        return new ViopSettlementRow(
                tradeDate,
                normalizeCode(contractCode),
                lastPrice,
                parseDecimal(changePercentRaw(row)),
                parseDecimal(first(row, "fark", "changeamount", "change_amount")),
                parseDecimal(volumeTlRaw(row)),
                parseDecimal(volumeQtyRaw(row)),
                parseDecimal(openInterestRaw(row)),
                sourceFile);
    }

    private static String settlementPriceRaw(Map<String, String> row) {
        return first(
                row,
                "uzlasmafiyati",
                "sonfiyat",
                "lastprice",
                "settlement",
                "settlementprice");
    }

    private static String changePercentRaw(Map<String, String> row) {
        return first(
                row,
                "degisim",
                "changepercent",
                "change_pct",
                "uzlasmafiyatidegisimi(percent)",
                "changeofsettlementprice(percent)",
                "uzlasmafiyatidegisimi");
    }

    private static String volumeTlRaw(Map<String, String> row) {
        return first(row, "islemhacmi", "hacimtl", "volumetl", "volume_tl", "volume", "tradedvalue");
    }

    private static String volumeQtyRaw(Map<String, String> row) {
        return first(
                row,
                "islemmiktari",
                "hacimadet",
                "volumeqty",
                "volume_qty",
                "quantity",
                "tradedvolume",
                "tradvolume");
    }

    private static String openInterestRaw(Map<String, String> row) {
        return first(row, "acikpozisyon", "openposition", "openinterest", "oi", "openposition");
    }

    private static boolean isDuplicateHeaderRow(String contractCode, String tradeDateRaw) {
        String code = contractCode.trim().toUpperCase(Locale.ROOT);
        if ("INSTRUMENT SERIES".equals(code) || "SOZLESME KODU".equals(code) || "KONTRAT".equals(code)) {
            return true;
        }
        if (tradeDateRaw != null) {
            String date = tradeDateRaw.trim().toUpperCase(Locale.ROOT);
            if ("TRADE DATE".equals(date) || "TARIH".equals(date)) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        t = t.replace("%", "").replace("bp", "").replace(" ", "");
        if (t.isBlank() || "-".equals(t) || "—".equals(t)) {
            return null;
        }
        if (t.contains(",")) {
            // Turkish: 12.500.000,00
            t = t.replace(".", "").replace(",", ".");
        } else {
            long dotCount = t.chars().filter(ch -> ch == '.').count();
            if (dotCount > 1) {
                t = t.replace(".", "");
            }
            // Single dot or no dot: ISO decimal (e.g. 39.41, 96.611)
        }
        try {
            return new BigDecimal(t);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String trimOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private record UnzippedFile(String fileName, String contentType, byte[] body) {}
}

