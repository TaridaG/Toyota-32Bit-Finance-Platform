# Yapılandırma

Ortam değişkenleri, Spring `application*.yml` dosyaları ve Docker Compose birlikte platform davranışını belirler. Servis etkileşimleri: [services.md](services.md). Geliştirme modları: [development.md](development.md).

**Docker’da tek operasyonel dosya:** [`Docker/.env`](../../Docker/.env) (`cp .env.example .env`). Öncelik kuralları ve restart/build matrisi: [configuration-precedence.md](configuration-precedence.md). Kişisel override: [`docker-compose.override.yml.example`](../../Docker/docker-compose.override.yml.example).

---

## Yapılandırma katmanları

```mermaid
flowchart TB
  subgraph sources [Kaynaklar]
    ENVFILE[Docker/.env]
    COMPOSE[docker-compose.yml environment]
    APPYML[application.yml]
    APPPROF[application-dev.yml / docker.yml]
  end

  subgraph runtime [Çalışma anı]
    FA[finance-api]
    GW[api-gateway]
    MDS[market-data-service]
    OTH[diğer servisler]
    VITE[frontend-web Vite env]
  end

  ENVFILE --> COMPOSE
  COMPOSE --> FA
  COMPOSE --> GW
  COMPOSE --> MDS
  COMPOSE --> OTH
  APPYML --> FA
  APPPROF --> FA
  ENVFILE -.->|VITE_* build-time| VITE

  Note1[Öncelik: env değişkeni geçersiz kılar YAML default]
```

| Katman | Konum | Ne zaman değişir? |
|--------|-------|------------------|
| Compose `environment` | `Docker/docker-compose.yml` | Internal hostname, profil; API anahtarları `${VAR:?}` ile `.env` zorunlu |
| `.env` + `env_file` | `Docker/.env` | Gizli / ortama özel (TCMB, Finnhub, OpenAI, SMTP, EVDS) — tüm uygulama servisleri |
| Spring profil | `application-{profile}.yml` | dev / docker / kafka / cache-redis |
| Frontend | `frontend-web/.env.development` | Vite proxy hedefi |

---

## `.env` → servis dağılımı

```mermaid
flowchart LR
  DOTENV[Docker/.env]

  subgraph consumers [Tüketen servisler]
    NS[news-service]
    FA[finance-api]
    MDS[market-data-service]
    NOTIF[notification-service]
    GW[api-gateway]
  end

  DOTENV -->|NEWS_DB_PASSWORD| NS
  DOTENV -->|OPENAI_API_KEY AI_ENABLED| FA
  DOTENV -->|APP_MFA_* SMTP_*| FA
  DOTENV -->|MARKET_EVDS_API_KEY| MDS
  DOTENV -->|SMTP_*| NOTIF
  DOTENV -->|JWT_ISSUER_URI opsiyonel| GW

  COMPOSE[compose inline env] -->|TCMB_API_KEY FINNHUB| MDS
  COMPOSE -->|POSTGRES_PASSWORD| PG[(PostgreSQL)]
```

---

## Docker ortam dosyası

Şablon: [`Docker/.env.example`](../../Docker/.env.example)

Kullanım:

```bash
cd Docker
cp .env.example .env
```

Compose `env_file: .env` ile `finance-api` ve diğer servislere aktarır.

## Zorunlu / kritik değişkenler

| Değişken | Servis | Açıklama |
|----------|--------|----------|
| `NEWS_DB_PASSWORD` | news-service | Compose’da zorunlu (`?` syntax). `.env.example` kopyasında `123456` hazır gelir; PostgreSQL `POSTGRES_PASSWORD` ile aynı olmalı |
| `JWT_ISSUER_URI` | api-gateway | Tarayıcı issuer: `http://localhost:8085/realms/finance` |
| `KAFKA_BOOTSTRAP_SERVERS` | Tüm Kafka kullananlar | Docker: `kafka:9092`, yerel: `localhost:9092` |

**Gizliler:** `TCMB_API_KEY` ve `FINNHUB_API_KEY` yalnızca `Docker/.env` içinde tutulur (`.env.example` placeholder). `docker compose up` öncesi `.env` doldurulmalıdır; repoda gerçek anahtar yoktur.

## Piyasa verisi (TCMB / EVDS)

| Değişken | Not |
|----------|-----|
| `TCMB_API_KEY` | Zorunlu — yalnızca `Docker/.env` (EVDS / tahvil / oranlar) |
| `MARKET_EVDS_API_KEY` | **Boş satır yazmayın** (`MARKET_EVDS_API_KEY=`) — Spring boş string görür, yedek anahtar devreye girmez |
| `MARKET_EVDS_BASE_URL` | Varsayılan EVDS3 dis API |
| `PROVIDERS_FINNHUB_ENABLED` / `FINNHUB_API_KEY` | NASDAQ ve Finnhub geçmişi |
| `MARKET_HISTORY_BACKFILL_*` | İlk açılışta geçmiş fiyat doldurma |

Ayrıntılı yorumlar: `.env.example` içindeki Türkçe satırlar.

## Güvenlik ve kimlik

```mermaid
sequenceDiagram
  participant UI as frontend-web
  participant KC as Keycloak realm finance
  participant GW as api-gateway
  participant FA as finance-api

  UI->>KC: OIDC login / token
  UI->>GW: Authorization Bearer
  GW->>KC: JWKS doğrulama
  GW->>FA: X-User-Id roles headers
  FA->>FA: MFA TOTP APP_MFA_*
  FA->>KC: Admin API KEYCLOAK_*
```

