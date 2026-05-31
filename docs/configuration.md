# Yapılandırma

## Docker ortam dosyası

Şablon: [`Docker/.env.example`](../Docker/.env.example)

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

**Demo notu:** `TCMB_API_KEY` ve `FINNHUB_API_KEY` şu an `docker-compose.yml` içinde tanımlı; `.env`’e yazmadan stack ayağa kalkar.

## Piyasa verisi (TCMB / EVDS)

| Değişken | Not |
|----------|-----|
| `TCMB_API_KEY` | Compose varsayılan demo anahtarı; üretimde kendi anahtarınız |
| `MARKET_EVDS_API_KEY` | **Boş satır yazmayın** (`MARKET_EVDS_API_KEY=`) — Spring boş string görür, yedek anahtar devreye girmez |
| `MARKET_EVDS_BASE_URL` | Varsayılan EVDS3 dis API |
| `PROVIDERS_FINNHUB_ENABLED` / `FINNHUB_API_KEY` | NASDAQ ve Finnhub geçmişi |
| `MARKET_HISTORY_BACKFILL_*` | İlk açılışta geçmiş fiyat doldurma |

Ayrıntılı yorumlar: `.env.example` içindeki Türkçe satırlar.

## Güvenlik ve kimlik

| Değişken | Açıklama |
|----------|----------|
| `APP_MFA_ENCRYPTION_SECRET` | TOTP secret şifreleme |
| `APP_TRUSTED_DEVICE_SIGNING_SECRET` | Güvenilir cihaz çerezi imzası |
| `APP_SECURITY_ALLOW_HEADER_USER_FALLBACK` | Dev: `true`; prod: `false` |
| `KEYCLOAK_PORTAL_CLIENT_SECRET` | `finance-portal` client (`finance-portal-dev-secret` demo) |

Keycloak realm import: [`Docker/keycloak/realm-finance.json`](../Docker/keycloak/realm-finance.json).

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

Docker demo Gmail uygulama şifresi içerir — **üretimde kullanmayın**.

## Frontend

[`frontend-web/.env.example`](../frontend-web/.env.example):

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
