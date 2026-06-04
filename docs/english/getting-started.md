# Getting Started

Use this guide to bring up **32 Bit Finance Platform** for the first time. The monorepo includes a microservice backend (Spring Boot), React SPA, PostgreSQL, Redis, Kafka, Keycloak, and an observability stack.

| Goal | Recommended path |
|------|------------------|
| Demo / new developer | [Path A — Full stack (Docker)](#path-a--full-stack-docker-compose) |
| Backend development from IDE | [Path B — Hybrid](#path-b--hybrid-infrastructure-docker-app-local) |
| UI only, remote API | [Path C — Frontend + remote API](#path-c--frontend-only--remote-api) |

General project overview: [../README.md](../../README.md). Architecture and port details: [architecture.md](architecture.md), [services.md](services.md).

### Which setup path?

```mermaid
flowchart TD
  START[Projeyi ayağa kaldırmak istiyorum]
  START --> Q1{Backend kodu debug?}
  Q1 -->|hayır, demo| A[Yol A Tam Docker]
  Q1 -->|evet, tek servis| B[Yol B Hibrit]
  START --> Q2{Sadece UI?}
  Q2 -->|evet| C[Yol C Frontend + uzak API]
  Q2 -->|hayır| Q1

  A --> A1[Copy .env - Fill keys - compose up]
  B --> B1[Docker altyapı + mvn spring-boot:run]
  C --> C1[VITE_API_BASE_URL + npm run dev]
```

---

## Prerequisites

| Tool | Version / notes |
|------|-----------------|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | Compose v2; **~8 GB RAM for full stack** |
| JDK | 21 (hybrid / local backend) |
| Maven | 3.9+ |
| Node.js | 20+ (`frontend-web`) |

Clone the repository:

```bash
git clone https://github.com/TaridaG/Toyota-32Bit-Finance-Platform.git
cd Toyota-32Bit-Finance-Platform
```

---

## Path A — Full stack (Docker Compose)

Starts all infrastructure and application services with a single command. **Recommended path for new developers and demos.**

> **Important:** `cp .env.example .env` only creates the file. You must **fill in** `Docker/.env` for market data and email (no real keys in the repo).

| Step | What you do |
|------|-------------|
| **1** | Create `Docker/.env` (copy from template) |
| **2** | **Required:** set `TCMB_API_KEY`, `FINNHUB_API_KEY` |
| **3** | **Optional:** SMTP (registration / alert email), OpenAI (admin AI) |
| **4** | `docker compose up -d --build` |

### 1. Create the file

```bash
cd Docker
cp .env.example .env
```

Windows (PowerShell):

```powershell
cd Docker
Copy-Item .env.example .env
```

Template: [`Docker/.env.example`](../../Docker/.env.example).

### 2. Edit the file (required)

Open `Docker/.env` in a text editor. **Do not leave these empty:**

```env
# Required — market data
TCMB_API_KEY=your-evds-key
FINNHUB_API_KEY=your-finnhub-key
```

`POSTGRES_PASSWORD` and `NEWS_DB_PASSWORD` default to `123456` in the template for local demo; change them in production.

**What works without keys / SMTP?**

| Feature | Without keys / SMTP |
|---------|---------------------|
| Portal login (`admin1` / `123456`) | Works |
| Market lists, charts, rate cards | Empty or incomplete (TCMB + Finnhub required) |
| New user registration email | Does not work (SMTP required) |
| Alert email | Does not work (SMTP required) |
| Admin info card AI | Off (`OPENAI_API_KEY` + `AI_ENABLED=true` required) |

> Run Step 4 `docker compose up` **after** you save the keys. No `--force-recreate` needed on first install.

### 3. Optional features

For email or AI, remove the leading `#` on the relevant lines in `.env` and fill in values (example):

```env
# Registration (finance-api) + alert mail (notification-service)
# Gmail: use an app password, not your normal login password
SMTP_USERNAME=you@gmail.com
SMTP_PASSWORD=16-char-app-password
SPRING_MAIL_USERNAME=you@gmail.com
SPRING_MAIL_PASSWORD=16-char-app-password
APP_REGISTRATION_VERIFICATION_FROM=you@gmail.com
NOTIFICATION_MAIL_FROM=you@gmail.com

# Admin info card AI (finance-api)
# AI_ENABLED=true
# OPENAI_API_KEY=sk-...
```

| Variable | Purpose | Note |
|----------|---------|------|
| `MARKET_EVDS_API_KEY` | Separate TCMB EVDS key | Falls back to `TCMB_API_KEY`. **Do not set an empty line (`KEY=`)** |
| `APP_MFA_ENCRYPTION_SECRET` / `APP_TRUSTED_DEVICE_SIGNING_SECRET` | Production MFA | Compose defaults are enough for local demo |

If the stack is **already running** and you change `.env`, recreate the affected service:

```bash
docker compose up -d --force-recreate market-data-service   # TCMB / Finnhub
docker compose up -d --force-recreate finance-api             # OpenAI / registration mail
docker compose up -d --force-recreate notification-service    # SMTP / alerts
```

All variables: [configuration.md](configuration.md).

### 4. Start the stack

```bash
docker compose up -d --build
```

### Docker startup order (summary)

```mermaid
flowchart TD
  T0[docker compose up --build]
  T1[postgres redis kafka]
  T2[keycloak opensearch]
  T3[finance-api Flyway healthy]
  T4[market-data-service backfill]
  T5[news analytics notification]
  T6[api-gateway frontend-web]
  T7[prometheus grafana log-consumer]

  T0 --> T1 --> T2 --> T3 --> T4 --> T5 --> T6 --> T7

  T3 -.->|MDS bu adımı bekler| T4
  T2 -.->|log-consumer OS healthy| T7
```

The first build may take several minutes due to Maven compilations. On an existing setup, use only `up --build` to preserve the database; **`docker compose down -v` deletes volumes.**

### 5. Verification

```mermaid
flowchart TD
  V1[docker compose ps healthy]
  V2[localhost:5173 portal]
  V3[localhost:8080 actuator/health]
  V4[admin1 / 123456 giriş]
  V5[Prometheus targets UP]
  V6[opsiyonel Grafana 3000]

  V1 --> V2 --> V3 --> V4
  V3 --> V5 --> V6
```

| Check | Address | Expected |
|-------|---------|----------|
| Portal (SPA) | http://localhost:5173 | Landing / login screen |
| API Gateway | http://localhost:8080 | HTTP 200 or redirect |
| Gateway health | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| Swagger UI | http://localhost:8080/swagger-ui.html | OpenAPI UI |
| Keycloak (realm: `finance`) | http://localhost:8085 | OIDC server |
| Container status | `docker compose ps` | `finance-postgres`, `finance-api`, `finance-redis` **healthy** |

**Demo login (portal):**

| User | Password | Role |
|------|----------|------|
| `user1` | `123456` | USER |
| `admin1` | `123456` | ADMIN (admin panel, info cards) |

Keycloak admin console: http://localhost:8085/admin — `admin` / `admin` (compose default).

### 6. Observability and infrastructure

Endpoints accessible while the stack is running:

| Component | Address | Credentials |
|-----------|---------|-------------|
| Grafana | http://localhost:3000 | `admin` / `admin` |
| Prometheus | http://localhost:9090 | — |
| Jaeger | http://localhost:16686 | — |
| OpenSearch Dashboards | http://localhost:5601 | `admin` / `123456789` |
| OpenSearch REST | https://localhost:9200 | `admin` / `123456789` |
| PostgreSQL | `localhost:5432` | `finance` / `123456`, DB: `finance` |
| Redis | `localhost:6379` | — |
| Kafka | `localhost:9092` | — |

For the OpenSearch log index, you may need to create an `application-logs-*` index pattern once on first setup — details: [observability.md](observability.md).

### 7. First-run timings

- **Flyway migration:** `finance-api`, `market-data-service`, `analytics-service`, and `news-service` apply their schemas on first startup; a few minutes is normal.
- **Market data backfill:** `market-data-service` fills the catalog and historical prices in the background. Completion may take **approximately 30 minutes** depending on instrument count; seeing empty lists or incomplete charts in the first minutes is normal.
- **OpenSearch:** First boot ~1 minute; `log-consumer-service` writes logs after the cluster is healthy.

To monitor progress:

```bash
docker compose logs -f market-data-service
docker compose logs -f finance-api
```

Demo scheduler intervals are tuned for free external API quotas (TCMB EVDS, Yahoo, CoinGecko, etc.). To tighten intervals: [configuration.md](configuration.md).

### 8. Shutdown

```bash
cd Docker
docker compose down
```

Do **not** use the `-v` flag if you do not want to delete database and OpenSearch volumes:

```bash
# WARNING: Deletes all persistent data (PostgreSQL, OpenSearch, Kafka)
docker compose down -v
```

---

## Path B — Hybrid (infrastructure Docker, app local)

Run infrastructure in containers and debug backend or frontend from IDE / terminal.

### 1. Infrastructure services

```bash
cd Docker
docker compose up -d postgres redis kafka keycloak opensearch
```

PostgreSQL: `localhost:5432`, database `finance`, user / password `finance` / `123456`.

### 2. finance-api

```bash
# from repo root
mvn -pl finance-api -am spring-boot:run -Dspring-boot.run.profiles=dev,kafka
```

Example environment variables (bash):

```bash
export JWT_ISSUER_URI=http://localhost:8085/realms/finance
export JWT_JWK_SET_URI=http://localhost:8085/realms/finance/protocol/openid-connect/certs
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export CLIENTS_MARKET_DATA_BASE_URL=http://localhost:8082
```

Default HTTP port: **8080**.

### 3. market-data-service

```bash
mvn -pl market-data-service -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Dev profile: port **8082**, in-memory H2. Configure `application-dev.yml` and environment keys for real EVDS / Finnhub data.

> `news-service` also uses **8082** on the host; **do not run it on 8082 at the same time as market-data-service on the same machine.** They are separated by hostname on the Docker network.

### 4. api-gateway (local)

```bash
mvn -pl api-gateway -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Dev profile port: **9090** (`application-dev.yml`).

To point the frontend at the gateway, set `frontend-web/.env.development`:

```env
VITE_PROXY_TARGET=http://localhost:9090
VITE_DEV_PROXY_GATEWAY=true
```

> Docker Prometheus host port is also **9090**. Do not run gateway dev and Prometheus on the host at the same time, or change the Prometheus port.

Full service list and ports: [services.md](services.md).

### 5. frontend-web

```bash
cd frontend-web
npm install
npm run dev
```

For the full Docker stack at http://localhost:5173, `frontend-web/.env.development` is **not required** (nginx + gateway proxy). Only for local `npm run dev` or hybrid proxy:

```bash
cp .env.example .env.development   # Windows: Copy-Item .env.example .env.development
```

http://localhost:5173 — default proxy targets `finance-api:8080` or the gateway env above.

You can leave other backend modules (`analytics-service`, `news-service`, `notification-service`) in Docker for the full flow, or run `spring-boot:run` per module in separate terminals.

---

## Path C — Frontend only + remote API

`frontend-web/.env.development`:

```env
VITE_API_BASE_URL=https://your-api-host
```

CORS, Keycloak redirect URIs, and gateway security settings must allow this origin. Details: [api.md](api.md).

---

## Common issues

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| 401 on all APIs | Expired token or issuer mismatch | Keycloak `8085`; gateway `JWT_ISSUER_URI` = `http://localhost:8085/realms/finance` |
| Empty market list / charts | MDS backfill in progress | Wait a few minutes; `docker compose logs -f market-data-service` |
| `news-service` keeps restarting | Missing or wrong `NEWS_DB_PASSWORD` | `NEWS_DB_PASSWORD=123456` in `Docker/.env` (same as PostgreSQL) |
| No EVDS / policy rate data | Empty `MARKET_EVDS_API_KEY=` line | Remove the line or set a valid key — [configuration.md](configuration.md) |
| OpenSearch auth error | Old volume, password mismatch | See volume deletion notes in `.env.example` |
| Port conflict (9090) | Local gateway dev + Docker Prometheus | Stop one or change the port |
| Build takes very long | First Maven build | Normal; subsequent `up` calls use cache |

---

## Next steps

| Topic | Document |
|-------|----------|
| Development, tests, profiles | [development.md](development.md) |
| Gateway routes and Swagger | [api.md](api.md) |
| Service responsibilities | [services.md](services.md) |
| Environment variables | [configuration.md](configuration.md) |
| Metrics, traces, logs | [observability.md](observability.md) |
