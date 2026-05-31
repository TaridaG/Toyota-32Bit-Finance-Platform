# Dokümantasyon

Finance Platform monoreposu için teknik rehberler. Güncel mimari ve yapılandırma kaynağı olarak bu klasörü kullanın; `frontend-web/README.md` yalnızca Vite şablon notları içerir.

## Rehberler

| Belge | Ne zaman okunur? |
|-------|------------------|
| [architecture.md](architecture.md) | Sistem bileşenleri, Kafka, güvenlik ve veri akışını anlamak |
| [getting-started.md](getting-started.md) | İlk kurulum (Docker veya hibrit yerel) |
| [services.md](services.md) | Hangi servis ne yapar, hangi portta dinler |
| [api.md](api.md) | `/api/v1` yönlendirme, Swagger, kimlik doğrulama |
| [development.md](development.md) | Maven, profiller, test, frontend proxy |
| [configuration.md](configuration.md) | `Docker/.env` ve servis ortam değişkenleri |
| [observability.md](observability.md) | Prometheus, Grafana, Jaeger, OpenSearch |

## Hızlı bağlantılar

- Kök özet: [../README.md](../README.md)
- Docker Compose: [../Docker/docker-compose.yml](../Docker/docker-compose.yml)
- Ortam şablonu: [../Docker/.env.example](../Docker/.env.example)
- Keycloak realm: [../Docker/keycloak/realm-finance.json](../Docker/keycloak/realm-finance.json)
- Frontend env: [../frontend-web/.env.example](../frontend-web/.env.example)

## Dokümantasyonu güncelleme

Mimari veya port değişikliği yaptığınızda ilgili `docs/*.md` dosyasını aynı PR’da güncelleyin. Gateway route sırası değiştiyse [api.md](api.md) ve [architecture.md](architecture.md) birlikte gözden geçirilmelidir.
