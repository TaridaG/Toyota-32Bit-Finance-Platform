<p align="center">
  <img src="docs/assets/32bit.gif" alt="32bit" height="72" align="middle">
  &nbsp;&nbsp;&nbsp;
  <img src="docs/assets/author.gif" alt="Abdulkadir Kılıç" height="80" align="middle">
  &nbsp;&nbsp;&nbsp;
  <img src="docs/assets/brand.png" alt="32 Bit Finance" height="110" align="middle">
</p>

<h1 align="center">32 BİT FİNANCE PLATFORM</h1>

<p align="center"><sub>Dokumentation auf Deutsch — technische Leitfäden unter <a href="docs/deutsch/">docs/deutsch/</a></sub></p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Hibernate-59666C?logo=hibernate&logoColor=white" alt="Hibernate">
  <img src="https://img.shields.io/badge/JPA-6DB33F?logo=spring&logoColor=white" alt="JPA">
  <img src="https://img.shields.io/badge/React-61DAFB?logo=react&logoColor=black" alt="React">
  <img src="https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white" alt="TypeScript">
  <img src="https://img.shields.io/badge/Vite-646CFF?logo=vite&logoColor=white" alt="Vite">
  <img src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white" alt="Docker">
</p>
<p align="center">
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Kafka-231F20?logo=apachekafka&logoColor=white" alt="Kafka">
  <img src="https://img.shields.io/badge/Keycloak-4D4DFF?logo=keycloak&logoColor=white" alt="Keycloak">
  <img src="https://img.shields.io/badge/OpenTelemetry-000000?logo=opentelemetry&logoColor=white" alt="OpenTelemetry">
  <img src="https://img.shields.io/badge/Prometheus-E6522C?logo=prometheus&logoColor=white" alt="Prometheus">
  <img src="https://img.shields.io/badge/Grafana-F46800?logo=grafana&logoColor=white" alt="Grafana">
  <img src="https://img.shields.io/badge/OpenSearch-005EB8?logo=opensearch&logoColor=white" alt="OpenSearch">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="Lizenz: MIT">
</p>

<p align="center">
  <a href="README.tr.md">Türkçe</a> · <a href="README.md">English</a> · <strong>Deutsch</strong>
</p>

<p align="center">
  <strong>Auf die Türkei ausgerichtetes Finanzportal in drei Sprachen</strong> — Portfolio aufbauen, Märkte und Nachrichten auf einem Bildschirm verfolgen.
  Oberfläche und Inhalte unterstützen <strong>Türkisch</strong>, <strong>Englisch</strong> und <strong>Deutsch</strong>; beim Sprachwechsel passen sich Menüs, Karten und Meldungen an.
</p>

<p align="center">
  Live-Preise für Aktien, Fonds, Krypto, Devisen, Anleihen und Eurobonds; Transaktionshistorie, Ziele und externe Portfolioansicht.
  Wichtige Nachrichten <strong>markieren</strong>, Zeichnungen und Notizen in der technischen Analyse <strong>speichern</strong> und später wieder öffnen.
  Zins- und Festgeldprodukte, Bankkurse, RSI und weitere Indikatoren, Alarme, Watchlist und Finanzbildung — eine Plattform zur Verwaltung Ihrer Portfolios.
</p>

## Inhaltsverzeichnis

