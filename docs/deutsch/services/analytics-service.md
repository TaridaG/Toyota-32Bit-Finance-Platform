# analytics-service

## Zusammenfassung

**analytics-service** konsumiert von `market-data-service` veröffentlichte Preis-Events und berechnet **technische Analysemetriken** (RSI, VWAP usw.), speichert Ergebnisse in PostgreSQL. Einfache Insight-Events werden auf `analytics.insight.simple` geschrieben; Portal-Endpunkte `/api/v1/analytics/**` laufen über das Gateway.

## Verantwortlichkeiten

- `market.price.updated` und FX-Snapshot-Events aus Kafka konsumieren
- Indikatoren berechnen und persistent speichern (Analytics-Flyway-Schema)
- REST-API: Analyseabfragen (`AnalyticsController`)
- Produktion von `analytics.insight.simple` (Preisbewegung / Insight-Benachrichtigungen)
- HTTP-Integration mit `finance-api` (bei Bedarf)

## Außerhalb des Aufgabenbereichs

- Rohpreisabruf und EVDS — `market-data-service`
- Portal-Portfolio / Alarm-Geschäftslogik — `finance-api`
- E-Mail-Versand — `notification-service` (konsumiert Insight-Topic)
- News-Zuordnung — `news-service`

## Möglichkeiten

| Perspektive | Fähigkeit |
|-------------|-----------|
| Portal | Analyse-Seite Indikatoren, Insight-Karten (über Gateway) |
| Betrieb | Health, Prometheus-Metriken |
| Plattform | Aus Preisstrom abgeleitete Metriken |

## Laufzeit

| Eigenschaft | Wert |
|-------------|------|
| Maven-Modul | `analytics-service` |
| Container-Name | `analytics-service` |
| HTTP-Port | **8080** (Spring Boot Standard) |
| Spring-Profile | `docker` (Compose) |
| Healthcheck | `/actuator/health` |

## Abhängigkeiten

| Komponente | Verwendung |
|------------|------------|
| PostgreSQL | DB `finance`, `analytics_flyway_schema_history` |
| Kafka | Preis konsumieren; Insight produzieren |
| HTTP | `FINANCE_BASE_URL` → finance-api (intern) |
| Keycloak | JWT (Resource Server) |

## Kontextdiagramm

```mermaid
flowchart LR
  MDS[market-data-service]
  KF[Kafka]
  AS[analytics-service]
  PG[(PostgreSQL)]
  GW[api-gateway]
  UI[frontend-web]
  NS[notification-service]

  MDS -->|market.price.updated| KF
  KF --> AS
  AS --> PG
  AS -->|analytics.insight.simple| KF
  KF --> NS
  UI --> GW
  GW --> AS
```

## HTTP-Datenflüsse

### Wichtige Endpunkte

| Pfad (Gateway) | Controller | Beschreibung |
|----------------|------------|--------------|
| `/api/v1/analytics/**` | `AnalyticsController` | Indikator- / Insight-Abfragen |

```mermaid
sequenceDiagram
  participant UI as frontend-web
  participant GW as api-gateway
  participant AS as analytics-service
  participant PG as PostgreSQL

  UI->>GW: GET /api/v1/analytics/...
  GW->>AS: JWT + proxy
  AS->>PG: hesaplanmış metrikler
  AS-->>GW: JSON
  GW-->>UI: 200 OK
```

## Verarbeitungs-Pipeline

```mermaid
flowchart LR
  KIN[market.price.updated]
  CON[MarketPriceUpdatedConsumer]
  FEAT[FeatureExtractionService]
  PG[(analytics tables)]
  OUT[analytics.insight.simple]

  KIN --> CON --> FEAT
  FEAT --> PG
  FEAT --> OUT
```

## Kafka / Ereignisflüsse

| Topic | Rolle | Beschreibung |
|-------|-------|--------------|
| `market.price.updated` | Konsumiert | `MarketPriceUpdatedConsumer` |
| FX snapshot topic | Konsumiert | `FxSnapshotUpdatedConsumer` |
| `analytics.insight.simple` | Produziert | `FeatureExtractionService` — Benachrichtigungs-Pipeline |

```mermaid
sequenceDiagram
  participant MDS as market-data-service
  participant KF as Kafka
  participant AS as analytics-service
  participant PG as PostgreSQL
  participant NS as notification-service

  MDS->>KF: market.price.updated
  KF->>AS: consume
  AS->>PG: RSI / VWAP persist
  AS->>KF: analytics.insight.simple
  KF->>NS: insight e-posta kuyruğu
```

Consumer mit Retry + DLQ (`topic.dlq`) konfiguriert.

## Scheduler / Hintergrundaufgaben

Überwiegend **Kafka-gesteuert**; zusätzliche Batch-Jobs begrenzt auf Migration/Startup. Periodische Berechnung wird durch Preis-Events ausgelöst.

## Datenmodell

| Element | Wert |
|---------|------|
| Flyway | `analytics-service/src/main/resources/db/migration/` |
| History-Tabelle | `analytics_flyway_schema_history` |
| Hauptkonzepte | RSI täglich, VWAP, Insight- / Policy-Metrik-Tabellen |

## Paket- / Codestruktur

```
analytics-service/src/main/java/com/company/analytics/
├── processing/       # Kafka consumer'lar, FeatureExtractionService
├── query/            # AnalyticsController
└── bootstrap/        # config, health
```

## Konfiguration

| Variable | Beschreibung |
|----------|--------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka |
| `FINANCE_BASE_URL` | finance-api HTTP |
| `TRACING_SAMPLE_PROBABILITY` | Trace-Sampling |

Vollständige Liste: [configuration.md](../configuration.md).

## Beobachtbarkeit

| Kanal | Detail |
|-------|--------|
| Logs | `app.logs` |
| Metriken | Prometheus-Job `analytics-service` |
| Trace | OTLP → Jaeger |

Details: [observability.md](../observability.md).

## Lokale Ausführung

```bash
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finance

mvn -pl analytics-service -am spring-boot:run
```

Einrichtung: [getting-started.md](../getting-started.md).

## Verwandte Dokumente

| Dokument | Inhalt |
|----------|--------|
| [market-data-service.md](market-data-service.md) | Preis-Event-Produzent |
| [notification-service.md](notification-service.md) | Insight-Consumer |
| [api-gateway.md](api-gateway.md) | Analytics-Route |
| [architecture.md](../architecture.md) | Kafka-Übersicht |
