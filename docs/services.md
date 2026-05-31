# Servisler

Maven modülleri parent `finance-platform` (`pom.xml`) altında toplanır.

## Özet tablo

| Modül | Açıklama | Docker iç port | Host port (Compose) |
|-------|----------|----------------|---------------------|
| `api-gateway` | Tek API girişi, JWT, rate limit | 8080 | **8080** |
| `finance-api` | Portal BFF, portföy, auth, admin | 8080 | (yalnızca internal) |
| `market-data-service` | Piyasa verisi, EVDS, katalog | 8080 | (internal) |
| `analytics-service` | Gösterge / insight işleme | 8080 | (internal) |
| `news-service` | Haber API & ingestion | 8082 | (internal) |
| `notification-service` | E-posta & alarm tüketimi | 8086 | (internal) |
| `log-consumer-service` | Log indeksleme | 8087 | (internal) |
| `frontend-web` | React SPA | 5173 | **5173** |

## Yerel geliştirme portları

| Servis | Profil | Port |
|--------|--------|------|
| api-gateway | `dev` | 9090 |
| api-gateway | `docker` | 8080 |
| finance-api | (varsayılan Spring Boot) | 8080 |
| market-data-service | `dev` | 8082 |
| news-service | (sabit) | 8082 |
| notification-service | (sabit) | 8086 |
| log-consumer-service | (sabit) | 8087 |

`news-service` ve `market-data-service` aynı host portunu kullanır; **ikisini aynı makinede 8082’de birlikte çalıştırmayın**. Docker ağında hostname ile ayrılır.

## finance-api

**Paket alanları (örnek):** `portfolio`, `alarm`, `watchlist`, `chart`, `auth`, `registration`, `mfa`, `admin`, `news` (enrichment), `market` (overview, eurobond proxy), `infocards`, `profile`, `ai`.

**Veri:** PostgreSQL + Flyway (`finance-api/src/main/resources/db/migration`).

**Önemli entegrasyonlar:**

- Keycloak Admin API (kullanıcı/rol)
- `market-data-service` HTTP client
- Kafka producer (outbox) ve consumer (`MarketDataTopics`)
- Redis (cache profili: `cache-redis`)
- Avatar dosyaları: `photos/{userId}/` ([configuration.md](configuration.md))

## market-data-service

Enstrüman kataloğu, scheduler’lar ile fiyat güncelleme, TCMB EVDS (politika faizi, tahvil, TL mevduat), Finnhub/Yahoo/CoinGecko vb.

**Veri:** Docker’da PostgreSQL + `mds_flyway_schema_history`; dev’de H2.

**Kafka:** `market.price.updated`, FX/fund snapshot topic’leri.

## analytics-service

`market.price.updated` dinleyerek teknik göstergeler hesaplar; finance-api ile HTTP (insight / politika).

Ayrı Flyway şeması: `analytics_flyway_schema_history`.

## news-service

RSS kaynaklarından ingestion, çeviri (MyMemory), REST API `/api/v1/news/**` (gateway üzerinden).

Docker’da `read_only` root filesystem; `NEWS_DB_PASSWORD` zorunlu.

## notification-service

Kafka tüketicisi: alarmlar, login uyarıları, watchlist, analytics insight.

SMTP ile e-posta; `MANAGEMENT_HEALTH_MAIL_ENABLED=false` Docker’da mail health kapalı olabilir.

## log-consumer-service

Topic: `app.logs` → OpenSearch indeks öneki `application-logs`.

## api-gateway

Spring Cloud Gateway (reactive). Redis rate limiter, Resilience4j circuit breaker.

Downstream URI’ler `gateway.services.*` ile yapılandırılır (`application.yml`).

## frontend-web

React 19 SPA; routing `src/app/router/index.tsx`.

**Oturum gerektiren sayfalar (`/app/...`):** portföy, analiz, haber, faiz-vadeli, profil, alarmlar, bildirimler, dashboard, harici portföy.

**Herkese açık:** `/`, `/login`, `/register`, `/markets`, `/finansal-okuryazarlik`, `/bank-rates`.

**Admin:** `/admin`, `/app/bilgi-kartlari`.

## Altyapı konteynerleri (Docker)

| Bileşen | Host port |
|---------|-----------|
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka | 9092 |
| Keycloak | 8085 |
| OpenSearch | 9200 |
| OpenSearch Dashboards | 5601 |
| Jaeger UI | 16686 |
| Prometheus | 9090 |
| Grafana | 3000 |
