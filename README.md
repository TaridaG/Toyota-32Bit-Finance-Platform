<p align="center">
  <img src="docs/assets/32bit.gif" alt="32bit" height="72" align="middle">
  &nbsp;&nbsp;&nbsp;
  <img src="docs/assets/author.gif" alt="Abdulkadir Kılıç" height="80" align="middle">
  &nbsp;&nbsp;&nbsp;
  <img src="docs/assets/brand.png" alt="32 Bit Finance" height="110" align="middle">
</p>

<h1 align="center">32 BIT FINANCE PLATFORM</h1>

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
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT">
</p>

<p align="center">
  <a href="README.tr.md">Türkçe</a> · <strong>English</strong> · <a href="README.de.md">Deutsch</a>
</p>

<p align="center">
  <strong>Turkey-focused finance portal in three languages</strong> — build your portfolio and follow markets and news on one screen.
  The UI and content support <strong>Turkish</strong>, <strong>English</strong>, and <strong>German</strong>; when you switch language, menus, cards, and messages adapt to your choice.
</p>

<p align="center">
  Track equities, funds, crypto, FX, bonds, and eurobonds with live prices; transaction history, goals, and external portfolio views.
  <strong>Star</strong> important news items, <strong>save</strong> chart drawings and notes in technical analysis and reopen them later.
  Interest and term products, bank rates, RSI and other indicators, alarms, watchlists, and financial literacy — one platform to manage your portfolios.
</p>

## Table of contents

