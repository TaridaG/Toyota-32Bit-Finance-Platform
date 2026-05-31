# Mimari

## Genel bakış

Platform, **edge** katmanında tek HTTP girişi (`api-gateway`), **domain** katmanında `finance-api` (portal BFF ve iş kuralları) ve **uzmanlaşmış** mikroservislerden oluşur. Olaylar çoğunlukla **Kafka** üzerinden akar; kalıcı veri **PostgreSQL**’de (servis başına Flyway şeması/tablosu).

```mermaid
flowchart LR
  subgraph clients [İstemciler]
    WEB[frontend-web]
  end

  subgraph edge [Edge]
    GW[api-gateway]
    KC[Keycloak]
  end

  subgraph core [Çekirdek]
    FA[finance-api]
  end

  subgraph specialized [Uzman servisler]
    MDS[market-data-service]
    NS[news-service]
    AS[analytics-service]
    NOTIF[notification-service]
    LOG[log-consumer-service]
  end

  subgraph infra [Altyapı]
    PG[(PostgreSQL)]
    KF[Kafka]
    RD[Redis]
    OS[OpenSearch]
  end

  WEB -->|HTTPS /api| GW
  WEB -->|OIDC| KC
  GW -->|JWT doğrulama| KC
  GW --> FA
  GW --> MDS
  GW --> NS
  GW --> AS
  FA --> PG
  MDS --> PG
  NS --> PG
  AS --> PG
  NOTIF --> PG
  FA --> KF
  MDS --> KF
  NS --> KF
  AS --> KF
  NOTIF --> KF
  LOG --> KF
  LOG --> OS
  GW --> RD
  FA --> RD
```

## İstek yolu (tipik)

1. Tarayıcı `frontend-web` üzerinden `/api/v1/...` çağırır (Vite dev proxy veya Docker’da gateway’e yönlendirilir).
2. **api-gateway** JWT’yi Keycloak JWKS ile doğrular; kullanıcı bağlamını downstream header’lara aktarır.
3. Path önekine göre hedef servis seçilir (özel route’lar genel `finance-api` route’undan önce gelir — bkz. [api.md](api.md)).
4. `finance-api` gerektiğinde `market-data-service`’e HTTP proxy veya Kafka tüketimi yapar.

```mermaid
sequenceDiagram
  participant UI as frontend-web
  participant GW as api-gateway
  participant FA as finance-api
  participant MDS as market-data-service

  UI->>GW: GET /api/v1/...
  GW->>FA: veya MDS route
  alt BFF agregasyon
    FA->>MDS: HTTP
    MDS-->>FA: veri
  end
  FA-->>GW: JSON
  GW-->>UI: response
```

## Katmanlı mimari

```mermaid
flowchart TB
  subgraph presentation [Sunum]
    FE[frontend-web React]
  end

  subgraph edge [Edge]
    GW[api-gateway]
    KC[Keycloak]
  end

  subgraph domain [Domain ve BFF]
    FA[finance-api]
  end

  subgraph specialized [Uzman servisler]
    MDS[market-data-service]
    NS[news-service]
    AS[analytics-service]
    NOTIF[notification-service]
  end

  subgraph data [Veri ve mesaj]
    PG[(PostgreSQL)]
    KF[Kafka]
    RD[Redis]
    OS[OpenSearch]
  end

  FE --> GW
  FE --> KC
  GW --> FA
  GW --> MDS
  GW --> NS
  GW --> AS
  FA --> PG
  MDS --> PG
  FA --> KF
  MDS --> KF
  KF --> NOTIF
  KF --> LOG[log-consumer]
  LOG --> OS
```

Detaylı servis diyagramları: [services.md](services.md) · Servis rehberleri: [services/README.md](services/README.md).

## Servis sorumlulukları

| Servis | Rol | Detaylı rehber |
|--------|-----|----------------|
| **api-gateway** | TLS sonlandırma (deploy’da), CORS, rate limiting (Redis), circuit breaker, route aggregation | [services/api-gateway.md](services/api-gateway.md) |
| **finance-api** | Kullanıcı, portföy, alarm, grafik kayıtları, admin KPI, kayıt/MFA, haber zenginleştirme proxy, piyasa özeti | [services/finance-api.md](services/finance-api.md) |
| **market-data-service** | Enstrüman kataloğu, canlı/geçmiş fiyat, TCMB EVDS (faiz, tahvil, TL mevduat), sağlayıcı entegrasyonları | [services/market-data-service.md](services/market-data-service.md) |
| **news-service** | RSS çekme, saklama, çeviri, ham haber API | [services/news-service.md](services/news-service.md) |
| **analytics-service** | `market.price.updated` tüketimi, RSI vb. göstergeler, insight üretimi | [services/analytics-service.md](services/analytics-service.md) |
| **notification-service** | Alarm e-postası, login uyarısı, watchlist, insight ve haber eşleşmesi | [services/notification-service.md](services/notification-service.md) |
| **log-consumer-service** | `app.logs` topic → OpenSearch indeksleme | [services/log-consumer-service.md](services/log-consumer-service.md) |
| **frontend-web** | SPA, Keycloak ile giriş, portal sayfaları | [services/frontend-web.md](services/frontend-web.md) |