| Değişken | Açıklama |
|----------|----------|
| `APP_MFA_ENCRYPTION_SECRET` | TOTP secret şifreleme |
| `APP_TRUSTED_DEVICE_SIGNING_SECRET` | Güvenilir cihaz çerezi imzası |
| `APP_SECURITY_ALLOW_HEADER_USER_FALLBACK` | Dev: `true`; prod: `false` |
| `KEYCLOAK_PORTAL_CLIENT_SECRET` | `finance-portal` client (`finance-portal-dev-secret` demo) |

Keycloak realm import: [`Docker/keycloak/realm-finance.json`](../../Docker/keycloak/realm-finance.json).

## AI (isteğe bağlı)

| Değişken | Açıklama |
|----------|----------|
| `AI_ENABLED` | Admin bilgi kartı AI |
| `OPENAI_API_KEY` | Repoya commit etmeyin |
| `OPENAI_MODEL_ADMIN_CONTENT` | Varsayılan `gpt-4.1-mini` |

## E-posta

| Değişken | Servis |
|----------|--------|
| `SPRING_MAIL_*` | finance-api kayıt doğrulama |
| `SMTP_USERNAME`, `SMTP_PASSWORD`, `NOTIFICATION_MAIL_FROM` | notification-service alarmları |

SMTP / Gmail uygulama şifresi yalnızca `Docker/.env` içinde tanımlanır — **repoya commit etmeyin**.

## Frontend

### Vite proxy modları (görsel)

```mermaid
flowchart TB
  BROWSER[Tarayıcı localhost:5173]
  VITE[Vite dev server]

  subgraph modes [Proxy modu]
    GW_MODE["VITE_DEV_PROXY_GATEWAY=true"]
    BFF_MODE[Varsayılan BFF]
    DIRECT_MODE["VITE_DEV_MARKET_DIRECT_TO_MDS=true"]
    REMOTE["VITE_API_BASE_URL set"]
  end

  GW8080[api-gateway :8080]
  GW9090[api-gateway dev :9090]
  FA8080[finance-api :8080]
  MDS8082[market-data-service :8082]
  REMOTEAPI[Uzak API host]

  BROWSER --> VITE
  VITE --> GW_MODE --> GW8080
  VITE --> GW_MODE --> GW9090
  VITE --> BFF_MODE --> FA8080
  VITE --> DIRECT_MODE --> MDS8082
  VITE --> REMOTE --> REMOTEAPI
```

[`frontend-web/.env.example`](../../frontend-web/.env.example):

| Değişken | Açıklama |
|----------|----------|
| `VITE_PROXY_TARGET` | API hedefi (gateway veya finance-api) |
| `VITE_DEV_PROXY_GATEWAY` | `true` → tüm `/api` tek hedef |
| `VITE_DEV_MARKET_DIRECT_TO_MDS` | Debug: market doğrudan MDS |
| `VITE_API_BASE_URL` | Proxy bypass (tam URL) |

## Profil avatarları

| Değişken | Varsayılan |
|----------|------------|
| `PROFILE_AVATAR_STORAGE_ROOT` | Yerel: `../../photos`, Docker: `/photos` volume |

Yapı: `photos/{userId}/avatar.jpg`.

## OpenSearch

| Değişken | Varsayılan (Compose) |
|----------|----------------------|
| `OPENSEARCH_USERNAME` / `PASSWORD` | `admin` / `123456789` |
| `OPENSEARCH_INDEX_PREFIX` | `application-logs` |

Dashboards ilk kurulum: index pattern `application-logs-*`, time field `@timestamp` ([observability.md](observability.md)).

## CORS

Gateway: `GATEWAY_CORS_ALLOWED_ORIGINS` (varsayılan `http://localhost:5173`).

Yeni frontend origin eklerken gateway + Keycloak `redirectUris` / `webOrigins` birlikte güncellenmelidir.

---

## Ortam değişkeni → dosya hızlı referans

```mermaid
flowchart TD
  Q{Ne yapılandırıyorsunuz?}
  Q -->|İlk Docker kurulum| A[cp .env.example .env]
  Q -->|Haber DB hatası| B[NEWS_DB_PASSWORD=123456]
  Q -->|EVDS verisi yok| C[MARKET_EVDS boş satır silin veya anahtar yazın]
  Q -->|Frontend API 401| D[JWT_ISSUER_URI + Keycloak 8085]
  Q -->|Vite proxy| E[frontend-web/.env.development]
  Q -->|Grafana log yok| F[OpenSearch index pattern observability.md]
  Q -->|Üretim MFA| G[APP_MFA_* APP_TRUSTED_DEVICE_*]

  A --> DOC[getting-started.md]
  B --> DOC
  C --> EVDS[configuration EVDS bölümü]
  D --> API[api.md]
  E --> DEV[development.md]
  F --> OBS[observability.md]
```

---

## İlgili belgeler

| Belge | İçerik |
|-------|--------|
| [getting-started.md](getting-started.md) | `.env` kurulum adımları |
| [configuration-precedence.md](configuration-precedence.md) | Öncelik, restart vs build |
| [development.md](development.md) | Yerel profil override |
| [observability.md](observability.md) | OpenSearch, Prometheus env |
| [services.md](services.md) | Servis port ve etkileşim diyagramları |
