# API Gateway Frontend Geliştirme Rehberi

Bu doküman `api-gateway` odaklıdır ve frontend geliştirirken gateway tarafında bilinmesi gereken endpointleri, veri akışlarını, güvenlik davranışlarını ve operasyonel notları toplar.

## 1) API Gateway Amacı ve Rolü

`api-gateway`, frontend ile backend servisleri arasındaki tek giriş noktasıdır.

- Kimlik doğrulama ve rol bazlı yetkilendirme uygular (JWT, Keycloak issuer).
- İstekleri ilgili mikroservise yönlendirir.
- Bazı path'lerde rewrite yapar.
- Circuit breaker ile fallback cevabı döner.
- `X-Correlation-Id` üretir/yayar.
- JWT'den kullanıcı bilgilerini çıkarıp backend'e güvenli header olarak taşır.
- `finance-api` route'u için rate limiting uygular.

## 2) Ortam ve Base URL

- Docker profilde gateway portu: `8080`
- Dev profilde gateway portu: `9090`

Frontend için Docker stack'te beklenen ana base:

- `http://localhost:8080`

Not: `frontend-web` içinde şu an axios base URL `http://localhost:8080/api` olarak sabit.
Bu yapı `/api/*` endpointleri için uygun, ama `/market/*` çağrıları için ya ikinci client gerekir ya da base URL sadece host olacak şekilde (`http://localhost:8080`) düzenlenmelidir.

## 3) Güvenlik Kuralları (Frontend için kritik)

Gateway security kuralları:

- `permitAll`: `/actuator/**`, `/health`
- `ROLE_ADMIN`: `/api/admin/**`
- `ROLE_USER` veya `ROLE_ADMIN`: `/api/**`, `/market/**`
- Diğer her şey: authenticated

Sonuç:

- Frontend'den API çağrılarının neredeyse tamamında `Authorization: Bearer <token>` zorunlu.
- JWT claim'lerinden rol çıkarımı `realm_access.roles` üzerinden yapılıyor.

## 4) Gateway'in Backend'e Eklediği Header'lar

Gateway, istemciden gelen spoof edilmiş kullanıcı header'larını temizler ve JWT'den yeniden yazar:

- `X-USER-ID`
- `X-USERNAME`
- `X-USER-ROLES`

Ek olarak:

- `X-Correlation-Id` request'te yoksa gateway üretir.
- Aynı header response'a da geri eklenir.

Frontend notu:

- `X-USER-*` header'larını frontend elle set etmemeli.
- Backend kullanıcı bağlamını gateway'nin JWT'den üretmesine bırakmalısın.

## 5) Rate Limit Davranışı

Rate limit yalnızca `finance-api` route'unda (`/api/**`, `/health`) aktif:

- Refill: saniyede 20 token
- Burst: 40
- Key: önce `X-USER-ID`, yoksa client IP

Frontend tarafında 429 handling (retry/backoff + kullanıcı mesajı) olmalı.

## 6) Route Haritası (Gateway Dış Yüzeyi -> İç Servis)

### 6.1 Finance API route

- Gateway path: `/api/**` ve `/health`
- Hedef servis: `finance-api`
- Rewrite: yok
- Circuit breaker fallback: `/fallback/finance`

### 6.2 Market Data route

- Gateway path: `/market/**`
- Rewrite: `/market/{*}` -> `/api/market/{*}`
- Hedef servis: `market-data-service`
- Circuit breaker fallback: `/fallback/market`

### 6.3 News route

- Gateway path: `/api/news/**`
- Hedef servis: `news-service`
- Rewrite: yok
- Circuit breaker fallback: `/fallback/news`

### 6.4 Reporting route

- Gateway path: `/api/reports/**`
- Hedef servis: `reporting-service`
- Rewrite: yok
- Circuit breaker fallback: `/fallback/reporting`

### 6.5 Analytics route

- Gateway path: `/api/analytics/**`
- Hedef servis: `analytics-service`
- Rewrite: yok
- Circuit breaker fallback: `/fallback/analytics`

## 7) Frontend'in Kullanabileceği Endpoint Envanteri (Gateway Üzerinden)

Asagidaki liste frontend'in gateway'e çağıracağı path'leri verir (yani dış path).

## 7.1 Finance API (gateway: `/api/**`)

### Kullanıcı

- `POST /api/users`
- `GET /api/users/{id}`
- `GET /api/users/{id}/balance`

### Enstrüman

- `GET /api/instruments`

### Trade

- `POST /api/trades/buy?instrumentId={id}&quantity={qty}`
- `POST /api/trades/sell?instrumentId={id}&quantity={qty}`

### Portfolio

- `GET /api/portfolio`
- `GET /api/portfolio/summary`
- `GET /api/portfolio/snapshots`

### External Portfolio

- `POST /api/external/portfolios`
- `GET /api/external/portfolios`
- `POST /api/external/portfolios/{portfolioId}/positions`
- `DELETE /api/external/portfolios/{portfolioId}/positions/{positionId}`
- `GET /api/external/portfolios/{portfolioId}/summary`
- `GET /api/external/portfolios/{portfolioId}/allocation`

### Chart

- `GET /api/charts/{instrumentId}/candles?from={ISO-8601}&to={ISO-8601}`
- `GET /api/charts/{instrumentId}/trades`
- `GET /api/charts/{instrumentId}/alarms`

### Alarm

- `GET /api/alarms`
- `POST /api/alarms`
- `DELETE /api/alarms/{id}`

### History

- `GET /api/history/transactions`
- `GET /api/history/alarms`

### Health

- `GET /health` (public)

## 7.2 News Service (gateway: `/api/news/**`)

- `GET /api/news?category=&q=&page=&size=`
- `GET /api/news/{id}`
- `POST /api/news/admin/ingest`

Not:

- Kod yorumunda admin korumanın gateway tarafından yapılabileceği belirtilmiş.
- Mevcut security kuralı `POST /api/news/admin/ingest` için otomatik admin ayrımı yapmıyor; `ROLE_USER` da `/api/**` kapsamında geçebilir. Gerekirse gateway kuralı ayrıca keskinleştirilmeli.

