# API

Harici HTTP sözleşmesi **`/api/v1/**`** üzerinden **api-gateway** ile sunulur. Servis etkileşimleri: [services.md](services.md). Ortam: [configuration.md](configuration.md).

---

## İstek yaşam döngüsü

```mermaid
sequenceDiagram
  participant C as İstemci
  participant GW as api-gateway
  participant KC as Keycloak
  participant SVC as Hedef servis

  C->>GW: HTTP /api/v1/... + Bearer JWT
  alt Public path
    GW->>SVC: /api/v1/public/**
  else Korumalı path
    GW->>KC: JWKS token doğrula
    GW->>GW: Rate limit Redis
    GW->>GW: Route eşleştir order
    GW->>SVC: Proxy + user headers
    alt Circuit open
      GW-->>C: 503 fallback
    else
      SVC-->>GW: yanıt
      GW-->>C: JSON
    end
  end
```

---

## Temel kurallar

- Harici sözleşme: **`/api/v1/**`**
- Kimlik: `Authorization: Bearer <access_token>` (Keycloak realm `finance`)
- Yerel Docker giriş noktası: **http://localhost:8080** (api-gateway)
- Public uçlar: `/api/v1/public/**` (kayıt, login tamamlama vb.)

## Gateway yönlendirme

Route **order** değeri düşük olan önce eşleşir (`GatewayRoutesConfig`).

### Route karar ağacı

```mermaid
flowchart TD
  START["Gelen path /api/v1/..."]
  START --> PUB{"/public/" ?}
  PUB -->|evet| FA1[finance-api]
  PUB -->|hayır| EUR{"/market/eurobonds/tr" ?}
  EUR -->|evet| FA2[finance-api]
  EUR -->|hayır| OVR{overview veya insights ?}
  OVR -->|evet| FA3[finance-api]
  OVR -->|hayır| RATES{"/rates/" ?}
  RATES -->|evet| MDS1[market-data-service]
  RATES -->|hayır| NEWSENR{enriched veya favorites ?}
  NEWSENR -->|evet| FA4[finance-api]
  NEWSENR -->|hayır| NEWS{"/news/" ?}
  NEWS -->|evet| NS[news-service]
  NEWS -->|hayır| MKT{"/market/" ?}
  MKT -->|evet| MDS2[market-data-service]
  MKT -->|hayır| ANA{"/analytics/" ?}
  ANA -->|evet| AS[analytics-service]
  ANA -->|hayır| FA5[finance-api catch-all]
```

### Route tablosu

| Path öneki | Hedef servis |
|------------|--------------|
| `/api/v1/public/**` | finance-api |
| `/api/v1/market/overview`, `/api/v1/market/insights` | finance-api |
| `/api/v1/market/eurobonds/tr/**` | finance-api |
| `/api/v1/market/**` | market-data-service |
| `/api/v1/rates/**` | market-data-service |
| `/api/v1/news/enriched/**`, `/api/v1/news/favorites/**` | finance-api |
| `/api/v1/news/**` | news-service |
| `/api/v1/analytics/**` | analytics-service |
| `/api/v1/**`, `/health` | finance-api (rate limit + circuit breaker) |

Circuit breaker fallback: `/fallback/market`, `/fallback/news`, `/fallback/analytics`.

### Circuit breaker akışı

```mermaid
stateDiagram-v2
  [*] --> Closed
  Closed --> Open: hata eşiği aşıldı
  Open --> HalfOpen: bekleme süresi doldu
  HalfOpen --> Closed: probe başarılı
  HalfOpen --> Open: probe başarısız
  Open --> Fallback: istek geldi
  Fallback --> [*]: forward /fallback/market news analytics
```

| Circuit | Fallback path | Downstream |
|---------|---------------|------------|
| `marketCircuitBreaker` | `/fallback/market` | market-data-service |
| `newsCircuitBreaker` | `/fallback/news` | news-service |
| `analyticsCircuitBreaker` | `/fallback/analytics` | analytics-service |
| `financeCircuitBreaker` | — | finance-api (rate limit) |

## Kimlik doğrulama akışı

