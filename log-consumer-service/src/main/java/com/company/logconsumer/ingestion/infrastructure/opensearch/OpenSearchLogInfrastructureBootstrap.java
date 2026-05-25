package com.company.logconsumer.ingestion.infrastructure.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.indices.PutIndexTemplateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ensures OpenSearch index template and ISM retention policy exist for application log indices.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSearchLogInfrastructureBootstrap {

    private static final String TEMPLATE_NAME = "application-logs-template";
    private static final String ISM_POLICY_ID = "application-logs-retention";

    private final OpenSearchClient openSearchClient;
    private final RestClient restClient;

    @Value("${opensearch.index-prefix:application-logs}")
    private String indexPrefix;

    @Value("${opensearch.log-retention-days:30}")
    private int retentionDays;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureInfrastructure() {
        ensureIndexTemplate();
        ensureRetentionPolicy();
    }

    private void ensureIndexTemplate() {
        String pattern = indexPrefix + "-*";
        try {
            PutIndexTemplateRequest request = PutIndexTemplateRequest.of(builder -> builder
                    .name(TEMPLATE_NAME)
                    .indexPatterns(pattern)
                    .priority(100)
                    .template(template -> template
                            .mappings(mappings -> mappings
                                    .properties("@timestamp", Property.of(p -> p.date(d -> d)))
                                    .properties("level", Property.of(p -> p.keyword(k -> k)))
                                    .properties("service", Property.of(p -> p.keyword(k -> k)))
                                    .properties("message", Property.of(p -> p.text(t -> t)))
                                    .properties("traceId", Property.of(p -> p.keyword(k -> k)))
                                    .properties("spanId", Property.of(p -> p.keyword(k -> k)))
                                    .properties("correlationId", Property.of(p -> p.keyword(k -> k)))
                                    .properties("logger_name", Property.of(p -> p.keyword(k -> k)))
                                    .properties("thread_name", Property.of(p -> p.keyword(k -> k)))
                                    .properties("stack_trace", Property.of(p -> p.text(t -> t)))
                            )
                    )
            );
            openSearchClient.indices().putIndexTemplate(request);
            log.info("OpenSearch index template '{}' registered for pattern '{}'", TEMPLATE_NAME, pattern);
        } catch (Exception ex) {
            log.warn("Failed to register OpenSearch index template for '{}': {}", pattern, ex.getMessage());
        }
    }

    private void ensureRetentionPolicy() {
        String pattern = indexPrefix + "-*";
        String policyJson = """
                {
                  "policy": {
                    "description": "Delete application log indices after retention period",
                    "default_state": "hot",
                    "states": [
                      {
                        "name": "hot",
                        "actions": [],
                        "transitions": [
                          {
                            "state_name": "delete",
                            "conditions": {
                              "min_index_age": "%dd"
                            }
                          }
                        ]
                      },
                      {
                        "name": "delete",
                        "actions": [
                          {
                            "delete": {}
                          }
                        ]
                      }
                    ],
                    "ism_template": [
                      {
                        "index_patterns": ["%s"],
                        "priority": 100
                      }
                    ]
                  }
                }
                """.formatted(retentionDays, pattern);

        Request request = new Request("PUT", "/_plugins/_ism/policies/" + ISM_POLICY_ID);
        request.setJsonEntity(policyJson);
        try {
            Response response = restClient.performRequest(request);
            log.info(
                    "OpenSearch ISM policy '{}' registered for '{}' (retention {} days, status {})",
                    ISM_POLICY_ID,
                    pattern,
                    retentionDays,
                    response.getStatusLine().getStatusCode()
            );
        } catch (ResponseException ex) {
            int status = ex.getResponse().getStatusLine().getStatusCode();
            if (status == 409) {
                log.info("OpenSearch ISM policy '{}' already exists", ISM_POLICY_ID);
                return;
            }
            log.warn("Failed to register OpenSearch ISM policy '{}': HTTP {}", ISM_POLICY_ID, status);
        } catch (Exception ex) {
            log.warn("Failed to register OpenSearch ISM policy '{}': {}", ISM_POLICY_ID, ex.getMessage());
        }
    }
}
