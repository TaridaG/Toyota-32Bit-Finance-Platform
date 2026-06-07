# Javadoc yazım standartları

Backend Java kodunda (`finance-api` ve diğer servisler) Javadoc için proje genelinde aşağıdaki konvansiyon uygulanır.

## Dil

- **Cümleler Türkçe** yazılır.
- **Yazılımsal terimler İngilizce** bırakılır; Türkçeye çevrilmez.

Örnek terimler: endpoint, controller, service, DTO, snapshot, ledger, transaction, consumer, producer, cache, JWT, OAuth2, mark-to-market, soft delete, portfolio, watchlist.

**İyi:**

```java
/** Portfolio özet, değerleme ve snapshot endpoint'lerini sunan controller. */
/** Transaction ledger'dan pozisyon maliyet tabanını hesaplar. */
/** Oturum açmış kullanıcının SELL transaction'larını sayfalı analytics satırları olarak döner. */
```

**Kaçınılacak:**

```java
/** Paginated realized sell analytics. */          // tamamen İngilizce
/** getMyHistory sözleşmesi. */                    // metot adını tekrar eden stub
/** TransactionHistoryService iş mantığını uygular (transaction history service). */
// ↑ yalnızca sınıf şablonu — public metotlarda davranış açıklaması tercih edilir
```

## Kapsam

| Katman | Beklenti |
|--------|----------|
| `@RestController` | Sınıf + her public endpoint metodu |
| Service interface | Sınıf + public metotlar (davranış odaklı) |
| ServiceImpl | Sınıf; karmaşık private/override metotlarda isteğe bağlı |
| Domain (karmaşık logic) | Sınıf + public metotlar |
| DTO / record | Kısa sınıf açıklaması yeterli |
| Repository | Tek satır sınıf açıklaması yeterli |
| Test | Zorunlu değil |

## Etiketler (`@param`, `@return`, `@throws`)

Zorunlu değil; karmaşık public API'lerde kullanın:

```java
/**
 * İstemci locale'i veya kayıtlı kullanıcı tercihine göre mail dili seçer.
 *
 * @param explicitLocale kayıt/profil UI'dan gelen isteğe bağlı locale
 * @param email tercih yoksa kullanıcı kaydı için aranan adres
 */
```

## Paket dokümantasyonu

Ana bounded context paketlerinde `package-info.java` bulunur (`portfolio`, `market`, `auth`, `alarm`).

## HTML üretimi

```bash
cd finance-api
./mvnw javadoc:javadoc
```

Çıktı: `finance-api/target/site/apidocs/index.html`

CI, `finance-api` değiştiğinde Javadoc HTML'ini artifact olarak yükler (build kırmaz).

## HTTP API vs Javadoc

- **Swagger / springdoc-openapi:** REST endpoint sözleşmesi, request/response şemaları.
- **Javadoc:** Java sınıf/metot iç dokümantasyonu, domain kuralları, implementasyon notları.