## 7.3 Reporting Service (gateway: `/api/reports/**`)

- `POST /api/reports/instrument`
- `GET /api/reports/{reportId}`
- `GET /api/reports/{reportId}/download` (binary dosya döner)
- `POST /api/reports/portfolio`
- `POST /api/reports/schedules/portfolio`
- `GET /api/reports/schedules`
- `POST /api/reports/schedules/{scheduleId}/pause`
- `POST /api/reports/schedules/{scheduleId}/activate`

Frontend notu:

- `download` endpoint'i `ApiResponse` değil, dosya stream'i (`Content-Disposition: attachment`) döndürür.
- Axios'ta `responseType: 'blob'` kullanılması gerekir.

## 7.4 Analytics Service (gateway: `/api/analytics/**`)

- `GET /api/analytics/instruments/{symbol}/candles?interval=&from=&to=`
- `GET /api/analytics/instruments/{symbol}/moving-average`
- `GET /api/analytics/instruments/{symbol}/rsi`
- `GET /api/analytics/instruments/{symbol}/trend`

## 7.5 Market Data Service (gateway: `/market/**`)

Gateway rewrite'i nedeniyle dışarıdan beklenen çağrı deseni:

- `GET /market/...` -> upstream `/api/market/...`

Ancak `market-data-service` içindeki görünür controller endpoint'i:

- `GET /api/admin/providers/{provider}`

Bu haliyle route uyumsuzluğu riski var:

- Gateway `/market/admin/providers/{provider}` çağrısını `/api/market/admin/providers/{provider}` olarak iletir.
- Serviste mevcut endpoint `/api/admin/providers/{provider}` olduğundan 404 üretme olasılığı yüksektir.

Frontend aksiyonu:

- Market endpoint geliştirmesine başlamadan önce gateway rewrite veya market-service endpoint prefix'i netleştirilmeli.

## 8) Veri Akışları (Frontend Perspektifi)

## 8.1 Normal API çağrısı akışı

1. Frontend -> Gateway (`Authorization: Bearer <jwt>`)
2. Gateway JWT doğrular (issuer: Keycloak realm)
3. Gateway `X-USER-ID`, `X-USERNAME`, `X-USER-ROLES` header'larını üretir
4. Gateway route + (varsa) rewrite uygular
5. Hedef servis işleyip `ApiResponse<T>` döner
6. Gateway response'a `X-Correlation-Id` ekleyerek frontend'e döner

## 8.2 Servis hata / circuit breaker akışı

1. Upstream servis erişilemez veya hata verir
2. Circuit breaker fallback endpoint'ine forward eder
3. JSON body döner, ornek:
   - `{"code":"FINANCE_UNAVAILABLE","message":"...","timestamp":"..."}`

Not:

- Fallback controller metodları açıkça non-2xx status set etmiyor.
- Frontend yalnızca status code'a bakmak yerine body'deki `code/message` alanlarını da kontrol etmelidir.

## 9) Response Format Beklentisi

Cogu servis başarılı durumda:

- `{ "success": true, "data": ... }`

Hata durumlarında servis bazlı farklılık olabilir:

- `{ "success": false, "error": { ... } }`
- veya gateway fallback body (`code/message/timestamp`)
- veya gateway global error body (`code/message/timestamp/correlationId`, status 502)

Frontend'te tek tip `normalizeError` katmanı yazılması önerilir.

## 10) Frontend İçin Uygulama Kuralları (Pratik)

1. API client'ta `Authorization` header interceptor'u olmalı.
2. `X-USER-*` header'larını asla frontend'den yollama.
3. `X-Correlation-Id` response header'ını logla (debug/incident için).
4. `download` endpointlerinde blob handling uygula.
5. 401/403/429/502 için merkezi hata yonetimi oluştur.
6. `/market/*` tarafına başlamadan route uyumsuzluğunu backend ile teyit et.
7. `apiClient` base URL'ini host seviyesine çekmeyi düşün (`http://localhost:8080`) ve modül bazlı path yönet.

## 11) Bu Faz İçin Önerilen Frontend Öncelik Sırası

1. Auth/token akışını netleştir (Keycloak token alma + refresh + bearer injector)
2. Ortak API client + hata normalize katmanı
3. External Portfolio akışı (`/api/external/portfolios`) tamamla
4. Analytics ekranları (`/api/analytics/...`)
5. Reporting ekranları + dosya indirme
6. News listeleme/arama
7. Market route netleşince market modülü

## 12) Finance API Servis Dökümü (Detaylı)

Bu bölüm `backend/finance-api` için frontend açısından pratikte gerekli olan teknik resmi verir.

## 12.1 Servisin amacı

`finance-api`, yatırım işlemleri ve kullanıcıya ait core finans verilerini yönetir:

- kullanıcı ve demo bakiye yönetimi
- al/sat işlemleri
- portföy ve portföy özeti hesaplama
- alarm tanımı ve alarm geçmişi
- grafik/işlem/alarm overlay verileri
- external portfolio (manuel lot bazlı) yönetimi

## 12.2 Runtime bağımlılıkları ve profiller

Servis bağımlılıkları:

- PostgreSQL (kalıcı veri)
- Redis (fiyat cache, `cache-redis` profili)
- Kafka (fiyat event tüketimi ve outbox publish, `kafka` profili)

Bu repodaki Docker Compose ile `finance-api` tipik olarak:

- `SPRING_PROFILES_ACTIVE=kafka,cache-redis`

Bu şu anlama gelir:

- fiyatların ana kaynağı Kafka event akışı olur
- cache Redis'te tutulur
- trade/valuation endpointleri canlı fiyat yoksa hata dönebilir

## 12.3 Güvenlik ve kullanıcı bağlamı (frontend için kritik)