Tüm servis rehberleri: [services/README.md](services/README.md).

## Olay güdümlü akış (özet)

| Topic | Üreten (ör.) | Tüketen (ör.) |
|-------|----------------|----------------|
| `market.price.updated` | market-data-service | analytics-service, finance-api |
| `market.fx.snapshot.updated` | market-data-service | finance-api, analytics-service |
| `market.fund.snapshot.updated` | market-data-service | finance-api |
| `alarm-triggered` | finance-api (outbox) | notification-service |
| `watchlist.item.added` / `removed` | finance-api (outbox) | notification-service |
| `login-security.alert` | finance-api (outbox) | notification-service |
| `transaction-executed` | finance-api (outbox) | (domain) |
| `analytics.insight.simple` | analytics-service | notification-service |
| `news.instrument.matched` | news-service | notification-service |
| `app.logs` | Tüm Spring servisler (Log4j2) | log-consumer-service |

`finance-api` kritik domain olaylarını **transactional outbox** ile Kafka’ya yayınlar (`OutboxPublisherScheduler`).

### Transactional outbox deseni

```mermaid
sequenceDiagram
  participant SVC as finance-api Service
  participant PG as PostgreSQL
  participant OB as OutboxPublisherScheduler
  participant KF as Kafka
  participant CON as notification-service

  SVC->>PG: BEGIN iş kuralı + INSERT outbox
  SVC->>PG: COMMIT
  OB->>PG: SELECT pending outbox
  OB->>KF: publish alarm-triggered vb.
  OB->>PG: mark published
  KF->>CON: consume
```

### Kafka topic haritası (görsel)

```mermaid
flowchart LR
  subgraph producers [Üreticiler]
    MDS[market-data-service]
    FA[finance-api outbox]
    AS[analytics-service]
    NS[news-service]
    LOGS[Tüm servisler Log4j2]
  end

  subgraph topics [Kafka topics]
    T1[market.price.updated]
    T2[market.fx.snapshot.updated]
    T3[market.fund.snapshot.updated]
    T4[alarm-triggered]
    T5[analytics.insight.simple]
    T6[news.instrument.matched]
    T7[app.logs]
  end

  subgraph consumers [Tüketiciler]
    FA2[finance-api]
    AS2[analytics-service]
    NOTIF[notification-service]
    LCS[log-consumer-service]
  end

  MDS --> T1 --> FA2
  MDS --> T1 --> AS2
  MDS --> T2 --> FA2
  MDS --> T3 --> FA2
  FA --> T4 --> NOTIF
  AS --> T5 --> NOTIF
  NS --> T6 --> NOTIF
  LOGS --> T7 --> LCS
```

## Veri ve şema

- Tek PostgreSQL instance’ı (`finance` veritabanı); Keycloak için ayrı `keycloak` DB ([`Docker/postgres/init`](../../Docker/postgres/init)).
- Flyway tablo adları servise özel (ör. `finance_flyway_schema_history`, `mds_flyway_schema_history`, `analytics_flyway_schema_history`).
- Yerel `market-data-service` **dev** profili varsayılan olarak bellek içi H2 kullanabilir; Docker/production PostgreSQL kullanır.

## Veri kalıcılığı

```mermaid
flowchart TB
  PG[(PostgreSQL instance)]

  PG --> DB1[(finance DB)]
  PG --> DB2[(keycloak DB)]

  DB1 --> FW1[finance_flyway_schema_history]
  DB1 --> FW2[mds_flyway_schema_history]
  DB1 --> FW3[analytics_flyway_schema_history]
  DB1 --> FW4[news + notification tabloları]

  FA[finance-api] --> FW1
  MDS[market-data-service] --> FW2
  AS[analytics-service] --> FW3
```

## Güvenlik

```mermaid
flowchart LR
  User[Kullanıcı]
  SPA[frontend-web]
  KC[Keycloak realm finance]
  GW[api-gateway JWT]
  API[Backend servisler]

  User --> SPA
  SPA -->|OIDC| KC
  SPA -->|Bearer| GW
  GW -->|JWKS| KC
  GW -->|resource server| API
  API -->|MFA secrets| FA_SEC[finance-api APP_MFA_*]
```

- **Keycloak** realm: `finance`, public client: `finance-gateway` (SPA), confidential: `finance-portal` (direct grant / backend).
- Gateway ve servisler **OAuth2 Resource Server** (JWT).
- Geliştirmede `APP_SECURITY_ALLOW_HEADER_USER_FALLBACK` ile header tabanlı kullanıcı çözümlemesi açılabilir; üretimde kapatılmalıdır.
- Portal MFA (TOTP), güvenilir cihaz çerezi ve kayıt e-posta doğrulaması `finance-api` içindedir.

## Gözlemlenebilirlik

Metrikler: Spring Actuator + Prometheus scrape. Trace: OTLP → Jaeger. Loglar: JSON → Kafka `app.logs` → OpenSearch. Ayrıntı: [observability.md](observability.md).

## Dağıtım notu

Üretim topolojisi bu repoda tanımlı değil; Docker Compose geliştirme/demo ortamı içindir. Gateway `JWT_ISSUER_URI` tarayıcıdan erişilebilir issuer ile uyumlu olmalıdır (`localhost:8085`).