- [Bildschirme](#bildschirme)
- [Projektstruktur](#projektstruktur)
- [Plattform-Überblick](#plattform-überblick)
- [Schnellstart (Docker)](#schnellstart-docker--empfohlen)
- [Lokale Entwicklung (Kurz)](#lokale-entwicklung-kurz)
- [Dokumentation](#dokumentation)
- [Lizenz](#lizenz)

## Bildschirme

<table align="center" border="0" cellpadding="8" cellspacing="0" width="100%">
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Startseite</strong><br>
      <img src="docs/assets/screens/anasayfa.gif" alt="Startseite" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Märkte</strong><br>
      <img src="docs/assets/screens/piyasalar.gif" alt="Märkte" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Mein Portfolio</strong><br>
      <img src="docs/assets/screens/portfoyum.gif" alt="Mein Portfolio" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Zinsen / Festgeld</strong><br>
      <img src="docs/assets/screens/faizvadeli.gif" alt="Zinsen / Festgeld" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Analyse</strong><br>
      <img src="docs/assets/screens/analiz.gif" alt="Analyse" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Nachrichten</strong><br>
      <img src="docs/assets/screens/haberler.gif" alt="Nachrichten" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Bankkurse</strong><br>
      <img src="docs/assets/screens/bankakurlari.gif" alt="Bankkurse" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Finanzbildung</strong><br>
      <img src="docs/assets/screens/finansalokuryazarlik.gif" alt="Finanzbildung" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Infokarten</strong><br>
      <img src="docs/assets/screens/bilgikartlari.gif" alt="Infokarten" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Profil</strong><br>
      <img src="docs/assets/screens/profil.gif" alt="Profil" width="100%">
    </td>
  </tr>
</table>

## Projektstruktur

Die **32 Bit Finance Platform** ist ein Finanzportal mit Microservice-Architektur. Der Nutzer sendet Anfragen aus der **React**-Oberfläche an eine einzige Adresse (**api-gateway**); das Gateway authentifiziert über **Keycloak** und leitet an den jeweiligen Service weiter. Portal-Geschäftsregeln wie Portfolio, Alarme, Registrierung und MFA sind in **finance-api** gebündelt; Marktpreise, Nachrichten, technische Analyse und Benachrichtigungen sind in eigenen Services spezialisiert. Services rufen sich bei Bedarf per **HTTP** auf; Preisaktualisierungen, Alarme und Logs laufen asynchron über **Kafka**. Persistente Daten liegen in **PostgreSQL**; jedes Service-Schema wird separat mit **Flyway** verwaltet.

<h3 align="center">
  <a href="docs/deutsch/architecture.md">PROJEKTARCHITEKTUR</a>
</h3>

<p align="center">
  <a href="docs/deutsch/architecture.md">
    <img src="docs/diagrams/genelmimari.png" alt="Architekturdiagramm" width="900">
  </a>
</p>

<table align="center" border="0" cellpadding="20" cellspacing="0">
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/api-gateway.md">api-gateway</a></h4>
      <p>Die <strong>einzige Eingangstür</strong> für Browser- und Mobile-Clients zum Backend. Validiert bei jeder <code>/api/v1/...</code>-Anfrage das JWT über Keycloak und übergibt die Nutzeridentität per sicheren Headern an Backend-Services. Leitet je nach Pfad an <code>finance-api</code>, <code>market-data-service</code>, <code>news-service</code> oder <code>analytics-service</code> weiter; wendet Rate-Limiting mit Redis und Circuit Breaker mit Resilience4j an. Bündelt die OpenAPI-Dokumentation aller Services in einer Swagger-Oberfläche. Detaillierte Routenliste: <a href="docs/deutsch/api.md">docs/deutsch/api.md</a>.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/finance-api.md">finance-api</a></h4>
      <p>Das <strong>Herz</strong> des Portals und die Backend-for-Frontend-(BFF)-Schicht. Nutzerspezifische Regeln für Portfolio, Transaktionshistorie, Ziele, Preisalarme, Watchlist, gespeicherte Chart-Zeichnungen und Profil leben hier. Registrierung, E-Mail-Verifizierung, TOTP-basierte MFA und vertrauenswürdige Geräte sowie Admin-KPIs, Infokarten und Nachrichten-Favoriten sind in diesem Service gebündelt. Holt Marktdaten per HTTP von <code>market-data-service</code>; konsumiert Live-Preise und FX-Snapshots aus Kafka. Schreibt kritische Domain-Ereignisse per transactional outbox nach Kafka; Daten in PostgreSQL mit Flyway.</p>
    </td>
  </tr>
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/market-data-service.md">market-data-service</a></h4>
      <p>Die <strong>Marktdaten-Engine</strong> der Plattform. Katalogisiert BIST-, Nasdaq-, Krypto-, Fonds-, FX-, Anleihen- und Eurobond-Instrumente; aktualisiert Live- und Historienpreise per Scheduler. Bezieht Daten aus Quellen wie TCMB EVDS, Finnhub, Yahoo, CoinGecko und Stooq; liefert Leitzins, Bankkurse und Basisinstrumentdaten. Kann beim Erststart historische Preise backfillen; nutzt hybriden JSON-Cache für häufig abgerufene Endpunkte. Veröffentlicht Preisänderungen auf Kafka-Topics wie <code>market.price.updated</code> und speist Analytik sowie Portal.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/analytics-service.md">analytics-service</a></h4>
      <p>Der Spezialdienst, der Live-Preisströme in <strong>technische Analyse</strong> umwandelt. Konsumiert Preisereignisse von <code>market-data-service</code> über Kafka; berechnet RSI und ähnliche Indikatoren, erzeugt Insights und Policy-Metriken. Speichert Ergebnisse im eigenen PostgreSQL-Schema und stellt sie per HTTP-API über das Gateway bereit. Schwere Indikatorberechnungen bleiben vom Portal-BFF getrennt; Analyse-Seite und zugehörige Karten beziehen aktuelle Metriken von hier.</p>
    </td>
  </tr>
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/news-service.md">news-service</a></h4>
      <p>Der Service, der Finanznachrichten <strong>sammelt und bereitstellt</strong>. Durchsucht definierte RSS-Quellen per geplanten Jobs, schreibt Artikel in die Datenbank und speist die Listen-/Filter-API des Portals. Bietet bei Bedarf Übersetzung über MyMemory; erreichbar über <code>/api/v1/news/**</code> am Gateway. Der Nachrichtenstrom skaliert unabhängig vom Portal; in Docker mit restriktivem Dateisystem und eigener DB-Passwort-Konfiguration.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/notification-service.md">notification-service</a></h4>
      <p>Das Zentrum für <strong>E-Mail-Benachrichtigungen</strong> an Nutzer. Hört auf Kafka-Ereignisse wie ausgelöste Alarme, verdächtige Anmeldungen, Watchlist-Änderungen und Analyse-Insights. Erzeugt je nach Typ templated E-Mails per SMTP und verlagert die Last aus der Live-UI. <code>finance-api</code> erzeugt Geschäftsregeln, dieser Service liefert — der Benachrichtigungskanal kann wechseln, ohne Domain-Code zu verkomplizieren.</p>
    </td>
  </tr>
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/log-consumer-service.md">log-consumer-service</a></h4>
      <p>Der <strong>zentrale Log-Archivar</strong> aller Backend-Services. Anwendungen schreiben per Log4j2 im JSON-Format auf das Kafka-Topic <code>app.logs</code>; dieser Service konsumiert und indexiert in OpenSearch. Betrieb kann über Grafana, OpenSearch Dashboards oder Suche Fehler und Traces filtern. App-Pods müssen Logs nicht selbst speichern; die Pipeline wird plattformweit an einer Stelle verwaltet.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/frontend-web.md">frontend-web</a></h4>
      <p>Die <strong>React-19</strong>-Single-Page-App (Vite + TypeScript), die der Nutzer sieht. Landing, Märkte, Analyse, Nachrichten, Bankkurse und Finanzbildung sind öffentlich; Portfolio, Zinsen/Festgeld, Dashboard und externe Portfolioansicht erfordern Anmeldung. Keycloak für Login und Registrierung; UI-Sprachen TR, EN und DE. Alle API-Aufrufe über <code>api-gateway</code>; separater <code>/admin</code>-Bereich für Admin und Infokarten.</p>
    </td>
  </tr>
</table>

> **Hinweis — Marktdaten und Scheduler**  
> Nach dem Start des Stacks kombiniert `market-data-service` historischen Preis-Backfill mit Live-Updates im Hintergrund. Das Vervollständigen von Katalog und Historien — abhängig von Instrumentenzahl und Provider-Latenz — kann **etwa 30 Minuten** dauern; das Portal füllt sich schrittweise, fehlende Charts oder leere Listen in den ersten Minuten sind normal.  
> Demo-Scheduler-Intervalle (z. B. Aktien ~1 Min., FX/Anleihen ~5 Min.) und Backfill-Schritte sind auf **kostenlose Kontingente** externer Quellen (TCMB EVDS, Yahoo, CoinGecko, Finnhub usw.) abgestimmt; API-Schlüssel stehen in `Docker/.env` (nicht im Repo). Pausen zwischen Anfragen vermeiden Rate-Limit-Verstöße. Bei kostenpflichtigen Plänen oder höherem Kontingent können `scheduler.*.delay-ms`, Backfill-`sleep-ms` und Cron-Ausdrücke in [docs/deutsch/configuration.md](docs/deutsch/configuration.md) verkürzt werden.  
> Fortschritt: `docker compose logs -f market-data-service`

## Plattform-Überblick

### Kafka-Ereignisse

| Topic | Produzent (Beisp.) | Konsument (Beisp.) |
|-------|---------------------|---------------------|
| `market.price.updated` | market-data-service | analytics-service, finance-api |
| `market.fx.snapshot.updated` | market-data-service | finance-api |
| `alarm-triggered` | finance-api (outbox) | notification-service |
| `watchlist.item.added` / `removed` | finance-api | notification-service |
| `login-security.alert` | finance-api | notification-service |
| `app.logs` | Alle Services | log-consumer-service |

### Infrastrukturkomponenten

Gemeinsame Komponenten via Docker Compose (`Docker/`):

| Komponente | Host-Port | Rolle |
|------------|-----------|-------|
| PostgreSQL | 5432 | Datenbanken `finance` + `keycloak` |
| Redis | 6379 | Gateway Rate Limit, finance-api Cache |
| Kafka | 9092 | Event Bus |
| Keycloak | 8085 | OIDC / Realm-Import |
| OpenSearch | 9200 | Log-Suche |
| OpenSearch Dashboards | 5601 | Log-UI |
| Jaeger | 16686 | Distributed Tracing |
| Prometheus | 9090 | Metrik-Scrape |
| Grafana | 3000 | Dashboards |

### Unterstützende Verzeichnisse

| Verzeichnis | Inhalt |
|-------------|--------|
| `Docker/` | `docker-compose`, Keycloak Realm, Observability Stack |
| `docs/` | Architektur, Services, API, Setup, Observability (`turkce/`, `english/`, `deutsch/`) |
| `photos/` | Profil-Avatare (lokal / Volume) |

## Schnellstart (Docker — empfohlen)

Im Repository **keine API-Schlüssel**. `cp .env.example .env` legt nur die Datei an; **Schlüssel und optional SMTP müssen Sie eintragen**. Schritt-für-Schritt: [docs/deutsch/getting-started.md](docs/deutsch/getting-started.md).

**Voraussetzungen:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Compose v2), ca. **8 GB RAM**.

| Schritt | Aktion |
|---------|--------|
| **1** | Repository klonen |
| **2** | `Docker/.env` anlegen (`cp .env.example .env`) |
| **3** | `Docker/.env` **ausfüllen:** Pflicht `TCMB_API_KEY`, `FINNHUB_API_KEY` |
| **4** | Optional: SMTP (Registrierung / Alarm-Mail), OpenAI (Admin-AI) |
| **5** | Aus `Docker`: `docker compose up -d --build` |
| **6** | Portal: http://localhost:5173 — Demo `admin1` / `123456` (Marktdaten 10–30 Min.) |

**1 · Klon**

```bash
git clone https://github.com/TaridaG/Toyota-32Bit-Finance-Platform.git
cd Toyota-32Bit-Finance-Platform/Docker
```

**2 · Umgebungsdatei**

```bash
cp .env.example .env
```

Windows (PowerShell): `Copy-Item .env.example .env`

> Die Kopie allein reicht nicht — in Schritt 3 die Datei **bearbeiten**.

**3 · Ausfüllen (Pflicht-API-Schlüssel)**

`Docker/.env` in einem Editor öffnen:

```env
TCMB_API_KEY=ihr-evds-schluessel
FINNHUB_API_KEY=ihr-finnhub-schluessel
```

- Schlüssel nur in `Docker/.env`; **diese Datei nicht committen**.
- `POSTGRES_PASSWORD` und `NEWS_DB_PASSWORD` in der Vorlage `123456` — für lokale Demo beibehalten.

**4 · Optional (SMTP / KI)**

| Variable | Zweck |
|----------|-------|
| `OPENAI_API_KEY` + `AI_ENABLED=true` | Admin-Infokarten-KI |
| `SPRING_MAIL_*` / `SMTP_*` | Registrierungs- und Alarm-E-Mail |
| `MARKET_EVDS_API_KEY` | Separater EVDS-Schlüssel (sonst `TCMB_API_KEY`; **keine leere `KEY=` Zeile**) |

E-Mail-Beispiel (Gmail **App-Passwort**, nicht das normale Login-Passwort):

```env
SMTP_USERNAME=you@gmail.com
SMTP_PASSWORD=16-stelliges-app-passwort
SPRING_MAIL_USERNAME=you@gmail.com
SPRING_MAIL_PASSWORD=16-stelliges-app-passwort
APP_REGISTRATION_VERIFICATION_FROM=you@gmail.com
NOTIFICATION_MAIL_FROM=you@gmail.com
```

Alle Variablen: [docs/deutsch/configuration.md](docs/deutsch/configuration.md).

> **Erstinstallation:** Nach Schritt 3 reicht in Schritt 5 `docker compose up`; `--force-recreate` ist nicht nötig.  
> **`.env` ändert sich bei laufendem Stack:** `docker compose up -d --force-recreate market-data-service` (TCMB/Finnhub), `finance-api` (Mail/KI), `notification-service` (SMTP).

**5 · Stack starten**

```bash
docker compose up -d --build
```

### Schnellzugriff

<p align="center"><sub>Bei laufendem Stack auf Logos klicken — Portal, API, Identität, Observability und Infrastruktur (TCP-Ports per Hover)</sub></p>

<p align="center">
  <a href="http://localhost:5173" title="Web-Oberfläche — http://localhost:5173"><img src="https://cdn.simpleicons.org/react/61DAFB" height="32" alt="Web-Oberfläche"></a>&nbsp;
  <a href="http://localhost:8080" title="API Gateway — http://localhost:8080"><img src="https://cdn.simpleicons.org/springboot/6DB33F" height="32" alt="API Gateway"></a>&nbsp;
  <a href="http://localhost:8080/swagger-ui.html" title="Swagger UI — http://localhost:8080/swagger-ui.html"><img src="https://cdn.simpleicons.org/swagger/85EA2D" height="32" alt="Swagger UI"></a>&nbsp;
  <a href="http://localhost:8085" title="Keycloak OIDC (Realm: finance) — http://localhost:8085"><img src="https://cdn.simpleicons.org/keycloak/4D4DFF" height="32" alt="Keycloak"></a>&nbsp;
  <a href="http://localhost:8085/admin" title="Keycloak Admin (admin / admin) — http://localhost:8085/admin"><img src="https://cdn.simpleicons.org/keycloak/FFFFFF" height="32" alt="Keycloak Admin"></a>
</p>
<p align="center">
  <a href="http://localhost:3000" title="Grafana (admin / admin) — http://localhost:3000"><img src="https://cdn.simpleicons.org/grafana/F46800" height="32" alt="Grafana"></a>&nbsp;
  <a href="http://localhost:9090" title="Prometheus — http://localhost:9090"><img src="https://cdn.simpleicons.org/prometheus/E6522C" height="32" alt="Prometheus"></a>&nbsp;
  <a href="http://localhost:16686" title="Jaeger — http://localhost:16686"><img src="https://cdn.simpleicons.org/opentelemetry/FFFFFF" height="32" alt="Jaeger"></a>&nbsp;
  <a href="http://localhost:5601" title="OpenSearch Dashboards — http://localhost:5601"><img src="https://cdn.simpleicons.org/opensearch/005EB8" height="32" alt="OpenSearch Dashboards"></a>&nbsp;
  <a href="https://localhost:9200" title="OpenSearch REST API (admin / 123456789) — https://localhost:9200"><img src="https://cdn.simpleicons.org/opensearch/FFFFFF" height="32" alt="OpenSearch API"></a>&nbsp;
  <span title="PostgreSQL — localhost:5432"><img src="https://cdn.simpleicons.org/postgresql/4169E1" height="32" alt="PostgreSQL"></span>&nbsp;
  <span title="Redis — localhost:6379"><img src="https://cdn.simpleicons.org/redis/DC382D" height="32" alt="Redis"></span>&nbsp;
  <span title="Kafka — localhost:9092"><img src="https://cdn.simpleicons.org/apachekafka/FFFFFF" height="32" alt="Kafka"></span>
</p>

### Demo-Benutzer

| Benutzer | Passwort | Rolle |
|----------|----------|-------|
| `user1` | `123456` | USER |
| `admin1` | `123456` | ADMIN |

Keycloak-Admin: http://localhost:8085/admin — `admin` / `admin`

Marktdaten-Verzögerung: Hinweis unter [Projektstruktur](#projektstruktur). Logs: `docker compose logs -f market-data-service`

## Lokale Entwicklung (Kurz)

1. Infrastruktur: `cd Docker && docker compose up -d postgres redis kafka keycloak`
2. Backend: `mvn -pl finance-api,market-data-service -am spring-boot:run` (Ports: [docs/deutsch/services.md](docs/deutsch/services.md))
3. Frontend: `cd frontend-web && npm install && npm run dev` (bei vollem Docker-Stack kein `.env.development` nötig)

Details: [docs/deutsch/getting-started.md](docs/deutsch/getting-started.md) (Pfad B / C).

## Dokumentation

Technische Leitfäden unter `docs/deutsch/`. Weitere Sprachen: [docs/README.md](docs/README.md) · [README.tr.md](README.tr.md) · [README.md](README.md)

| Dokument | Inhalt |
|----------|--------|
| [docs/deutsch/README.md](docs/deutsch/README.md) | Deutsch-Dokumentationsindex |
| [docs/deutsch/getting-started.md](docs/deutsch/getting-started.md) | Einrichtung (Docker, hybrid, Frontend) |
| [docs/deutsch/architecture.md](docs/deutsch/architecture.md) | Architektur und Datenfluss |
| [docs/deutsch/services.md](docs/deutsch/services.md) | Services, Ports |
| [docs/deutsch/api.md](docs/deutsch/api.md) | Gateway-Routen und OpenAPI |
| [docs/deutsch/configuration.md](docs/deutsch/configuration.md) | `Docker/.env` und Umgebungsvariablen |
| [docs/deutsch/development.md](docs/deutsch/development.md) | Entwicklung und Tests |
| [docs/deutsch/observability.md](docs/deutsch/observability.md) | Metriken, Traces, Logs |

## Haftungsausschluss

Dieses Repository ist ein **Demo- und Lernprojekt**. Es stellt keine Anlage-, Steuer- oder Rechtsberatung dar. Live-Marktdaten hängen von Drittanbieter-APIs und deren Nutzungsbedingungen ab. Der Einsatz in Produktion oder mit echtem Geld erfolgt auf eigenes Risiko.

## Lizenz

Dieses Projekt steht unter der [MIT-Lizenz](LICENSE).