`finance-api` Spring Security seviyesinde neredeyse her şeyi `permitAll` bırakıyor; asıl koruma header temelli:

- `InternalRequestGuardFilter` ile `/health` hariç tüm endpointlerde `X-USERNAME` zorunlu
- `HeaderCurrentUserResolver` kullanıcıyı `X-USERNAME` ile DB'den çözüyor

Sonuç:

- frontend `finance-api`'ye doğrudan gitmemeli; gateway üzerinden gitmeli
- gateway, JWT'den `X-USERNAME` ürettiği için akış çalışır
- token'daki username DB'de kullanıcı kaydı ile eşleşmiyorsa birçok endpoint business hata döndürür

## 12.4 Hata formatı ve status davranışı

Genel response yapısı:

- başarılı: `{ "success": true, "data": ... }`
- hatalı: `{ "success": false, "error": { code, message, timestamp } }`

Exception mapping:

- `IllegalStateException` -> `409 CONFLICT` (`CONFLICT_ERROR`)
- `ResourceNotFoundException` -> `404 NOT_FOUND`
- `AccessDeniedBusinessException` -> `403 ACCESS_DENIED`
- `RuntimeException` -> `400 RUNTIME_ERROR`
- diğer `Exception` -> `500 INTERNAL_ERROR`

Frontend aksiyonu:

- sadece HTTP status değil `error.code` ile de ayrıştırma yap

## 12.5 Finance API endpoint detayları

### A) Kullanıcı

- `POST /api/users`
  - body: `{ email, username }`
  - validasyon: `email` formatı, `username` 3-30 karakter
  - yan etki: kullanıcı ile birlikte demo balance otomatik açılır (`100000`, `USD`)
- `GET /api/users/{id}`
- `GET /api/users/{id}/balance`

Not:

- login sonrası kullanıcı DB'de yoksa önce create-user bootstrap akışı gerekir; aksi halde kullanıcı çözümlenemediği için birçok endpoint başarısız olur.

### B) Enstrüman

- `GET /api/instruments`
  - alanlar: `id`, `symbol`, `name`, `type`, `exchange`

### C) Trade

- `POST /api/trades/buy?instrumentId={id}&quantity={qty}`
- `POST /api/trades/sell?instrumentId={id}&quantity={qty}`

İş kuralları:

- buy:
  - kullanıcı ve enstrüman bulunmalı
  - `MARKET` fiyat mevcut olmalı
  - demo bakiye yeterli olmalı
- sell:
  - net pozisyon (BUY-SELL toplamı) satılacak miktardan az olamaz
  - `MARKET` fiyat mevcut olmalı

Yan etkiler:

- transaction kaydı atılır
- outbox üzerinden `transaction-executed` event'i publish edilir

Sık hatalar:

- `Price not available`
- `Insufficient demo balance`
- `Insufficient position for sell`

### D) Portfolio

- `GET /api/portfolio`
- `GET /api/portfolio/summary`
- `GET /api/portfolio/snapshots`

Hesaplama davranışı:

- pozisyonlar transactionlardan türetilir
- yalnızca net quantity > 0 enstrümanlar listede kalır
- unrealized PnL, güncel `MARKET` fiyat ile hesaplanır

Snapshot davranışı:

- scheduler aktif kullanıcılar için periyodik snapshot alır
- varsayılan periyot: `600000ms` (10 dk)

### E) External Portfolio

- `POST /api/external/portfolios`
  - body: `{ name, baseCurrency? }`
  - `baseCurrency` boşsa varsayılan `TRY`
- `GET /api/external/portfolios`
- `POST /api/external/portfolios/{portfolioId}/positions`
  - body: `{ instrumentId, quantity, unitPrice, feeAmount?, feeCurrency?, acquiredAt, sourceName?, notes? }`
  - `quantity` ve `unitPrice` > 0 olmalı
- `DELETE /api/external/portfolios/{portfolioId}/positions/{positionId}` (soft delete)
- `GET /api/external/portfolios/{portfolioId}/summary`
- `GET /api/external/portfolios/{portfolioId}/allocation`

Valuation notu:

- external portfolio valuation, `PriceCacheService` üstünden `MARKET` fiyatı çeker
- fiyat cache'de yoksa `Price not found for instrumentId=...` hatası alınır

### F) Chart

- `GET /api/charts/{instrumentId}/candles?from={instant}&to={instant}`
- `GET /api/charts/{instrumentId}/trades`
- `GET /api/charts/{instrumentId}/alarms`

Candles notu:

- servis şu an 1 fiyat kaydını 1 candle olarak mapliyor (OHLC aynı değer)
- gerçek agregasyon yok; frontend chart tarafında bunu bilerek UX tasarla

### G) Alarm

- `GET /api/alarms`
- `POST /api/alarms`
- `DELETE /api/alarms/{id}`

Alarm koşul enumları:

- `GREATER_THAN`
- `LESS_THAN`
- `EQUAL`
- `PERCENT_CHANGE_UP`
- `PERCENT_CHANGE_DOWN`

Kritik kontrat notu:

- `CreateAlarmRequest` içinde `userId` alanı `@NotNull`; controller bu alanı kullanmasa da validasyon için body'de bekleniyor.
- controller ayrıca `X-USER-ID` header parametresi istiyor (değeri servis içinde tekrar çözülüyor).

Frontend aksiyonu:

- alarm create çağrısında body'ye `userId` koy; header'ı gateway zaten enjekte eder.

### H) History

- `GET /api/history/transactions` (desc by createdAt)
- `GET /api/history/alarms` (desc by triggeredAt)

## 12.6 Event ve veri akışı

Fiyat akışı:

1. `market-data-service` -> Kafka `market-price-updated`
2. `finance-api` consumer event'i alır
3. `InstrumentPrice` kaydı yazar + cache günceller (`PriceService.savePrice`)
4. alarm değerlendirme/portfolio/trade akışları bu fiyatı kullanır

Trade/alarm event akışı:

1. trade veya alarm olayı oluşur
2. doğrudan Kafka'ya değil outbox tablosuna yazılır
3. `OutboxPublisherScheduler` batch halinde publish eder
4. başarısız publish retry/dead mantığına düşer

## 12.7 Frontend için implementasyon checklist (finance-api)

1. Uygulama açılışında kullanıcı bootstrap senaryosunu ele al (`POST /api/users` idempotent akış).
2. Tüm finance çağrılarını gateway base URL üzerinden yap.
3. Trade ekranında `Price not available` ve `Insufficient ...` mesajlarını kullanıcı dostu çevir.
4. Alarm create payload'ına `userId` eklemeyi unutma.
5. Portfolio/external valuation ekranlarında fiyat yok hatasını ayrı state olarak göster.
6. Chart candles endpointinin demo karakterini (OHLC aynı) chart beklentisine göre normalize et.

## 13) Analytics Service Servis Dökümü (Detaylı)

Bu bölüm `analytics-service` için frontend'in ihtiyaç duyduğu endpoint kontratlarını ve veri üretim mantığını kapsar.

## 13.1 Servisin amacı

`analytics-service`, market fiyat eventlerinden analitik veri üretir ve sorgulatır:

- daily + multi-interval candle üretimi
- moving average (MA7/MA30/MA90)
- RSI(14)
- trend metrikleri (direction, momentum, slope)

Servis sorgu odaklıdır; veriyi kendi DB tablolarında tutar ve frontend'e hazır formatta döner.

## 13.2 Gateway üzerinden erişim

Gateway dış path:

- `/api/analytics/**`

Analytics'in finance-api bağı:

- consumer tarafı eventte gelen `instrumentSymbol` için `finance-api /api/instruments` çağırır
- symbol -> instrumentId çözümlemesi yapar
- çözümleme başarısızsa event işlenmez (`Instrument not found for symbol: ...`)

## 13.3 Analytics endpointleri (frontend kontratı)

- `GET /api/analytics/instruments/{symbol}/candles`
  - query: `interval?`, `from?`, `to?`
- `GET /api/analytics/instruments/{symbol}/moving-average`
- `GET /api/analytics/instruments/{symbol}/rsi`
- `GET /api/analytics/instruments/{symbol}/trend`

Response zarfı:

- `{ "success": true, "data": [...] }`

Not:

- analytics-service içinde custom global exception handler yok; beklenmeyen hatalarda default Spring hata formatı dönebilir.

## 13.4 Candles endpoint davranışı

`interval` parametresi yoksa:

- daily candle tablosundan (`analytics_price_candle_daily`) döner
- `candleDate` dolu olur, `interval/openTime/closeTime` boş olabilir

`interval` parametresi varsa:

- multi-interval candle tablosundan (`price_candles`) döner
- `interval`, `openTime`, `closeTime` dolu olur

Desteklenen interval enum:

- `ONE_MINUTE`
- `FIVE_MINUTES`
- `ONE_HOUR`
- `ONE_DAY`

Tarih filtre davranışı:

- daily modda `from/to` `LocalDate` aralığıdır
- interval modda `from/to` UTC bazlı `Instant` aralığına çevrilir

Frontend notu:

- tek endpoint iki farklı zaman modeli döndürür (`LocalDate` ve `Instant`), chart adapter katmanı gerekir.

## 13.5 Moving Average davranışı

Üretilen alanlar:

- `ma7`
- `ma30`
- `ma90`

Önemli:

- yeterli geçmiş yoksa ilgili MA alanı `null` döner (özellikle ilk dönemlerde)
- sorgu metodu `findByInstrumentSymbol` order garantisi vermiyor

Frontend aksiyonu:

- tarih alanına göre client-side sort yap
- `null` MA değerlerini grafikte gap olarak işle

## 13.6 RSI davranışı

RSI(14) hesaplama koşulları:

- en az 15 günlük candle yoksa kayıt üretilmez
- `avgLoss == 0` ise kayıt yazmadan döner

Sonuç:

- RSI serisi başlangıçta boş olabilir
- bazı günlerde veri atlaması normaldir

## 13.7 Trend metric davranışı

TrendDirection kuralı:

- `ma7 > ma30` -> `BULLISH`
- `ma7 < ma30` -> `BEARISH`
- veri eksik/eşit -> `NEUTRAL`

Ek metrikler:

- `momentum`: `currentClose - close(7 gün önce)`
- `priceSlope`: `(currentClose - close(30 gün önce)) / 30`

Veri yetersizse:

- `momentum` ve/veya `priceSlope` `null` olabilir

## 13.8 Veri üretim pipeline (gerçek zaman)

1. Kafka topic `market.price.updated` event'i gelir
2. Idempotency kontrolü yapılır (`eventId`)
3. finance-api'den instrumentId çözülür
4. Hesaplama zinciri çalışır:
   - candle aggregation (daily + 1m/5m/1h/1d)
   - moving average
   - RSI
   - trend metric
5. event processed olarak işaretlenir

Kafka hata yönetimi:

- retry backoff: 2 sn, 3 tekrar
- sonra `{topic}.dlq`'ya taşınır

## 13.9 Frontend için kritik pratik notlar

1. Symbol bazlı sorguda canonical sembol formatını koru (örn. `BTCUSDT`).
2. Candles endpointinde `interval` varsa `openTime`, yoksa `candleDate` üzerinden çiz.
3. MA/RSI/Trend serilerini tek grafikte birleştirirken tarih bazlı normalize/join yap.
4. `null` metrikler için tooltip ve legend tarafında fallback metin kullan.
5. Boş data (`[]`) durumunu "hesaplama için veri birikiyor" state'i olarak göster.
6. TrendDirection renk standardı:
   - `BULLISH` yesil
   - `BEARISH` kirmizi
   - `NEUTRAL` gri

## 13.10 Finance + Analytics entegrasyon riski

Analytics consumer, instrument eşlemesi için finance-api çağrısına bağımlı.

Riskler:

- finance-api geçici erişilemezse event işlenemeyebilir
- yeni enstrümanlar ilk eventlerde çözülmeyebilir

Frontend etkisi:

- analytics verileri gecikmeli dolabilir; UI'da "hesaplama gecikiyor" mesajı göstermek faydalıdır.

## 14) Market Data Service Servis Dökümü (Detaylı)

Bu bölüm `market-data-service` davranışını, gateway entegrasyonunu ve frontend açısından gerçek kullanım sınırlarını anlatır.

## 14.1 Servisin amacı

`market-data-service` bir "backend market feed producer" servisidir:

- dış provider'lardan fiyat çeker (Binance, CoinGecko)
- takip edilen semboller için periyodik scheduler çalıştırır
- üretilen fiyatı Kafka topic'ine event olarak yayınlar
- provider başarım/sağlık metrikleri tutar

Özet:

- frontend'e doğrudan fiyat API'si sunan servis değil
- diğer servisleri (özellikle `finance-api`, `analytics-service`) besleyen upstream kaynaktır

## 14.2 Dış endpoint envanteri

Kodda görünen tek REST endpoint:

- `GET /api/admin/providers/{provider}`

Bu endpoint, provider sağlık metriklerini döner (`success`, `failure`, `lastFailure`).

Kritik:

- bilinmeyen provider için `null` dönebilir (hata fırlatmıyor)

## 14.3 Gateway entegrasyonu ve route uyumsuzluğu

Gateway route'u:

- `/market/**` -> rewrite -> `/api/market/**`

Market service endpoint'i:

- `/api/admin/providers/{provider}`

Sonuç:

- `/market/admin/providers/{provider}` çağrısı gateway'den `/api/market/admin/providers/{provider}` olur
- market service tarafında böyle bir path yok -> yüksek olasılıkla `404`

Frontend etkisi:

- mevcut haliyle gateway üzerinden market-data REST endpointi pratikte kullanılamaz
- frontend'in market-data'dan veri alması zaten bu mimaride doğrudan beklenmemeli

## 14.4 Fiyat üretim akışı

Scheduler:

- `MarketScheduler` periyodik çalışır
- `market.tracked-symbols` listesindeki her sembol için fiyat çeker
- her sembol için `MarketPriceUpdatedEvent` üretip Kafka'ya gönderir

Event topic:

- `market.price.updated`

Event payload alanları:

- `eventId`
- `instrumentSymbol`
- `price`
- `priceType` (`MARKET`)
- `source`
- `occurredAt`

## 14.5 Provider mimarisi

Providerlar:

- `BINANCE`
- `COINGECKO`
- `COMPOSITE` (primary)

`COMPOSITE` davranışı:

- providerları sırayla dener
- ilk başarılı provider fiyatını döner
- başarısız denemelerde health+metrics artırır
- tüm providerlar fail olursa `All providers failed for symbol=...`

## 14.6 Resilience davranışı

Kullanılan mekanizma:

- Resilience4j `Retry`
- Resilience4j `CircuitBreaker`
- timeout koruması (executor + future timeout)

Config kaynakları:

- `resilience.providers.binance.*`
- `resilience.providers.coingecko.*`

Defaultlar (repo ayarı):

- binance: attempts 3, timeout 3000ms
- coingecko: attempts 2, timeout 4000ms

## 14.7 Configuration notları (önemli)

Takip edilen semboller (`application.yml`):

- `BTCUSDT`
- `ETHUSDT`
- `BNBUSDT`
- `SOLUSDT`
- `XRPUSDT`

Provider seçimi:

- `market.provider: composite`

WebClient timeout notu:

- `WebClientConfig` composite seçiliyken timeout config için `binance` profilini baz alıyor.

Scheduler property notu:

- kod `scheduler.market.delay-ms` okuyor
- config dosyasında `scheduler.binance.delay-ms` var
- bu nedenle custom değer uygulanmayıp default `5000ms` ile çalışma riski var

## 14.8 Observability ve sağlık metrikleri

Provider health tracker:

- success/failure sayar
- son hata zamanını tutar

Micrometer metrikleri:

- `price_provider_success_total{provider=...}`
- `price_provider_failure_total{provider=...}`
- `price_provider_latency{provider=...}`

Frontend notu:

- olası admin/ops ekranı yaparsan bu metrikler gateway değil observability stack üstünden okunmalı.

## 14.9 Frontend için pratik sonuçlar

1. Frontend market-data-service'i "query API" gibi konumlamamalı.
2. Fiyat/analitik ekranları için veri kaynağı olarak `finance-api` ve `analytics-service` kullanılmalı.
3. Gateway path uyumsuzluğu çözülmeden `/market/*` endpointlerine UI bağlama.
4. "Canlı veri gecikmesi" durumunda kullanıcıya servis zinciri kaynaklı gecikme mesajı göster.
5. Admin panelde provider health gösterilecekse backend route düzeltmesi sonrası entegre et.

## 15) Log Consumer Service Servis Dökümü (Detaylı)

Bu bölüm `log-consumer-service` için mimari rolü, veri akışı ve frontend'e dolaylı etkisini anlatır.

## 15.1 Servisin amacı

`log-consumer-service`, uygulama loglarını Kafka'dan tüketip OpenSearch'e indeksleyen observability servisidir.

Yaptığı iş:

- topic'ten JSON log event tüketimi
- event parse + temel correlation MDC zenginleştirmesi
- daily index adına yazım (`application-logs-YYYY-MM-DD`)
- başarısız eventlerde retry ve DLQ mekanizması

Özet:

- frontend business verisi üretmez
- operasyon/izleme tarafını besler

## 15.2 Veri akışı

1. Producer servisler `app.logs` topic'ine log event basar
2. `AppLogsKafkaConsumer` event'i `String` olarak alır
3. `ObjectMapper` ile `AppLogEvent` modeline parse eder
4. `OpenSearchLogIndexer` event'i OpenSearch'e indexler
5. Başarılıysa manual ack (`ack.acknowledge()`)

