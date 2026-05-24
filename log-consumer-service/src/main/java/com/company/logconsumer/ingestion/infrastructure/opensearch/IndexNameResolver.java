package com.company.logconsumer.ingestion.infrastructure.opensearch;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * OpenSearch index adına günlük tarih suffix'i ekler ({@code prefix-yyyy-MM-dd}).
 */
@Component
public class IndexNameResolver {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String todayIndex(String prefix) {
        return prefix + "-" + LocalDate.now().format(FMT);
    }
}