```mermaid
flowchart LR
  subgraph client [İstemci]
    SPA[frontend-web]
  end

  subgraph auth [Kimlik]
    KC[Keycloak realm finance]
    GW[api-gateway JWT]
  end

  subgraph api [API]
    FA[finance-api]
    MDS[market-data-service]
  end

  SPA -->|OIDC| KC
  SPA -->|access_token| GW
  GW -->|JWKS| KC
  GW -->|Bearer + headers| FA
  GW --> MDS
```

Public uçlar (`/api/v1/public/**`) gateway’de JWT zorunluluğu route’a göre değişebilir; kayıt/giriş uçları finance-api’dedir.

## Geliştirme proxy (frontend)

| Mod | Env | Davranış |
|-----|-----|----------|
| Tek proxy (Docker) | `VITE_DEV_PROXY_GATEWAY=true` | Tüm `/api` → gateway |
| Varsayılan yerel | — | `/api` → finance-api (8080); market BFF üzerinden |
| Doğrudan MDS | `VITE_DEV_MARKET_DIRECT_TO_MDS=true` | `/api/v1/market` → 8082 (debug) |

## OpenAPI / Swagger

### Swagger birleştirme

```mermaid
flowchart TB
  DEV[Geliştirici tarayıcı]
  GW[api-gateway :8080]
  SW[swagger-ui.html]

  subgraph backends [OpenAPI kaynakları]
    FA[finance-api /v3/api-docs]
    MDS[market-data-service]
    NS[news-service]
    AS[analytics-service]
    NOTIF[notification-service]
    LOG[log-consumer-service]
  end

  DEV --> SW
  SW --> GW
  GW -->|"/services/finance/..."| FA
  GW -->|"/services/market/..."| MDS
  GW -->|"/services/news/..."| NS
  GW -->|"/services/analytics/..."| AS
  GW --> NOTIF
  GW --> LOG
```

- UI (gateway üzerinden): http://localhost:8080/swagger-ui.html
- Birleşik dokümanlar: `/services/{finance|market|news|analytics|notification|logs}/v3/api-docs`
- Her servis kendi `application-openapi.yml` ile springdoc kullanır

Gateway test profilinde route’lar farklı olabilir (`TestGatewayRoutesConfig`).

## Örnek istekler

```http
GET /api/v1/market/instruments?assetClass=STOCK
Authorization: Bearer eyJ...
```

```http
GET /api/v1/public/health
```

Public kayıt/giriş uçları `finance-api` `PublicRegistrationController`, `PublicAuthenticationController` altındadır; tam liste için Swagger kullanın.

## Hata ve limit

- Gateway Redis rate limiter: kullanıcı başına (`userIdKeyResolver`)
- 429 / 503: rate limit veya circuit breaker açık
- `finance-api` dev profilde `app.expose-internal-errors: true` — üretimde kapatın

## log-consumer ve notification HTTP

Gateway yapılandırmasında `log-consumer` ve `notification` base URI tanımlıdır; birincil portal trafiği `/api/v1` altında finance / market / news / analytics üzerinden gider. Bu servislerin REST yüzeyi çoğunlukla iç/diagnostic amaçlıdır; OpenAPI path’leri gateway’de `/services/notification/...` ve `/services/logs/...` ile expose edilir.

## Versiyonlama

Şu an yalnızca `v1` yayımlıdır. Kırıcı değişikliklerde yeni prefix (`/api/v2`) ve gateway route eklenmesi beklenir.

---

## Hata kodları (özet)

```mermaid
flowchart TD
  REQ[API isteği]
  REQ --> RL{Rate limit?}
  RL -->|evet| E429[429 Too Many Requests]
  RL -->|hayır| AUTH{JWT geçerli?}
  AUTH -->|hayır| E401[401 Unauthorized]
  AUTH -->|evet| CB{Circuit open?}
  CB -->|evet| E503[503 + fallback body]
  CB -->|hayır| SVC[Servis işle]
  SVC --> OK[200 / 4xx iş kuralı]
```

---

## İlgili belgeler

| Belge | İçerik |
|-------|--------|
| [services/api-gateway.md](services/api-gateway.md) | Gateway detay |
| [configuration.md](configuration.md) | JWT, CORS env |
| [development.md](development.md) | Vite proxy |