Topic ve group:

- topic: `APP_LOGS_TOPIC` varsayılan `app.logs`
- consumer group: `LOG_CONSUMER_GROUP_ID` varsayılan `log-consumer-service`

## 15.3 OpenSearch indexleme davranışı

Index adı çözümü:

- prefix: `OPENSEARCH_INDEX_PREFIX` varsayılan `application-logs`
- format: `{prefix}-{yyyy-MM-dd}`

Belge yazımı:

- document id manuel verilmiyor (OpenSearch auto-id)
- aynı event tekrar işlense bile duplicate doküman oluşabilir

OpenSearch bağlantısı:

- host/port property ile plain HTTP client
- auth/TLS konfigürasyonu kodda yok (iç ağ varsayımı)

## 15.4 Event şeması (beklenen log payload)

`AppLogEvent` alanları:

- `timestamp`
- `level`
- `serviceName`
- `message`
- `traceId`
- `spanId`
- `correlationId`
- `logger`
- `thread`
- `exception`

Not:

- model `@JsonIgnoreProperties(ignoreUnknown = true)`, yani extra alanlar parse'i bozmaz

## 15.5 Hata, retry ve DLQ davranışı

Kafka listener konfigürasyonu:

- `AckMode.RECORD`
- `enable.auto.commit=false`
- parse/index hatasında exception fırlatılır

Error handler:

- backoff: 2000ms
- retry: 3 deneme
- başarısız olursa `{topic}.dlq` partition'a gönderim

DLQ örneği:

- `app.logs.dlq`

Frontend etkisi:

- doğrudan UI tüketimi yok
- fakat log hataları artarsa "sistemde gözlemlenebilirlik kaybı" olur; incident/debug süresi artar

## 15.6 Metrics ve observability

Servis içi metrikler:

- `kafka_events_processed_total`
- `kafka_events_failed_total`
- `kafka_event_processing_latency`

Actuator/Prometheus:

- health/info/metrics/prometheus endpointleri açık

Not:

- bu metrikler ops dashboard (Grafana/Prometheus) için; frontend product ekranında doğrudan kullanılmaz.

## 15.7 REST endpoint durumu

Kodda business/admin REST controller yok.

Sonuç:

- `log-consumer-service` frontend tarafından API gibi çağrılacak bir servis değil
- log görüntüleme ihtiyacı varsa OpenSearch Dashboards veya ayrı bir backend query servisi gerekir

## 15.8 Frontend için pratik sonuçlar

1. Bu servise doğrudan API entegrasyonu planlama.
2. Uygulama içi "hata kayıtları" ekranı istenirse veri kaynağı olarak OpenSearch tarafı için ayrı gateway'lenmiş query API tasarla.
3. Correlation ID'yi frontend'de loglayıp incident durumunda backend loglarıyla eşleştirme akışı kur.
4. DLQ birikirse kullanıcıya dolaylı etkisi (teşhis gecikmesi) olur; SRE/ops alarmı şart.

## 16) News Service Servis Dökümü (Detaylı)

Bu bölüm `news-service` için endpoint kontratlarını, ingestion mekanizmasını ve frontend tarafındaki kullanım detaylarını kapsar.

## 16.1 Servisin amacı

`news-service`, dış RSS kaynaklarından haberleri çekip normalize ederek sorgulanabilir hale getirir.

Ana görevler:

- RSS feed'lerden periyodik ingest
- haberleri Postgres'te saklama
- kategori ve metin aramasıyla sayfalı listeleme
- haber detayını döndürme

## 16.2 Gateway üzerinden erişim

Gateway dış path:

- `/api/news/**`

Erişilen ana endpointler:

- `GET /api/news`
- `GET /api/news/{id}`
- `POST /api/news/admin/ingest`

Ek:

