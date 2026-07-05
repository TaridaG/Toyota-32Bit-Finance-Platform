package com.company.marketdataservice.viop.application;

import com.company.marketdataservice.bootstrap.config.ViopMarketProperties;
import com.company.marketdataservice.viop.domain.ViopContractRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * BIST dosyalarında {@code PAZAR} kolonu yoksa kontrat metadata'sından VIOP pazar kodunu türetir.
 *
 * <p>Segment filtreleri ({@code D_FI}, {@code D_BO}, …) bu değere bağlıdır; resmi {@code viop_*.csv}
 * çoğu gün PAZAR içermediği için ingest sonrası infer zorunludur.
 */
@Component
public class ViopPazarInferrer {

    private static final Set<String> INDEX_MARKERS = Set.of("XU030", "XU100", "XUSIN");
    private static final Set<String> COMMODITY_MARKERS =
            Set.of("USDTRY", "EURTRY", "GBPTRY", "XAU", "ALTIN", "GUMUS", "SILVER", "PETROL", "BRENT");

    private final ViopMarketProperties properties;

    public ViopPazarInferrer(ViopMarketProperties properties) {
        this.properties = properties;
    }

    /**
     * {@code pazar} boş olan satırlara segment kodu atar; dolu satırlara dokunmaz.
     */
    public List<ViopContractRow> inferMissing(List<ViopContractRow> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return contracts == null ? List.of() : contracts;
        }
        List<ViopContractRow> out = new ArrayList<>(contracts.size());
        for (ViopContractRow row : contracts) {
            if (StringUtils.hasText(row.pazar())) {
                out.add(row);
                continue;
            }
            String inferred = inferPazar(row);
            if (inferred == null) {
                out.add(row);
                continue;
            }
            out.add(copyWithPazar(row, inferred));
        }
        return out;
    }

    /** Tek satır için pazar kodu türetir; eşleşme yoksa {@code null}. */
    public String inferPazar(ViopContractRow row) {
        if (row == null) {
            return null;
        }
        String haystack = buildHaystack(row);
        if (haystack.isBlank()) {
            return null;
        }
        if (containsAny(haystack, Set.copyOf(properties.getBondKeywords()))) {
            return "D_BO";
        }
        if (containsAny(haystack, Set.copyOf(properties.getRateKeywords()))) {
            return "D_FI";
        }
        if (containsAnyMarker(haystack, INDEX_MARKERS)) {
            return "D_IX";
        }
        if (containsAnyMarker(haystack, COMMODITY_MARKERS)) {
            return "D_CM";
        }
        String code = row.contractCode() == null ? "" : row.contractCode().trim().toUpperCase(Locale.ROOT);
        if (code.startsWith("F_") || code.startsWith("O_")) {
            return "D_EQ";
        }
        return null;
    }

    private static ViopContractRow copyWithPazar(ViopContractRow row, String pazar) {
        return new ViopContractRow(
                row.contractCode(),
                row.underlying(),
                row.marketType(),
                row.marketGroup(),
                row.expiryDate(),
                row.settlementType(),
                row.currency(),
                pazar,
                row.sourceFile());
    }

    private static String buildHaystack(ViopContractRow row) {
        return (nullToEmpty(row.contractCode())
                        + " "
                        + nullToEmpty(row.underlying())
                        + " "
                        + nullToEmpty(row.marketGroup()))
                .toUpperCase(Locale.ROOT);
    }

    private static boolean containsAny(String haystack, Set<String> needles) {
        if (!StringUtils.hasText(haystack) || needles == null || needles.isEmpty()) {
            return false;
        }
        for (String needle : needles) {
            if (!StringUtils.hasText(needle)) {
                continue;
            }
            if (haystack.contains(needle.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyMarker(String haystack, Set<String> markers) {
        return containsAny(haystack, markers);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
