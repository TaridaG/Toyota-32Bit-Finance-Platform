package com.company.logconsumer.ingestion.infrastructure.opensearch;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexNameResolverTest {

    private final IndexNameResolver resolver = new IndexNameResolver();

    @Test
    void todayIndex_appendsCurrentDate() {
        String expected = "application-logs-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        assertEquals(expected, resolver.todayIndex("application-logs"));
    }
}
