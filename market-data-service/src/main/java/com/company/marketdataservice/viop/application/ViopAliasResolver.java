package com.company.marketdataservice.viop.application;

import com.company.marketdataservice.bootstrap.config.ViopMarketProperties;
import com.company.marketdataservice.viop.domain.ViopAliasSnapshot;
import com.company.marketdataservice.viop.domain.ViopContractRow;
import com.company.marketdataservice.viop.domain.ViopSettlementRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * VIOP settlement ve sözleşme satırlarından TLREF, DIBS ve FAIZ near alias sembollerini
 * çözen application katmanı bileşeni.
 */
@Component
public class ViopAliasResolver {

    public static final String ALIAS_TLREF_NEAR = "VIOP_TLREF_NEAR";
    public static final String ALIAS_DIBS_NEAR = "VIOP_DIBS_NEAR";
    public static final String ALIAS_FAIZ_NEAR = "VIOP_FAIZ_NEAR";

    private final ViopMarketProperties properties;

    public ViopAliasResolver(ViopMarketProperties properties) {
        this.properties = properties;
    }

    /**
     * Verilen trade date için en yakın vadeli faiz/tahvil future sözleşmelerinden alias snapshot listesi üretir.
     */
    public List<ViopAliasSnapshot> resolve(
            List<ViopContractRow> contracts,
            List<ViopSettlementRow> settlements,
            LocalDate tradeDate) {
        if (contracts == null || contracts.isEmpty() || settlements == null || settlements.isEmpty()) {
            return List.of();
        }
        Map<String, ViopContractRow> byCode = new HashMap<>();
        for (ViopContractRow contract : contracts) {
            if (contract.contractCode() != null && !contract.contractCode().isBlank()) {
                byCode.put(contract.contractCode().toUpperCase(Locale.ROOT), contract);
            }
        }

        List<Candidate> tlref = new ArrayList<>();
        List<Candidate> dibs = new ArrayList<>();
        List<Candidate> faiz = new ArrayList<>();
        for (ViopSettlementRow s : settlements) {
            ViopContractRow c = byCode.get(s.contractCode().toUpperCase(Locale.ROOT));
            if (c == null || c.expiryDate() == null || c.expiryDate().isBefore(tradeDate)) {
                continue;
            }
            if (!"FUTURE".equalsIgnoreCase(c.marketType())) {
                continue;
            }
            String hay = buildHaystack(c);
            Candidate candidate = new Candidate(c.contractCode(), c.expiryDate(), s.lastPrice());
            if (containsAny(hay, Set.copyOf(properties.getRateKeywords())) && hay.contains("TLREF")) {
                tlref.add(candidate);
                continue;
            }
            if (containsAny(hay, Set.copyOf(properties.getBondKeywords()))) {
                dibs.add(candidate);
                continue;
            }
            if (containsAny(hay, Set.copyOf(properties.getRateKeywords()))) {
                faiz.add(candidate);
            }
        }
        Comparator<Candidate> byNearest = Comparator.comparing(Candidate::expiryDate);
        List<ViopAliasSnapshot> out = new ArrayList<>();
        addIfAny(out, ALIAS_TLREF_NEAR, tradeDate, tlref.stream().min(byNearest).orElse(null));
        addIfAny(out, ALIAS_DIBS_NEAR, tradeDate, dibs.stream().min(byNearest).orElse(null));
        Candidate faizNear = faiz.stream().min(byNearest).orElse(null);
        if (faizNear == null) {
            // Keep FAIZ alias populated when only TLREF rate futures are available for the day.
            faizNear = tlref.stream().min(byNearest).orElse(null);
        }
        addIfAny(out, ALIAS_FAIZ_NEAR, tradeDate, faizNear);
        return out;
    }

    private static void addIfAny(List<ViopAliasSnapshot> out, String alias, LocalDate tradeDate, Candidate c) {
        if (c == null || c.value() == null) {
            return;
        }
        out.add(new ViopAliasSnapshot(alias, c.contractCode(), tradeDate, c.value()));
    }

    private static boolean containsAny(String haystack, Set<String> needles) {
        if (haystack == null || haystack.isBlank() || needles == null || needles.isEmpty()) {
            return false;
        }
        for (String needle : needles) {
            if (needle == null || needle.isBlank()) {
                continue;
            }
            if (haystack.contains(needle.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String buildHaystack(ViopContractRow c) {
        return (nullToEmpty(c.contractCode()) + " " + nullToEmpty(c.underlying()) + " " + nullToEmpty(c.marketGroup()))
                .toUpperCase(Locale.ROOT);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private record Candidate(String contractCode, LocalDate expiryDate, BigDecimal value) {}
}