- `GET /health` (gateway finance route'u üzerinden de erişilebilir)

## 16.3 Endpoint kontratları

### A) Liste endpointi

- `GET /api/news?category=&q=&page=&size=`

Parametreler:

- `category` (opsiyonel enum):
  - `GENERAL_ECONOMY`, `STOCK`, `FX`, `CRYPTO`, `FUND`, `BOND`, `OTHER`
- `q` (opsiyonel): title/summary içinde case-insensitive arama
- `page` default `0`
- `size` default `20`

Davranış:

- sadece `active=true` kayıtlar döner
- sıralama: `publishedAt desc`
- response içinde Spring `Page` yapısı gelir (`content`, `number`, `size`, `totalElements`, vb.)

### B) Detay endpointi

- `GET /api/news/{id}`

Davranış:

- sadece `active=true` kayıtlar aranır
- yoksa `404 NOT_FOUND`

### C) Manual ingest endpointi

- `POST /api/news/admin/ingest`
- response: oluşturulan yeni kayıt sayısı (`int`)

Not:

- endpoint ismi admin olsa da servis içinde role kontrolü yok.
- mevcut gateway kurallarında bu path için özel admin ayrımı net değil; gerekli ise gateway tarafında explicit rule tanımlanmalı.

## 16.4 Ingestion pipeline

Kaynak:

- `news.feeds` altındaki RSS URL listesi

Çalışma biçimi:

1. Scheduler (`news.scheduler.delay-ms`, default 300000ms) ingest tetikler
2. Her feed için RSS parse edilir
3. En fazla `news.rss.max-entries-per-feed` kadar item alınır
4. `articleUrl` boşsa item atlanır
5. Aynı `articleUrl` varsa duplicate olarak atlanır
6. Yeni haber `news_articles` tablosuna yazılır

Önemli:

- feed bazlı hata tüm ingest'i durdurmaz; ilgili feed loglanıp diğerlerine devam eder
- string alanlar (`title`, `summary`, `url`, `sourceName`) DB limitine göre truncate edilir

## 16.5 Veri modeli notları

Tablo: `news_articles`

- unique: `article_url`
- index: `(category, published_at desc)`, `source_name`
- soft kullanılabilir alan: `active` (şu an delete endpoint yok)

News DTO'lar:

- listede `NewsResponse`
- detayda `NewsDetailResponse` (`createdAt`, `updatedAt` ek alanları var)

## 16.6 Hata formatı

GlobalExceptionHandler davranışı:

- `ResourceNotFoundException` -> `404`, code=`NOT_FOUND`
- `MethodArgumentNotValidException` -> `400`, code=`VALIDATION_ERROR`
- diğer hatalar -> `500`, code=`INTERNAL_ERROR`

Response zarfı:

- başarılı: `{ success: true, data: ... }`
- hata: `{ success: false, error: { code, message, timestamp } }`

## 16.7 Correlation ve gözlemlenebilirlik

`CorrelationIdFilter`:

- requestte `X-Correlation-Id` yoksa üretir
- response header'a yazar
- MDC'ye koyar (log correlation)

Frontend notu:

- response'taki `X-Correlation-Id` değerini error loglarında saklamak faydalıdır.

## 16.8 Frontend için pratik sonuçlar

1. News liste ekranını server-side pagination mantığıyla kur (`page`, `size`, `totalElements`).
2. `q` filtresinde debounce uygula; her tuşta istek atma.
3. `category` enumunu backend enumlarıyla birebir map et.
4. `publishedAt` alanını kullanıcı lokal saatine çevirip göster.
5. `POST /api/news/admin/ingest` için UI'da admin aksiyonunu feature flag/role guard ile sınırla.
6. Empty state ile error state'i ayır:
   - boş liste: "haber bulunamadı"
   - hata: API error mesajı + correlation id

## 17) Notification Service Servis Dökümü (Detaylı)

Bu bölüm `notification-service` için event tüketim akışını, e-posta davranışını ve frontend'e dolaylı etkilerini anlatır.

## 17.1 Servisin amacı

`notification-service`, Kafka eventlerini tüketip bildirim aksiyonları üretir.

Bu repoda iki ana alan var:

- alarm tetiklenince log tabanlı bildirim
- rapor tamamlandı/başarısız eventlerinde e-posta bildirimi

Özet:

- frontend'e doğrudan API sunan bir business servis değil
- asıl rolü asenkron event -> notification dönüşümü

## 17.2 Tüketilen topicler ve consumerlar

### A) Alarm eventi

- topic: `alarm-triggered`
- consumer: `AlarmTriggeredEventConsumer`
- group: `notification-service`
- işlem:
  - `alarm_triggered_total` metriğini artırır
  - `NotificationHandler` ile log bildirimi yazar

### B) Report completed eventi

- topic: `report.completed`
- consumer: `ReportCompletedEventConsumer`
- group: `notification-service-report-completed`
- işlem:
  - `ReportNotificationHandler.handleCompleted(...)`
  - e-posta subject/body üretilir
  - SMTP ile gönderim denenir

### C) Report failed eventi

- topic: `report.failed`
- consumer: `ReportFailedEventConsumer`
- group: `notification-service-report-failed`
- işlem:
  - `ReportNotificationHandler.handleFailed(...)`
  - hata e-postası gönderimi

## 17.3 Event şemaları

Alarm event alanları:

- `alarmId`
- `userId`
- `instrumentSymbol`
- `condition`
- `price`
- `triggeredAt`

Report completed alanları:

- `reportId`, `reportType`, `exportFormat`
- `fileName`, `fileKey`
- `instrumentSymbol`, `userId`, `userEmail`
- `completedAt`

Report failed alanları:

- `reportId`, `reportType`, `exportFormat`
- `instrumentSymbol`, `reason`
- `userId`, `userEmail`
- `failedAt`

## 17.4 E-posta davranışı

Email katmanı:

- `EmailService` arayüzü
- `SmtpEmailService` implementasyonu (`JavaMailSender`)
- `EmailTemplateService` ile subject/body üretimi

SMTP config:

- host: `smtp.gmail.com`
- port: `587`
- auth + STARTTLS aktif
- credential: `SMTP_USERNAME`, `SMTP_PASSWORD`

Frontend etkisi:

- kullanıcıya "rapor hazır/başarısız" e-posta gidip gitmediği frontend'de doğrudan izlenmiyor.
- gerekirse reporting tarafında ayrı notification status alanı tasarlamak gerekir.

## 17.5 Hata, retry ve DLQ

Kafka error handler:

- fixed backoff: `2000ms`
- retry: `3`
- sonra `{topic}.dlq` partition'a gönderim

Örnek DLQ topicleri:

- `alarm-triggered.dlq`
- `report.completed.dlq`
- `report.failed.dlq`

Not:

- tüketicilerde correlationId header MDC'ye taşınıyor (varsa)
- listener içinde exception olursa retry/DLQ zinciri devreye girer

## 17.6 Metrics ve observability

Servis metrikleri:

- `alarm_triggered_total`

Actuator:

- `health`, `info`, `metrics`, `prometheus` açık

Pratik:

- alarm ve report bildirim başarımı için ek custom metric yok; istenirse email success/failure metriği eklenebilir.

## 17.7 REST endpoint durumu

Kodda bu servis için business REST controller yok.

Sonuç:

- frontend, `notification-service` ile doğrudan konuşmamalı
- bildirim görünürlüğü gerekiyorsa gateway arkasında ayrı "notification query" endpointleri tasarlanmalı

## 17.8 Frontend için pratik sonuçlar

1. Alarm ve report akışını event-driven kabul et; UI'da "bildirim eventual consistency" mesajı gerekebilir.
2. "Rapor tamamlandı" bilgisini sadece e-postaya bağlama; reporting tarafında polling/status endpointi kullan.
3. Incident/debug için correlation id'yi frontend loglarında tut.
4. Notification başarısızlıklarını kullanıcıya göstermek istersen DLQ veya email send sonuçlarını expose eden yeni backend API tasarla.

## 18) Reporting Service Servis Dökümü (Detaylı)

Bu bölüm `reporting-service` için rapor üretim, sorgulama, indirme ve schedule akışlarını frontend bakışıyla açıklar.

## 18.1 Servisin amacı

`reporting-service`, kullanıcı için dosya tabanlı rapor üretir ve saklar:

- instrument report (analytics verisinden)
- portfolio report (finance portföy verisinden)
- export formatları: `CSV`, `PDF`
- MinIO üstünde dosya saklama
- async Kafka pipeline ile üretim
- schedule ile periyodik rapor üretimi

## 18.2 Gateway üzerinden erişim

Gateway dış path:

- `/api/reports/**`

Frontend'in çağırdığı uçlar:

- `POST /api/reports/instrument`
- `POST /api/reports/portfolio`
- `GET /api/reports/{reportId}`
- `GET /api/reports/{reportId}/download`
- `POST /api/reports/schedules/portfolio`
- `GET /api/reports/schedules`
- `POST /api/reports/schedules/{scheduleId}/pause`
- `POST /api/reports/schedules/{scheduleId}/activate`

## 18.3 Senkron vs asenkron davranış

Rapor oluşturma endpointleri (`instrument`, `portfolio`) senkron dosya döndürmez.

Akış:

1. metadata kaydı `QUEUED` olarak açılır
2. `report.requested` event'i Kafka'ya gönderilir
3. consumer raporu işler, MinIO'ya yükler
4. metadata `COMPLETED` veya `FAILED` olur
5. frontend `GET /api/reports/{reportId}` ile polling yapar
6. `COMPLETED` ise `download` endpointine gider

Frontend sonucu:

- "request accepted" + "processing" state tasarlamak zorunlu

## 18.4 Endpoint kontrat detayları

### A) Instrument report create

- `POST /api/reports/instrument`
- request:
  - `symbol` (zorunlu)
  - `from` (LocalDate, zorunlu)
  - `to` (LocalDate, zorunlu)
  - `exportFormat` (`CSV`/`PDF`, zorunlu)
  - `userId`, `userEmail` (opsiyonel ama eventte taşınıyor)

### B) Portfolio report create

- `POST /api/reports/portfolio`
- request:
  - `exportFormat` zorunlu
  - `userId`, `userEmail` opsiyonel

### C) Metadata sorgu

- `GET /api/reports/{reportId}`
- response alanları:
  - `reportId`
  - `status` (`QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`)
  - `fileName`
  - `reportType`
  - `exportFormat`
  - `instrumentSymbol`

### D) Download

- `GET /api/reports/{reportId}/download`
- binary response döner (`text/csv` veya `application/pdf`)
- `Content-Disposition: attachment` ile gelir

Kritik:

- dosya hazır değilse `Report content is not ready yet` hatası döner
- frontend bu endpointi yalnızca status `COMPLETED` olduktan sonra çağırmalı

### E) Schedule yönetimi

- `POST /api/reports/schedules/portfolio`
  - `exportFormat`, `frequency`, `preferredHour`, `preferredMinute` zorunlu
  - `WEEKLY` için `preferredDayOfWeek` zorunlu
  - `MONTHLY` için `preferredDayOfMonth (1..28)` zorunlu
- `GET /api/reports/schedules`
- `POST /api/reports/schedules/{scheduleId}/pause`
- `POST /api/reports/schedules/{scheduleId}/activate`

## 18.5 Rapor üretim iç akışı

Processing sırasında:

- instrument rapor:
  - analytics `candles`, `moving-average`, `trend` verilerini toplar
  - exporter ile CSV/PDF üretir
- portfolio rapor:
  - finance `GET /api/portfolio` verisini alır
  - allocation/summary hesaplayıp CSV/PDF üretir

Dosya saklama:

- MinIO bucket: `reports`
- key olarak dosya adı kullanılır

Status eventleri:

- başarılı: `report.completed`
- başarısız: `report.failed`

Bu eventleri `notification-service` tüketip e-posta gönderir.

## 18.6 Schedule runner davranışı

`ReportScheduleRunner` periyodik tetik:

- fixed delay: `reporting.scheduler.fixed-delay-ms` (default 60000)
- `ACTIVE` ve `nextRunAt <= now` schedule'ları toplar
- her biri için yeni report metadata + `report.requested` event üretir
- `nextRunAt` değerini frequency'ye göre günceller

Önemli:

- scheduled event üretiminde `userId`/`userEmail` şu an `null` gidiyor
- bu nedenle schedule kaynaklı raporlarda notification/email akışı eksik kalabilir

## 18.7 Hata formatı ve status code notu

GlobalExceptionHandler:

- `IllegalArgumentException` -> `ApiResponse.error(BAD_REQUEST)`
- `IllegalStateException` -> `ApiResponse.error(ILLEGAL_STATE)`
- generic -> `ApiResponse.error(INTERNAL_ERROR)`

Not:

- handler `ResponseEntity` kullanmıyor, dolayısıyla çoğu hata HTTP 200 ile `success=false` dönebilir.
- frontend sadece HTTP status'a güvenmemeli; `success` ve `error.code` mutlaka kontrol edilmeli.

## 18.8 Kafka retry / DLQ

Reporting consumer config:

- ack mode: `RECORD`
- concurrency: `2`
- retry: 3 (2s backoff)
- fail sonrası `{topic}.dlq`

Örnek DLQ:

- `report.requested.dlq`

## 18.9 Frontend için pratik sonuçlar

1. Rapor oluşturma sonrası hemen download deneme; metadata polling uygula.
2. `status` için state machine kur: `QUEUED -> PROCESSING -> COMPLETED/FAILED`.
3. Download çağrısında `blob` handling kullan (`responseType: 'blob'`).
4. Hata yakalamada `success=false` kontrolünü merkezi hale getir (HTTP 200 olsa bile).
5. Schedule formunda frequency'ye göre dinamik validasyon yap (weekly/monthly alanlarını koşullu göster).
6. Notification e-postasını yardımcı sinyal kabul et; asıl doğrulama kaynağı reporting metadata endpointi olsun.

