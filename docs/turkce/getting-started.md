# Başlangıç

**32 Bit Finance Platform**'u ilk kez ayağa kaldırmak için bu rehberi kullanın. Monorepo; mikroservis backend (Spring Boot), React SPA, PostgreSQL, Redis, Kafka, Keycloak ve gözlemlenebilirlik stack'ini içerir.

| Hedef | Önerilen yol |
|-------|----------------|
| Demo / yeni geliştirici | [Yol A — Tam stack (Docker)](#yol-a--tam-stack-docker-compose) |
| IDE'den backend geliştirme | [Yol B — Hibrit](#yol-b--hibrit-altyapı-docker-uygulama-yerel) |
| Yalnızca UI, uzak API | [Yol C — Frontend + uzak API](#yol-c--sadece-frontend--uzak-api) |

Genel proje özeti: [../README.md](../../README.md). Mimari ve port detayları: [architecture.md](architecture.md), [services.md](services.md).

### Hangi kurulum yolu?

```mermaid
flowchart TD
  START[Projeyi ayağa kaldırmak istiyorum]
  START --> Q1{Backend kodu debug?}
  Q1 -->|hayır, demo| A[Yol A Tam Docker]
  Q1 -->|evet, tek servis| B[Yol B Hibrit]
  START --> Q2{Sadece UI?}
  Q2 -->|evet| C[Yol C Frontend + uzak API]
  Q2 -->|hayır| Q1

  A --> A1[cd Docker && cp .env && compose up]
  B --> B1[Docker altyapı + mvn spring-boot:run]
  C --> C1[VITE_API_BASE_URL + npm run dev]
```

---

## Önkoşullar

| Araç | Sürüm / not |
|------|-------------|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | Compose v2; **tam stack için ~8 GB RAM** |
| JDK | 21 (hibrit / yerel backend) |
| Maven | 3.9+ |
| Node.js | 20+ (`frontend-web`) |

Depoyu klonlayın:

```bash
git clone https://github.com/TaridaG/Toyota-32Bit-Finance-Platform.git
cd Toyota-32Bit-Finance-Platform
```

---

## Yol A — Tam stack (Docker Compose)

Tüm altyapı ve uygulama servislerini tek komutla başlatır. **Yeni geliştiriciler ve demo için önerilen yoldur.**

### 1. Ortam dosyası

```bash
cd Docker
cp .env.example .env
```

Windows (PowerShell):

```powershell
cd Docker
Copy-Item .env.example .env
```

`.env.example` kopyalandığında `NEWS_DB_PASSWORD=123456` hazır gelir (PostgreSQL şifresiyle aynı olmalı). Demo için **başka bir değişken yazmanız gerekmez.**

Şifreyi değiştirirseniz `docker-compose.yml` içindeki `POSTGRES_PASSWORD` ile birlikte güncelleyin.

### 2. Stack'i başlat

```bash
docker compose up -d --build
```

### Docker ayağa kalkma sırası (özet)

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

İlk build Maven derlemeleri nedeniyle birkaç dakika sürebilir. Mevcut bir kurulumda veritabanını korumak için yalnızca `up --build` kullanın; **`docker compose down -v` volume'ları siler.**

### 3. Doğrulama

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

| Kontrol | Adres | Beklenen |
|---------|-------|----------|
| Portal (SPA) | http://localhost:5173 | Landing / giriş ekranı |
| API Gateway | http://localhost:8080 | HTTP 200 veya yönlendirme |
| Gateway health | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| Swagger UI | http://localhost:8080/swagger-ui.html | OpenAPI arayüzü |
| Keycloak (realm: `finance`) | http://localhost:8085 | OIDC sunucusu |
| Container durumu | `docker compose ps` | `finance-postgres`, `finance-api`, `finance-redis` **healthy** |

**Demo giriş (portal):**

| Kullanıcı | Şifre | Rol |
|-----------|-------|-----|
| `user1` | `123456` | USER |
| `admin1` | `123456` | ADMIN (admin paneli, bilgi kartları) |

Keycloak yönetim konsolu: http://localhost:8085/admin — `admin` / `admin` (compose varsayılanı).

### 4. Gözlemlenebilirlik ve altyapı

Stack ayaktayken erişilebilir uçlar:

| Bileşen | Adres | Kimlik bilgisi |
|---------|-------|----------------|
| Grafana | http://localhost:3000 | `admin` / `admin` |
| Prometheus | http://localhost:9090 | — |
| Jaeger | http://localhost:16686 | — |
| OpenSearch Dashboards | http://localhost:5601 | `admin` / `123456789` |
| OpenSearch REST | https://localhost:9200 | `admin` / `123456789` |
| PostgreSQL | `localhost:5432` | `finance` / `123456`, DB: `finance` |
| Redis | `localhost:6379` | — |
| Kafka | `localhost:9092` | — |

OpenSearch log indeksi için ilk kurulumda bir kez `application-logs-*` index pattern oluşturmanız gerekebilir — ayrıntı: [observability.md](observability.md).

### 5. İlk çalıştırma süreleri

- **Flyway migration:** `finance-api`, `market-data-service`, `analytics-service` ve `news-service` ilk açılışta şemalarını uygular; birkaç dakika normaldir.
- **Piyasa verisi backfill:** `market-data-service` katalog ve geçmiş fiyatları arka planda doldurur. Tamamlanması enstrüman sayısına bağlı olarak **yaklaşık 30 dakika** sürebilir; ilk dakikalarda boş liste veya eksik grafik görmek normaldir.
- **OpenSearch:** İlk boot ~1 dakika; `log-consumer-service` cluster healthy olduktan sonra log yazar.

İlerlemeyi izlemek:

```bash
docker compose logs -f market-data-service
docker compose logs -f finance-api
```

Demo zamanlayıcı aralıkları ücretsiz harici API kotasına göre ayarlanmıştır (TCMB EVDS, Yahoo, CoinGecko vb.). Sıklaştırma: [configuration.md](configuration.md).

### 6. Durdurma

```bash
cd Docker
docker compose down
```

Veritabanı ve OpenSearch volume'larını **silmek istemiyorsanız** `-v` bayrağını kullanmayın:

```bash
# DİKKAT: Tüm kalıcı veriyi siler (PostgreSQL, OpenSearch, Kafka)
docker compose down -v
```

---

## İsteğe bağlı `.env` değişkenleri

Demo için `TCMB_API_KEY` ve `FINNHUB_API_KEY` değerleri `docker-compose.yml` içinde tanımlıdır; `.env`'e yazmanız gerekmez.

| Değişken | Ne zaman? | Not |
|----------|-----------|-----|
| `MARKET_EVDS_API_KEY` | TCMB EVDS için ayrı anahtar | Tanımlamazsanız compose'daki `TCMB_API_KEY` kullanılır. **Boş satır (`KEY=`) yazmayın** |
| `OPENAI_API_KEY` | Admin bilgi kartı AI | `AI_ENABLED=true`; anahtar yoksa AI devre dışı kalır |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | Kendi e-postanızdan alarm ve kayıt maili | Tanımlamazsanız compose demo SMTP kullanır |
| `APP_MFA_ENCRYPTION_SECRET` / `APP_TRUSTED_DEVICE_SIGNING_SECRET` | Üretim ortamı | Demo'da compose varsayılanları yeterli |

Tüm değişkenler: [configuration.md](configuration.md).

---

## Yol B — Hibrit (altyapı Docker, uygulama yerel)

Altyapıyı container'da çalıştırıp backend veya frontend'i IDE / terminalden debug etmek için.

### 1. Altyapı servisleri

```bash
cd Docker
docker compose up -d postgres redis kafka keycloak opensearch
```

PostgreSQL: `localhost:5432`, veritabanı `finance`, kullanıcı / şifre `finance` / `123456`.

### 2. finance-api

```bash
# repo kökünden
mvn -pl finance-api -am spring-boot:run -Dspring-boot.run.profiles=dev,kafka
```

Örnek ortam değişkenleri (bash):

```bash
export JWT_ISSUER_URI=http://localhost:8085/realms/finance
export JWT_JWK_SET_URI=http://localhost:8085/realms/finance/protocol/openid-connect/certs
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export CLIENTS_MARKET_DATA_BASE_URL=http://localhost:8082
```

Varsayılan HTTP portu: **8080**.

### 3. market-data-service

```bash
mvn -pl market-data-service -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Dev profili: port **8082**, bellek içi H2. Gerçek EVDS / Finnhub verisi için `application-dev.yml` ve ortam anahtarlarını yapılandırın.

> `news-service` de host'ta **8082** kullanır; **market-data-service ile aynı makinede aynı anda 8082'de çalıştırmayın.** Docker ağında hostname ile ayrılırlar.

### 4. api-gateway (yerel)

```bash
mvn -pl api-gateway -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Dev profili portu: **9090** (`application-dev.yml`).

Frontend'de gateway'e yönlendirmek için `frontend-web/.env.development`:

```env
VITE_PROXY_TARGET=http://localhost:9090
VITE_DEV_PROXY_GATEWAY=true
```

> Docker'daki Prometheus host portu da **9090**'dır. Gateway dev ile Prometheus'u aynı anda host'ta çalıştırmayın veya Prometheus portunu değiştirin.

Tam servis listesi ve portlar: [services.md](services.md).

### 5. frontend-web

```bash
cd frontend-web
cp .env.example .env.development   # Windows: Copy-Item .env.example .env.development
npm install
npm run dev
```

http://localhost:5173 — varsayılan proxy `finance-api:8080` veya yukarıdaki gateway env ile.

Diğer backend modüllerini (`analytics-service`, `news-service`, `notification-service`) tam akış için Docker'da bırakabilir veya modül başına ayrı terminalde `spring-boot:run` çalıştırabilirsiniz.

---

## Yol C — Sadece frontend + uzak API

`frontend-web/.env.development`:

```env
VITE_API_BASE_URL=https://your-api-host
```

CORS, Keycloak redirect URI'leri ve gateway güvenlik ayarlarının bu origin'e izin vermesi gerekir. Ayrıntı: [api.md](api.md).

---

## Sık karşılaşılan sorunlar

| Belirti | Olası neden | Çözüm |
|---------|-------------|--------|
| 401 tüm API'lerde | Süresi dolmuş token veya issuer uyumsuzluğu | Keycloak `8085`; gateway `JWT_ISSUER_URI` = `http://localhost:8085/realms/finance` |
| Piyasa listesi / grafikler boş | MDS backfill devam ediyor | Birkaç dakika bekleyin; `docker compose logs -f market-data-service` |
| `news-service` sürekli restart | `NEWS_DB_PASSWORD` eksik veya yanlış | `Docker/.env` içinde `NEWS_DB_PASSWORD=123456` (PostgreSQL ile aynı) |
| EVDS / politika faizi verisi yok | Boş `MARKET_EVDS_API_KEY=` satırı | Satırı silin veya geçerli anahtar yazın — [configuration.md](configuration.md) |
| OpenSearch auth hatası | Eski volume, şifre uyumsuz | `.env.example` içindeki volume silme notlarına bakın |
| Port çakışması (9090) | Yerel gateway dev + Docker Prometheus | Birini durdurun veya portu değiştirin |
| Build çok uzun sürüyor | İlk Maven derlemesi | Normal; sonraki `up` çağrıları cache kullanır |

---

## Sonraki adımlar

| Konu | Belge |
|------|-------|
| Geliştirme, test, profiller | [development.md](development.md) |
| Gateway rotaları ve Swagger | [api.md](api.md) |
| Servis sorumlulukları | [services.md](services.md) |
| Ortam değişkenleri | [configuration.md](configuration.md) |
| Metrik, trace, log | [observability.md](observability.md) |
