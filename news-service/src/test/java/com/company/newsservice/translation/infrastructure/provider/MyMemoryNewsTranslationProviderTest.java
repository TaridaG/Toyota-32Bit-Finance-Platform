package com.company.newsservice.translation.infrastructure.provider;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MyMemoryNewsTranslationProviderTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void translate_returnsEmptyForBlankInput() {
        MyMemoryNewsTranslationProvider provider = providerWithBaseUrl("http://127.0.0.1:" + port);

        assertEquals("", provider.translate("  ", "en"));
    }

    @Test
    void translate_skipsApiWhenSourceAndTargetLanguageMatch() {
        MyMemoryNewsTranslationProvider provider = providerWithBaseUrl("http://127.0.0.1:" + port);

        assertEquals("Already English", provider.translate("Already English", "en"));
    }

    @Test
    void translate_returnsTranslatedTextFromApi() {
        serveJson("""
                {
                  "responseStatus": 200,
                  "responseData": {
                    "translatedText": "Merhaba dünya"
                  }
                }
                """);

        MyMemoryNewsTranslationProvider provider = providerWithBaseUrl("http://127.0.0.1:" + port);

        assertEquals("Merhaba dünya", provider.translate("Hello world", "tr"));
    }

    @Test
    void translate_returnsOriginalWhenApiFails() {
        serveJson("""
                {
                  "responseStatus": 429,
                  "responseData": {
                    "translatedText": "MYMEMORY WARNING: USAGE LIMIT"
                  }
                }
                """);

        MyMemoryNewsTranslationProvider provider = providerWithBaseUrl("http://127.0.0.1:" + port);
        String original = "Hello world";

        assertEquals(original, provider.translate(original, "tr"));
    }

    @Test
    void providerId_isMymemory() {
        MyMemoryNewsTranslationProvider provider = providerWithBaseUrl("http://127.0.0.1:" + port);
        assertEquals("mymemory", provider.providerId());
    }

    private MyMemoryNewsTranslationProvider providerWithBaseUrl(String baseUrl) {
        NewsProperties properties = new NewsProperties();
        properties.getTranslation().getMymemory().setBaseUrl(baseUrl);
        return new MyMemoryNewsTranslationProvider(properties);
    }

    private void serveJson(String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        server.createContext("/get", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }
}