- [Screens](#screens)
- [Project structure](#project-structure)
- [Platform overview](#platform-overview)
- [Quick start (Docker)](#quick-start-docker--recommended)
- [Local development (summary)](#local-development-summary)
- [Documentation](#documentation)
- [License](#license)

## Screens

<table align="center" border="0" cellpadding="8" cellspacing="0" width="100%">
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Home</strong><br>
      <img src="docs/assets/screens/anasayfa.gif" alt="Home" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Markets</strong><br>
      <img src="docs/assets/screens/piyasalar.gif" alt="Markets" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>My Portfolio</strong><br>
      <img src="docs/assets/screens/portfoyum.gif" alt="My Portfolio" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Interest / Deposits</strong><br>
      <img src="docs/assets/screens/faizvadeli.gif" alt="Interest / Deposits" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Analysis</strong><br>
      <img src="docs/assets/screens/analiz.gif" alt="Analysis" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>News</strong><br>
      <img src="docs/assets/screens/haberler.gif" alt="News" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Bank rates</strong><br>
      <img src="docs/assets/screens/bankakurlari.gif" alt="Bank rates" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Financial literacy</strong><br>
      <img src="docs/assets/screens/finansalokuryazarlik.gif" alt="Financial literacy" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Info cards</strong><br>
      <img src="docs/assets/screens/bilgikartlari.gif" alt="Info cards" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Profile</strong><br>
      <img src="docs/assets/screens/profil.gif" alt="Profile" width="100%">
    </td>
  </tr>
</table>

## Project structure

**32 Bit Finance Platform** is a finance portal built on a microservice architecture. Users call a single address (**api-gateway**) from the **React** UI; the gateway authenticates identity with **Keycloak** and routes each request to the right service. Portal business rules such as portfolio, alarms, registration, and MFA live in **finance-api**; market prices, news, technical analysis, and notifications are handled by dedicated services. Services call each other over **HTTP** when needed; flows such as price updates, alarms, and logging proceed asynchronously over **Kafka**. Persistent data is stored in **PostgreSQL**; each service schema is managed separately with **Flyway**.

<h3 align="center">
  <a href="docs/english/architecture.md">PROJECT ARCHITECTURE</a>
</h3>

<p align="center">
  <a href="docs/english/architecture.md">
    <img src="docs/diagrams/genelmimari.png" alt="Project architecture diagram" width="900">
  </a>
</p>

<table align="center" border="0" cellpadding="20" cellspacing="0">
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/english/services/api-gateway.md">api-gateway</a></h4>
      <p>The <strong>single entry point</strong> for browser and mobile clients to reach the backend. For every incoming <code>/api/v1/...</code> request it validates the JWT via Keycloak and forwards user identity to downstream services through secure headers. It routes by path to <code>finance-api</code>, <code>market-data-service</code>, <code>news-service</code>, or <code>analytics-service</code>; applies Redis rate limiting and Resilience4j circuit breakers. It aggregates OpenAPI documentation for all services in one Swagger UI for developers and integrators. Full route list: <a href="docs/english/api.md">docs/english/api.md</a>.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/english/services/finance-api.md">finance-api</a></h4>
      <p>The portal <strong>core</strong> and Backend-for-Frontend (BFF) layer. User-specific rules such as portfolio, transaction history, goals, price alarms, watchlist, chart drawing saves, and profile live here. Registration, email verification, TOTP-based MFA, trusted devices, admin KPIs, info cards, and news favorites are also centralized in this service. It calls <code>market-data-service</code> over HTTP for market data and consumes live price and FX snapshots from Kafka. Critical domain events are written to Kafka via a transactional outbox; data is stored in PostgreSQL with Flyway migrations.</p>
    </td>
  </tr>
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/english/services/market-data-service.md">market-data-service</a></h4>
      <p>The platform <strong>market data engine</strong>. It catalogs BIST, Nasdaq, crypto, funds, FX, bonds, and eurobond instruments; updates live and historical prices via schedulers. It pulls data from sources such as TCMB EVDS, Finnhub, Yahoo, CoinGecko, and Stooq; provides policy rates, bank rates, and basic instrument metadata. On first setup it can run historical price backfill and uses a hybrid JSON cache for frequently read endpoints. It publishes price changes to Kafka topics such as <code>market.price.updated</code> to feed analytics and the portal layer.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/english/services/analytics-service.md">analytics-service</a></h4>
      <p>A specialist service that turns the live price stream into <strong>technical analysis</strong>. It consumes price events published by <code>market-data-service</code> over Kafka; computes RSI and similar indicators and produces insight and policy metrics. Results are stored in its own PostgreSQL schema and exposed via HTTP API through the gateway. Heavy indicator work stays out of the portal BFF; the analysis page and related cards read current metrics from here.</p>
    </td>
  </tr>
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/english/services/news-service.md">news-service</a></h4>
      <p>The service where financial news is <strong>aggregated and served</strong>. It scans configured RSS sources on a schedule, writes articles to the database, and feeds the portal listing and filtering API. It offers MyMemory translation where needed; accessed via gateway at <code>/api/v1/news/**</code>. The news pipeline scales independently of the portal experience; in Docker it is configured with a strict filesystem layout and a separate database password.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/english/services/notification-service.md">notification-service</a></h4>
      <p>The hub for <strong>email notifications</strong> to users. It listens on Kafka for events such as alarm triggers, suspicious login, watchlist changes, and analytics insights. It sends templated email over SMTP by event type, moving instant UI load off the portal. <code>finance-api</code> produces business events; this service delivers them so domain code stays simple even if the notification channel changes.</p>
    </td>
  </tr>
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/english/services/log-consumer-service.md">log-consumer-service</a></h4>
      <p>The <strong>central log archive</strong> for all backend services. Applications write JSON logs to the <code>app.logs</code> Kafka topic via Log4j2; this service consumes messages and indexes them in OpenSearch. Operations can filter errors and traces via Grafana, OpenSearch Dashboards, or direct search. Application pods do not need to store logs; the log pipeline is managed platform-wide from one place.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/english/services/frontend-web.md">frontend-web</a></h4>
      <p>The <strong>React 19</strong> single-page application users see (Vite + TypeScript). Landing, markets, analysis, news, bank rates, and financial literacy are public; portfolio, interest/deposits, dashboard, and external portfolio views require a session. It handles login and registration with Keycloak and supports TR, EN, and DE. All API calls go through <code>api-gateway</code>; a separate <code>/admin</code> area exists for admin and info card management.</p>
    </td>
  </tr>
</table>

> **Note — market data and schedulers**  
> After the stack is up, `market-data-service` merges historical price backfill with live updates in the background. Completing the catalog and historical series — depending on instrument count and provider response times — can take **about 30 minutes**; the portal fills in gradually, and missing charts or empty lists in the first minutes are normal.  
> Scheduler intervals in the demo configuration (e.g. equities ~1 min, FX/bonds ~5 min) and backfill steps are tuned for external sources with **free-tier quotas** (TCMB EVDS, Yahoo, CoinGecko, Finnhub, etc.); API keys are set in `Docker/.env` (not in the repo). Delays between requests help avoid rate-limit violations. When moving to a paid API plan or higher quota, tighten `scheduler.*.delay-ms`, backfill `sleep-ms`, and related cron expressions via [docs/english/configuration.md](docs/english/configuration.md).  
> To watch progress: `docker compose logs -f market-data-service`

## Platform overview

### Kafka events

| Topic | Producer (e.g.) | Consumer (e.g.) |
|-------|-----------------|-----------------|
| `market.price.updated` | market-data-service | analytics-service, finance-api |
| `market.fx.snapshot.updated` | market-data-service | finance-api |
| `alarm-triggered` | finance-api (outbox) | notification-service |
| `watchlist.item.added` / `removed` | finance-api | notification-service |
| `login-security.alert` | finance-api | notification-service |
| `app.logs` | All services | log-consumer-service |

### Infrastructure components

Shared components brought up with Docker Compose (`Docker/`):

| Component | Host port | Role |
|-----------|-----------|------|
| PostgreSQL | 5432 | `finance` + `keycloak` databases |
| Redis | 6379 | Gateway rate limit, finance-api cache |
| Kafka | 9092 | Event bus |
| Keycloak | 8085 | OIDC / realm import |
| OpenSearch | 9200 | Log search |
| OpenSearch Dashboards | 5601 | Log UI |
| Jaeger | 16686 | Distributed tracing |
| Prometheus | 9090 | Metric scrape |
| Grafana | 3000 | Dashboards |

### Supporting directories

| Directory | Contents |
|-----------|----------|
| `Docker/` | `docker-compose`, Keycloak realm, observability stack |
| `docs/` | Architecture, services, API, setup, observability (`turkce/`, `english/`, `deutsch/`) |
| `photos/` | Profile avatar files (local / volume) |

## Quick start (Docker — recommended)

The repository contains **no API keys**. `cp .env.example .env` only creates the file; **you must add keys and (optionally) SMTP inside**. Step-by-step guide: [docs/english/getting-started.md](docs/english/getting-started.md).

**Requirements:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Compose v2), roughly **8 GB RAM**.

| Step | What to do |
|------|------------|
| **1** | Clone the repository |
| **2** | Create `Docker/.env` (`cp .env.example .env`) |
| **3** | **Fill in** `Docker/.env`: required `TCMB_API_KEY`, `FINNHUB_API_KEY` |
| **4** | Optional: SMTP (registration / alarm email), OpenAI (admin AI) |
| **5** | From the `Docker` folder: `docker compose up -d --build` |
| **6** | Portal: http://localhost:5173 — demo `admin1` / `123456` (market data may take 10–30 min to populate) |

**1 · Clone**

```bash
git clone https://github.com/TaridaG/Toyota-32Bit-Finance-Platform.git
cd Toyota-32Bit-Finance-Platform/Docker
```

**2 · Create environment file**

```bash
cp .env.example .env
```

Windows (PowerShell): `Copy-Item .env.example .env`

> Copying alone is not enough — edit the file **contents** in Step 3.

**3 · Fill in (required API keys)**

Open `Docker/.env` in a text editor:

```env
TCMB_API_KEY=your-evds-key
FINNHUB_API_KEY=your-finnhub-key
```

- Keys live only in `Docker/.env`; **do not commit this file**.
- `POSTGRES_PASSWORD` and `NEWS_DB_PASSWORD` default to `123456` in the template — fine to leave for local demo.

**4 · Optional (SMTP / AI)**

| Variable | Purpose |
|----------|---------|
| `OPENAI_API_KEY` + `AI_ENABLED=true` | Admin info card AI |
| `SPRING_MAIL_*` / `SMTP_*` | Registration verification and alarm email |
| `MARKET_EVDS_API_KEY` | Separate EVDS key (falls back to `TCMB_API_KEY`; **do not write empty `KEY=` lines**) |

Email example (Gmail **app password**, not your normal login password):

```env
SMTP_USERNAME=you@gmail.com
SMTP_PASSWORD=16-digit-app-password
SPRING_MAIL_USERNAME=you@gmail.com
SPRING_MAIL_PASSWORD=16-digit-app-password
APP_REGISTRATION_VERIFICATION_FROM=you@gmail.com
NOTIFICATION_MAIL_FROM=you@gmail.com
```

All variables: [docs/english/configuration.md](docs/english/configuration.md).

> **First install:** After Step 3, `docker compose up` in Step 5 is enough; `--force-recreate` is not required.  
> **While the stack is running, if `.env` changes:** `docker compose up -d --force-recreate market-data-service` (TCMB/Finnhub), `finance-api` (mail/AI), `notification-service` (SMTP).

**5 · Start the stack**

```bash
docker compose up -d --build
```

### Quick access

<p align="center"><sub>When the stack is running, click the logos — portal, API, identity, observability, and infrastructure (hover for TCP ports)</sub></p>

<p align="center">
  <a href="http://localhost:5173" title="Web UI — http://localhost:5173"><img src="https://cdn.simpleicons.org/react/61DAFB" height="32" alt="Web UI"></a>&nbsp;
  <a href="http://localhost:8080" title="API Gateway — http://localhost:8080"><img src="https://cdn.simpleicons.org/springboot/6DB33F" height="32" alt="API Gateway"></a>&nbsp;
  <a href="http://localhost:8080/swagger-ui.html" title="Swagger UI — http://localhost:8080/swagger-ui.html"><img src="https://cdn.simpleicons.org/swagger/85EA2D" height="32" alt="Swagger UI"></a>&nbsp;
  <a href="http://localhost:8085" title="Keycloak OIDC (realm: finance) — http://localhost:8085"><img src="https://cdn.simpleicons.org/keycloak/4D4DFF" height="32" alt="Keycloak"></a>&nbsp;
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

### Demo users

| User | Password | Role |
|------|----------|------|
| `user1` | `123456` | USER |
| `admin1` | `123456` | ADMIN |

Keycloak admin console: http://localhost:8085/admin — `admin` / `admin`

Market data delay: see the note under [Project structure](#project-structure). Logs: `docker compose logs -f market-data-service`

## Local development (summary)

1. Infrastructure: `cd Docker && docker compose up -d postgres redis kafka keycloak`
2. Backend: `mvn -pl finance-api,market-data-service -am spring-boot:run` (ports: [docs/english/services.md](docs/english/services.md))
3. Frontend: `cd frontend-web && npm install && npm run dev` (no `.env.development` needed with the full Docker stack)

Details: [docs/english/getting-started.md](docs/english/getting-started.md) (Path B / C).

## Documentation

Technical guides live under `docs/english/`. Other languages: [docs/README.md](docs/README.md) · [README.tr.md](README.tr.md) · [README.de.md](README.de.md)

| Document | Contents |
|----------|----------|
| [docs/english/README.md](docs/english/README.md) | English documentation index |
| [docs/english/getting-started.md](docs/english/getting-started.md) | Setup (Docker, hybrid, frontend) |
| [docs/english/architecture.md](docs/english/architecture.md) | Architecture and data flow |
| [docs/english/services.md](docs/english/services.md) | Services and ports |
| [docs/english/api.md](docs/english/api.md) | Gateway routes and OpenAPI |
| [docs/english/configuration.md](docs/english/configuration.md) | `Docker/.env` and environment variables |
| [docs/english/development.md](docs/english/development.md) | Development and testing |
| [docs/english/observability.md](docs/english/observability.md) | Metrics, trace, and logs |

## Disclaimer

This repository is a **demo and learning project**. It does not provide investment, tax, or legal advice. Live market data depends on third-party APIs and their terms of use. Use in production or with real money is at your own risk.

## License

This project is licensed under the [MIT License](LICENSE).
