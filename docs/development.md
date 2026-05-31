# Geliştirme

## Maven

Kök dizinden tüm modüller:

```bash
mvn clean verify
```

Tek modül:

```bash
mvn -pl finance-api -am test
mvn -pl market-data-service -am package
```

Parent artifact: `com.company:finance-platform:1.0.0-SNAPSHOT`.

## Spring profilleri

| Servis | Yaygın profiller |
|--------|------------------|
| finance-api | `dev`, `docker`, `kafka`, `cache-redis` |
| api-gateway | `dev` (9090), `docker` (8080) |
| market-data-service | `dev` (H2), `docker` (PostgreSQL) |

`application-*.yml` dosyalarına profil özel override yazın; ortam değişkenleri `${VAR:default}` ile geçersiz kılınır.

## Veritabanı migration

- **finance-api:** `src/main/resources/db/migration/V*.sql`
- **market-data-service:** `db/migration` (MDS tabloları)
- **analytics-service:** ayrı migration seti
- **news-service:** kendi Flyway yolu

Yeni migration eklerken geri alınamaz DDL’den kaçının; production’da Flyway repair manuel gerektirir.

## Kafka (yerel)

```bash
cd Docker && docker compose up -d kafka
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Topic’ler çoğunlukla ilk publish’te auto-create (ortam ayarına bağlı). Kritik topic adları: [architecture.md](architecture.md).

## Test

```bash
mvn -pl finance-api test
mvn -pl news-service test
mvn -pl market-data-service test
```

Test profili `application-test.yml` H2 veya Testcontainers kullanabilir; modül README’sine bakmadan varsaymayın.

Frontend:

```bash
cd frontend-web
npm run lint
npm test   # varsa — package.json scripts kontrol edin
```

`computeMeasureStats.test.ts` gibi birim testler `src` altında co-located.

## Frontend yapısı

```
frontend-web/src/
├── app/router/       # React Router, auth guard
├── pages/            # Sayfa bileşenleri
├── features/         # API client, domain hook’lar
├── shared/           # Layout, UI primitives
└── data/             # Sabit portal sayfa tanımları
```

i18n: `i18next` — çeviri dosyaları `src` altında locale klasörlerinde.

State: `zustand` store’lar `features` veya `shared` altında.

## Kod organizasyonu (backend)

`finance-api` paketleri domain-driven:

- `infrastructure/http` — REST controller
- `application` — use case / service impl
- `domain` — entity (JPA)
- `repository` veya `infrastructure/persistence`

Yeni REST uçları gateway route sırasını etkileyebilir; [api.md](api.md) güncelleyin.

## Git ve PR

- Commit mesajları: neyi ve neden değiştirdiğinizi kısa cümleyle yazın
- `.env`, gerçek API anahtarları ve `photos/` içeriği commitlenmemeli
- Docker Compose içindeki demo mail şifreleri yalnızca local demo içindir; fork’larda rotate edin

## Faydalı komutlar

```bash
# Tek servis log (Docker)
docker compose -f Docker/docker-compose.yml logs -f finance-api

# Flyway durumu (finance-api container)
docker exec finance-api ls /app  # image yapısına göre değişir

# PostgreSQL shell
docker exec -it finance-postgres psql -U finance -d finance
```
