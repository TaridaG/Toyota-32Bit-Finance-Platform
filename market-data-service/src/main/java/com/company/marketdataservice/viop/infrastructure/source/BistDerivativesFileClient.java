package com.company.marketdataservice.viop.infrastructure.source;

import com.company.marketdataservice.bootstrap.config.ViopMarketProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * BIST türev veri dosyalarını HTTP üzerinden indiren infrastructure client bileşeni.
 */
@Component
public class BistDerivativesFileClient {

    private final ViopMarketProperties properties;
    private final HttpClient client;

    public BistDerivativesFileClient(ViopMarketProperties properties) {
        this.properties = properties;
        this.client =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(Math.max(1000, properties.getConnectTimeoutMs())))
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build();
    }

    /**
     * Verilen URL'den dosyayı indirir ve içerik tipi ile ham byte gövdesini döner.
     */
    public DownloadedFile fetch(String url) throws IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofMillis(Math.max(2000, properties.getReadTimeoutMs())))
                        .header("Accept", "*/*")
                        .header("User-Agent", "FinanceMarketDataService/1.0")
                        .GET()
                        .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected status=" + response.statusCode() + " url=" + url);
        }
        String path = URI.create(url).getPath();
        String fileName = path == null || path.isBlank() ? "unknown.bin" : path.substring(path.lastIndexOf('/') + 1);
        String contentType =
                response.headers().firstValue("content-type").orElse("application/octet-stream").toLowerCase(Locale.ROOT);
        return new DownloadedFile(url, fileName, contentType, response.body());
    }

    public record DownloadedFile(String url, String fileName, String contentType, byte[] body) {}
}

