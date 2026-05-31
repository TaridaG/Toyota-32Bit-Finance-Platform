# API

Die externe HTTP-Schnittstelle wird über **api-gateway** unter **`/api/v1/**`** bereitgestellt. Service-Interaktionen: [services.md](services.md). Umgebung: [configuration.md](configuration.md).

---

## Anfrage-Lebenszyklus

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

## Grundregeln

- Externe Schnittstelle: **`/api/v1/**`**
- Identität: `Authorization: Bearer <access_token>` (Keycloak-Realm `finance`)
- Lokaler Docker-Einstiegspunkt: **http://localhost:8080** (api-gateway)
- Öffentliche Endpunkte: `/api/v1/public/**` (Registrierung, Login-Abschluss usw.)

## Gateway-Routing

Routes mit niedrigerem **order**-Wert werden zuerst abgeglichen (`GatewayRoutesConfig`).

### Route-Entscheidungsbaum

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

### Routing-Tabelle

| Pfadpräfix | Zielservice |
|------------|--------------|
| `/api/v1/public/**` | finance-api |
| `/api/v1/market/overview`, `/api/v1/market/insights` | finance-api |
| `/api/v1/market/eurobonds/tr/**` | finance-api |
| `/api/v1/market/**` | market-data-service |
| `/api/v1/rates/**` | market-data-service |
| `/api/v1/news/enriched/**`, `/api/v1/news/favorites/**` | finance-api |
| `/api/v1/news/**` | news-service |
| `/api/v1/analytics/**` | analytics-service |
| `/api/v1/**`, `/health` | finance-api (Rate Limit + Circuit Breaker) |

Circuit-Breaker-Fallback: `/fallback/market`, `/fallback/news`, `/fallback/analytics`.

### Circuit-Breaker-Ablauf

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

## Authentifizierungsablauf

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

Öffentliche Endpunkte (`/api/v1/public/**`) können je nach Route unterschiedliche JWT-Anforderungen am Gateway haben; Registrierungs- und Login-Endpunkte liegen in finance-api.

## Entwicklungs-Proxy (Frontend)

| Modus | Env | Verhalten |
|-----|-----|----------|
| Einzelner Proxy (Docker) | `VITE_DEV_PROXY_GATEWAY=true` | Gesamtes `/api` → Gateway |
| Standard lokal | — | `/api` → finance-api (8080); Markt über BFF |
| Direkt MDS | `VITE_DEV_MARKET_DIRECT_TO_MDS=true` | `/api/v1/market` → 8082 (Debug) |

## OpenAPI / Swagger

### Swagger-Zusammenführung

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

- UI (über Gateway): http://localhost:8080/swagger-ui.html
- Zusammengeführte Dokumente: `/services/{finance|market|news|analytics|notification|logs}/v3/api-docs`
- Jeder Service nutzt springdoc mit eigener `application-openapi.yml`

Im Gateway-Testprofil können die Routes abweichen (`TestGatewayRoutesConfig`).

## Beispielanfragen

```http
GET /api/v1/market/instruments?assetClass=STOCK
Authorization: Bearer eyJ...
```

```http
GET /api/v1/public/health
```

Öffentliche Registrierungs- und Login-Endpunkte befinden sich unter `PublicRegistrationController` und `PublicAuthenticationController` in finance-api; die vollständige Liste finden Sie in Swagger.

## Fehler und Limits

- Gateway-Redis-Rate-Limiter: pro Benutzer (`userIdKeyResolver`)
- 429 / 503: Rate Limit oder offener Circuit Breaker
- `finance-api` im Dev-Profil `app.expose-internal-errors: true` — in Produktion deaktivieren

## log-consumer und notification HTTP

In der Gateway-Konfiguration sind Base-URIs für `log-consumer` und `notification` definiert; der primäre Portal-Traffic läuft unter `/api/v1` über finance / market / news / analytics. Die REST-Oberfläche dieser Services dient überwiegend internen/diagnostischen Zwecken; OpenAPI-Pfade werden am Gateway unter `/services/notification/...` und `/services/logs/...` bereitgestellt.

## Versionierung

Derzeit ist nur `v1` veröffentlicht. Bei Breaking Changes wird ein neues Präfix (`/api/v2`) und eine Gateway-Route erwartet.

---

## Fehlercodes (Übersicht)

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

## Verwandte Dokumente

| Dokument | Inhalt |
|-------|--------|
| [services/api-gateway.md](services/api-gateway.md) | Gateway-Details |
| [configuration.md](configuration.md) | JWT, CORS env |
| [development.md](development.md) | Vite-Proxy |
