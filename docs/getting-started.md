# Başlangıç

## Önkoşullar

| Araç | Sürüm / not |
|------|-------------|
| Docker Desktop | Compose v2, yeterli RAM |
| JDK | 21 |
| Maven | 3.9+ (wrapper yoksa sistem Maven) |
| Node.js | 20+ (`frontend-web` için) |

## Yol A — Tam stack (Docker Compose)

Tüm altyapı ve uygulama servislerini tek komutla ayağa kaldırır. Yeni geliştiriciler için önerilen yol.

```bash
cd Docker
cp .env.example .env
```

`.env.example` dosyasını `.env` olarak kopyalamanız yeterlidir. Şablonda `NEWS_DB_PASSWORD=123456` hazır gelir (PostgreSQL şifresiyle aynı; değiştirmenize gerek yok).

İsteğe bağlı (yorum satırını açıp doldurun): `MARKET_EVDS_API_KEY`, `OPENAI_API_KEY`, `SMTP_*`, `APP_MFA_*` — açıklamalar [configuration.md](configuration.md).

```bash
docker compose up -d --build
```

### Doğrulama

| Kontrol | Beklenen |
|---------|----------|
| http://localhost:5173 | Portal açılır |
| http://localhost:8080/actuator/health (gateway üzerinden finance health proxy değil — doğrudan container network) | Servisler healthy |
| http://localhost:8085 | Keycloak admin konsolu (`admin` / `admin`) |
| `docker compose ps` | `finance-api`, `api-gateway`, `postgres` healthy |

Giriş: `user1` / `123456` veya `admin1` / `123456`.

### İlk çalıştırma süreleri

- `finance-api` ve `market-data-service` Flyway migration + (opsiyonel) fiyat geçmişi backfill nedeniyle birkaç dakika sürebilir.
- OpenSearch ilk boot ~1 dk; `log-consumer-service` OpenSearch healthy olduktan sonra log yazar.

### Durdurma

```bash
docker compose down
# Veriyi silmek için: docker compose down -v
```

## Yol B — Hibrit (altyapı Docker, uygulama yerel)

Altyapıyı container’da, IDE’den servis çalıştırmak için:

```bash
cd Docker
docker compose up -d postgres redis kafka keycloak opensearch
```

PostgreSQL: `localhost:5432`, DB `finance`, kullanıcı/şifre `finance` / `123456`.

### finance-api

```bash
mvn -pl finance-api -am spring-boot:run -Dspring-boot.run.profiles=dev,kafka
```

Ortam (örnek):

```bash
export JWT_ISSUER_URI=http://localhost:8085/realms/finance
export JWT_JWK_SET_URI=http://localhost:8085/realms/finance/protocol/openid-connect/certs
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export CLIENTS_MARKET_DATA_BASE_URL=http://localhost:8082
```

Varsayılan HTTP portu: **8080**.

### market-data-service

```bash
mvn -pl market-data-service -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Dev profili: port **8082**, datasource H2 (bellek). Gerçek EVDS/Finnhub için `application-dev.yml` ve ortam anahtarlarını yapılandırın.

### api-gateway (yerel)

```bash
mvn -pl api-gateway -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Port **9090** (`application-dev.yml`). Frontend’de:

```env
VITE_PROXY_TARGET=http://localhost:9090
VITE_DEV_PROXY_GATEWAY=true
```

> **Not:** Docker’daki Prometheus host portu da `9090`; gateway dev ile çakışır. İkisini aynı anda host’ta çalıştırmayın veya Prometheus portunu değiştirin.

### frontend-web

```bash
cd frontend-web
cp .env.example .env.development
npm install
npm run dev
```

http://localhost:5173 — varsayılan proxy `finance-api:8080` veya gateway’e yukarıdaki env ile.

## Yol C — Sadece frontend + uzak API

`.env.development`:

```env
VITE_API_BASE_URL=https://your-api-host
```

CORS ve Keycloak redirect URI’lerinin bu origin’e izin vermesi gerekir.

## Sık karşılaşılan sorunlar

| Belirti | Olası neden | Çözüm |
|---------|-------------|--------|
| 401 tüm API’lerde | Token süresi / yanlış issuer | Keycloak `8085`, gateway `JWT_ISSUER_URI` uyumu |
| Piyasa listesi boş | MDS henüz backfill yapmadı | `market-data-service` logları, birkaç dakika bekle |
| Haber servisi restart | `NEWS_DB_PASSWORD` eksik | `Docker/.env` içinde tanımlayın |
| EVDS verisi yok | Boş `MARKET_EVDS_API_KEY=` satırı | Satırı silin veya geçerli anahtar yazın — [configuration.md](configuration.md) |
| OpenSearch şifre hatası | Eski volume | `.env.example` içindeki volume silme notları |

Sonraki adım: [development.md](development.md), [api.md](api.md).
