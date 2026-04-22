package com.company.logconsumer.config;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.opensearch.client.RestClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

@Configuration
public class OpenSearchConfig {

    @Bean
    public RestClient restClient(
            @Value("${opensearch.host}") String host,
            @Value("${opensearch.port}") int port,
            @Value("${opensearch.scheme:http}") String scheme,
            @Value("${opensearch.username:}") String username,
            @Value("${opensearch.password:}") String password
    ) {
        return RestClient.builder(new HttpHost(host, port, scheme))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    if (!username.isBlank()) {
                        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(
                                AuthScope.ANY,
                                new UsernamePasswordCredentials(username, password)
                        );
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }

                    if ("https".equalsIgnoreCase(scheme)) {
                        try {
                            SSLContext sslContext = SSLContexts.custom()
                                    .loadTrustMaterial(null, (chain, authType) -> true)
                                    .build();
                            httpClientBuilder.setSSLContext(sslContext);
                            httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                        } catch (Exception ex) {
                            throw new IllegalStateException("Failed to initialize OpenSearch SSL context", ex);
                        }
                    }
                    return httpClientBuilder;
                })
                .build();
    }

    @Bean
    public OpenSearchClient openSearchClient(RestClient restClient) {
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
    }
}