package com.company.logconsumer.bootstrap.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Log4j2KafkaConfigurationTest {

    @Test
    void log4j2SpringXml_declaresKafkaAppenderOnAppLogsTopic() throws Exception {
        Path mainConfig = Path.of("src/main/resources/log4j2-spring.xml");
        String xml = Files.readString(mainConfig, StandardCharsets.UTF_8);
        assertTrue(xml.contains("<Kafka name=\"Kafka\" topic=\"app.logs\""),
                "Kafka appender must publish to app.logs");
        assertTrue(xml.contains("log4j2-kafka-template.json"),
                "Kafka appender must use JsonTemplateLayout template");
        assertTrue(xml.contains("<AppenderRef ref=\"AsyncKafka\"/>"),
                "Root logger must ship logs to Kafka");
    }

    @Test
    void log4j2KafkaTemplate_declaresServiceAndTimestampFields() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("log4j2-kafka-template.json")) {
            assertNotNull(in, "log4j2-kafka-template.json must be on classpath");
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"@timestamp\""));
            assertTrue(json.contains("\"service\""));
            assertTrue(json.contains("spring.application.name"));
        }
    }
}
