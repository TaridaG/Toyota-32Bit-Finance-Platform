package com.company.finance_api.bootstrap.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Log4j2KafkaConfigurationTest {

    @Test
    void log4j2SpringXml_publishesToAppLogsTopic() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/log4j2-spring.xml"), StandardCharsets.UTF_8);
        assertTrue(xml.contains("topic=\"app.logs\""));
        assertTrue(xml.contains("AsyncKafka"));
        assertTrue(xml.contains("log4j2-kafka-template.json"));
    }
}
