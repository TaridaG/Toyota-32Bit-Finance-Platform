# Architektur

## Überblick

Die Plattform besteht aus einer einzigen HTTP-Einstiegsschicht (**edge**) (`api-gateway`), der **Domain**-Schicht `finance-api` (Portal-BFF und Geschäftsregeln) und **spezialisierten** Microservices. Ereignisse fließen überwiegend über **Kafka**; persistente Daten liegen in **PostgreSQL** (Flyway-Schema/Tabelle pro Service).

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

## Anfragepfad (typisch)

1. Der Browser ruft über `frontend-web` `/api/v1/...` auf (Vite-Dev-Proxy oder in Docker zum Gateway weitergeleitet).
2. **api-gateway** validiert das JWT mit Keycloak JWKS und leitet den Benutzerkontext an Downstream-Header weiter.
3. Je nach Pfadpräfix wird der Zielservice gewählt (spezielle Routes vor der allgemeinen `finance-api`-Route — siehe [api.md](api.md)).
4. `finance-api` proxyt bei Bedarf per HTTP zu `market-data-service` oder konsumiert Kafka.

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

## Schichtenarchitektur

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

Detaillierte Service-Diagramme: [services.md](services.md) · Service-Handbücher: [services/README.md](services/README.md).

## Service-Verantwortlichkeiten

| Service | Rolle | Detailliertes Handbuch |
|--------|-----|----------------|
| **api-gateway** | TLS-Terminierung (im Deployment), CORS, Rate Limiting (Redis), Circuit Breaker, Route-Aggregation | [services/api-gateway.md](services/api-gateway.md) |
| **finance-api** | Benutzer, Portfolio, Alarme, Chart-Speicherungen, Admin-KPI, Registrierung/MFA, News-Anreicherungs-Proxy, Marktübersicht | [services/finance-api.md](services/finance-api.md) |
| **market-data-service** | Instrumentenkatalog, Live-/Historienpreise, TCMB EVDS (Zinsen, Anleihen, TL-Einlagen), Provider-Integrationen | [services/market-data-service.md](services/market-data-service.md) |
| **news-service** | RSS-Abruf, Speicherung, Übersetzung, Roh-News-API | [services/news-service.md](services/news-service.md) |
| **analytics-service** | Konsumiert `market.price.updated`, RSI u. a. Indikatoren, Insight-Erzeugung | [services/analytics-service.md](services/analytics-service.md) |
| **notification-service** | Alarm-E-Mails, Login-Warnung, Watchlist, Insight- und News-Matching | [services/notification-service.md](services/notification-service.md) |
| **log-consumer-service** | `app.logs` Topic → OpenSearch-Indizierung | [services/log-consumer-service.md](services/log-consumer-service.md) |
| **frontend-web** | SPA, Keycloak-Login, Portal-Seiten | [services/frontend-web.md](services/frontend-web.md) |

Alle Service-Handbücher: [services/README.md](services/README.md).

## Ereignisgesteuerte Abläufe (Übersicht)

| Topic | Produzent (Beisp.) | Konsument (Beisp.) |
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
| `app.logs` | Alle Spring-Services (Log4j2) | log-consumer-service |

`finance-api` veröffentlicht kritische Domain-Ereignisse per **Transactional Outbox** nach Kafka (`OutboxPublisherScheduler`).

### Transactional-Outbox-Muster

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

### Kafka-Topic-Karte (visuell)

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

## Daten und Schema

- Eine PostgreSQL-Instanz (`finance`-Datenbank); separate `keycloak`-DB für Keycloak ([`Docker/postgres/init`](../../Docker/postgres/init)).
- Flyway-Tabellennamen sind servicespezifisch (z. B. `finance_flyway_schema_history`, `mds_flyway_schema_history`, `analytics_flyway_schema_history`).
- Lokales `market-data-service` **dev**-Profil kann standardmäßig In-Memory-H2 nutzen; Docker/Produktion verwendet PostgreSQL.

## Datenpersistenz

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

## Sicherheit

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

- **Keycloak**-Realm: `finance`, Public Client: `finance-gateway` (SPA), Confidential: `finance-portal` (Direct Grant / Backend).
- Gateway und Services als **OAuth2 Resource Server** (JWT).
- In der Entwicklung kann headerbasierte Benutzerauflösung mit `APP_SECURITY_ALLOW_HEADER_USER_FALLBACK` aktiviert werden; in Produktion deaktivieren.
- Portal-MFA (TOTP), vertrauenswürdiges Gerät-Cookie und Registrierungs-E-Mail-Verifizierung liegen in `finance-api`.

## Beobachtbarkeit

Metriken: Spring Actuator + Prometheus Scrape. Traces: OTLP → Jaeger. Logs: JSON → Kafka `app.logs` → OpenSearch. Details: [observability.md](observability.md).

## Deployment-Hinweis

Die Produktionstopologie ist in diesem Repository nicht definiert; Docker Compose dient der Entwicklungs-/Demo-Umgebung. Das Gateway benötigt ein `JWT_ISSUER_URI`, das mit dem vom Browser erreichbaren Issuer übereinstimmt (`localhost:8085`).
