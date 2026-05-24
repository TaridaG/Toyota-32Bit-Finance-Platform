package com.company.logconsumer.bootstrap.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaRecordSupportTest {

    @Test
    void extractEventId_parsesJsonField() {
        String json = """
                {"eventId":"evt-42","message":"failed"}
                """;

        assertEquals("evt-42", KafkaRecordSupport.extractEventId(json));
    }

    @Test
    void extractEventId_returnsUnknownWhenMissing() {
        assertEquals("unknown", KafkaRecordSupport.extractEventId("{\"message\":\"x\"}"));
    }

    @Test
    void extractEventId_returnsUnknownForNull() {
        assertEquals("unknown", KafkaRecordSupport.extractEventId(null));
    }
}
