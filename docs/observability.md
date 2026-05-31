# Gözlemlenebilirlik

Docker Compose ile birlikte gelen observability stack’i geliştirme ve demo içindir.

## Bileşenler

| Araç | URL | Amaç |
|------|-----|------|
| **Prometheus** | http://localhost:9090 | Metrik toplama, target health |
| **Grafana** | http://localhost:3000 | Dashboard (admin / admin) |
| **Jaeger** | http://localhost:16686 | Dağıtık trace |
| **OpenSearch** | https://localhost:9200 | Log indeksi (`admin` / `123456789`) |
| **OpenSearch Dashboards** | http://localhost:5601 | Log arama (`kibanaserver` / `kibanaserver`) |

> Host port **9090** hem Prometheus hem (yerel profilde) api-gateway `dev` ile çakışabilir. Tam Compose’ta gateway **8080**’dedir.

## Metrikler

Her Spring servis:

```http
GET /actuator/health
GET /actuator/prometheus
```

Prometheus scrape yapılandırması: [`Docker/prometheus/prometheus.yml`](../Docker/prometheus/prometheus.yml).

Grafana provisioning: [`Docker/grafana/provisioning`](../Docker/grafana/provisioning) — varsayılan home dashboard: *Finance Platform — Overview*.

## Trace

Servisler `management.otlp.tracing.endpoint` ile Jaeger’a span gönderir (ör. `http://jaeger:4318/v1/traces`).

Örnekleme: `TRACING_SAMPLE_PROBABILITY` (varsayılan `0.1`).

Kafka listener observation: `finance-api` `application.yml` içinde etkin.

## Log pipeline

1. Uygulamalar **Log4j2** JSON layout ile Kafka topic **`app.logs`**’a yazar.
2. **log-consumer-service** tüketir ve **OpenSearch**’e indeksler (`application-logs-*`).
3. Retention: `OPENSEARCH_LOG_RETENTION_DAYS` (varsayılan 30).

### Dashboards ilk kurulum

1. http://localhost:5601 açın
2. Stack Management → Index patterns → Create
3. Pattern: `application-logs-*`
4. Time field: `@timestamp`

## Sistem zekası (opsiyonel)

`log-consumer-service` OpenSearch üzerinde admin/operasyon sorguları için HTTP uçları sağlar (ör. system intelligence controller testleri mevcut).

## Sağlık kontrolü

```bash
docker compose ps
curl -s http://localhost:8080/actuator/health | jq .
```

Prometheus **Targets** sayfasında tüm job’ların `UP` olması beklenir.

## Sorun giderme

| Sorun | Çözüm |
|-------|--------|
| OpenSearch unhealthy | RAM artırın; ilk boot 60s+ bekleyin |
| Log indeksi yok | `log-consumer-service` ve Kafka topic; MDS/finance log seviyesi |
| Grafana boş | Prometheus dependency; provisioning volume mount |
| Eski OpenSearch şifresi | `opensearch_data` volume silip yeniden oluşturun — `.env.example` notları |

Yapılandırma özeti: [configuration.md](configuration.md).
