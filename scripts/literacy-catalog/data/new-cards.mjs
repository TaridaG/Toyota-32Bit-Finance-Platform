/**
 * New general literacy cards (not contextual help bindings).
 * Full tr / en / de content.
 */
function card(tr, en, de, meta) {
  return { tr: { ...meta, ...tr }, en, de }
}

export const NEW_CARDS = [
  card(
    {
      title: 'Enflasyon',
      shortDescription: 'Mal ve hizmet fiyatlarının genel düzeyde sürekli artışıdır.',
      detailedDescription:
        'Enflasyon alım gücünü eritir; nominal getirileri reel getiriye çevirmek, yatırım karşılaştırmalarında zorunlu hale getirir.',
      howToInterpret:
        'Yıllık TÜFE oranı tek başına yeterli değildir; trend, bileşenler (gıda, enerji) ve politika faiziyle birlikte okunmalıdır.',
      commonMistake: '“Fiyatlar biraz arttı” ile yapısal enflasyonu aynı kefeye koymak.',
      relatedTerms: ['TÜFE Endeksi', 'Reel getiri', 'Alım gücü'],
      targetTerms: ['Enflasyon', 'enflasyon', 'TÜFE'],
    },
    {
      title: 'Inflation',
      shortDescription: 'A sustained rise in the general level of prices for goods and services.',
      detailedDescription:
        'Inflation erodes purchasing power; comparing investments requires real (inflation-adjusted) returns, not nominal figures alone.',
      howToInterpret:
        'Read annual CPI together with trend, components (food, energy), and policy rates—not as a single headline.',
      commonMistake: 'Treating a one-off price spike the same as persistent inflation.',
      relatedTerms: ['CPI index', 'Real return', 'Purchasing power'],
    },
    {
      title: 'Inflation',
      shortDescription: 'Ein anhaltender Anstieg des allgemeinen Preisniveaus für Güter und Dienstleistungen.',
      detailedDescription:
        'Inflation mindert die Kaufkraft; für Vergleiche braucht man reale (inflationsbereinigte) Renditen, nicht nur nominale.',
      howToInterpret:
        'Jährliche Inflation immer im Kontext von Trend, Komponenten und Leitzins lesen.',
      commonMistake: 'Einmalige Preissprünge mit dauerhafter Inflation gleichsetzen.',
      relatedTerms: ['VPI-Index', 'Reale Rendite', 'Kaufkraft'],
    },
    { slug: 'enflasyon', category: 'BASIC_FINANCE', type: 'TERM', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'Piyasa değeri',
      shortDescription: 'Bir şirketin veya varlığın piyasadaki toplam fiyatlandırılmış değeridir.',
      detailedDescription:
        'Hisse için genelde fiyat × dolaşımdaki pay; fon ve emtia için farklı tanımlar kullanılır. Büyüklük, likidite ve risk algısını yansıtır.',
      howToInterpret:
        'Piyasa değeri büyüklüğü tek başına “kaliteli yatırım” demek değildir; büyüme, kârlılık ve borç yapısıyla birlikte okunmalıdır.',
      relatedTerms: ['Hisse senedi', 'Likidite', 'Getiri'],
      targetTerms: ['Piyasa değeri', 'market cap'],
    },
    {
      title: 'Market capitalization',
      shortDescription: 'The total market price of a company or asset.',
      detailedDescription:
        'For equities, typically share price times shares outstanding. Size affects liquidity, index weight, and risk perception.',
      howToInterpret: 'Large cap does not automatically mean lower risk; combine with fundamentals and volatility.',
      relatedTerms: ['Stock', 'Liquidity', 'Return'],
    },
    {
      title: 'Marktkapitalisierung',
      shortDescription: 'Der gesamte Marktwert eines Unternehmens oder Vermögenswerts.',
      detailedDescription:
        'Bei Aktien meist Kurs mal ausstehende Aktien. Größe beeinflusst Liquidität und Risikowahrnehmung.',
      howToInterpret: 'Große Kapitalisierung allein bedeutet nicht automatisch geringeres Risiko.',
      relatedTerms: ['Aktie', 'Liquidität', 'Rendite'],
    },
    { slug: 'piyasa-degeri', category: 'MARKET_DATA', type: 'TERM', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'Dönem getirisi',
      shortDescription: 'Seçilen takvim aralığında (1 gün, 1 ay, 1 yıl) yüzde fiyat veya değer değişimidir.',
      detailedDescription:
        'Portal tablolarındaki 1G, 1A, 3A, 6A ve 1Y kolonları aynı mantığın farklı pencereleridir; karşılaştırma için dönem uzunluğu eşit olmalıdır.',
      howToInterpret:
        'Kısa vadeli güçlü getiri, uzun vadede sürdürülebilir olmayabilir. Farklı varlık sınıflarını aynı dönemle kıyaslayın.',
      commonMistake: '1 günlük performansı ile 1 yıllık hedefi aynı metrik gibi değerlendirmek.',
      relatedTerms: ['Getiri', 'Volatilite', 'Trend skoru'],
      targetTerms: ['1G', '1A', '1Y', 'dönem getirisi'],
    },
    {
      title: 'Period return',
      shortDescription: 'Percentage change in price or value over a chosen calendar window (1D, 1M, 1Y).',
      detailedDescription:
        'Table columns such as 1D, 1M, 3M, 6M, and 1Y are the same idea with different horizons; compare like with like.',
      howToInterpret: 'Strong short-term returns may not persist; align the period with your investment horizon.',
      commonMistake: 'Judging a one-day move with a one-year objective in mind.',
      relatedTerms: ['Return', 'Volatility', 'Trend score'],
    },
    {
      title: 'Periodenrendite',
      shortDescription: 'Prozentuale Wertänderung über ein gewähltes Zeitfenster (1T, 1M, 1J).',
      detailedDescription:
        'Spalten wie 1T, 1M, 3M, 6M und 1J sind dieselbe Logik mit unterschiedlicher Länge.',
      howToInterpret: 'Kurzfristige Spitzen sind nicht automatisch langfristig haltbar.',
      commonMistake: 'Eintägige Bewegung mit einem Jahresziel verwechseln.',
      relatedTerms: ['Rendite', 'Volatilität', 'Trend-Score'],
    },
    { slug: 'donem-getirisi', category: 'MARKET_DATA', type: 'TERM', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'Trend skoru',
      shortDescription: 'Kısa vadeli fiyat momentumunu segment içindeki diğer enstrümanlarla kıyaslayan özet skordur.',
      detailedDescription:
        'Piyasalar tablosundaki mini grafik ve WEAK–STRONG etiketleri, göreli performansı hızlı okumak için tasarlanmıştır; mutlak getiri değildir.',
      howToInterpret:
        'Yüksek skor “güçlü göreli hareket” demektir; düşük skor zayıf momentum. Mutlaka 1G ve 1A getirisiyle birlikte bakın.',
      relatedTerms: ['Volatilite', 'Dönem getirisi', 'Isı haritası'],
      targetTerms: ['Trend', 'trend skoru', 'momentum'],
    },
    {
      title: 'Trend score',
      shortDescription: 'A summary score comparing short-term price momentum to peers in the same segment.',
      detailedDescription:
        'Sparklines and WEAK–STRONG labels in the Markets table show relative strength, not absolute return.',
      howToInterpret: 'High score means strong relative momentum; always cross-check 1D and 1M returns.',
      relatedTerms: ['Volatility', 'Period return', 'Heat map'],
    },
    {
      title: 'Trend-Score',
      shortDescription: 'Ein Kennzahlvergleich kurzfristiger Momentum relativ zu anderen Instrumenten im Segment.',
      detailedDescription:
        'Sparklines und WEAK–STRONG Kennzeichnen relative Stärke, nicht absolute Rendite.',
      howToInterpret: 'Hoher Score = starkes relatives Momentum; mit 1T- und 1M-Rendite abgleichen.',
      relatedTerms: ['Volatilität', 'Periodenrendite', 'Heatmap'],
    },
    { slug: 'trend-skoru', category: 'MARKET_DATA', type: 'TERM', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'Emtia vadeli işlem',
      shortDescription: 'Altın, gümüş, bakır gibi emtiaların ileri tarihli kontratlarla işlem gördüğü piyasadır.',
      detailedDescription:
        'Yurtdışı vadeli segmentinde GC=F (altın), SI=F (gümüş) gibi semboller küresel referans fiyatları izler; TRY gösterimi kur ile birlikte hesaplanır.',
      howToInterpret:
        'Vadeli fiyat spot fiyattan farklı olabilir (carry/roll). Küresel risk ve dolar endeksi emtia fiyatlarını etkiler.',
      relatedTerms: ['Ons altın', 'Gram altın', 'VİOP', 'Volatilite'],
      targetTerms: ['Vadeli', 'GC=F', 'emtia', 'global futures'],
    },
    {
      title: 'Commodity futures',
      shortDescription: 'Contracts to buy or sell commodities (gold, silver, copper) at a future date.',
      detailedDescription:
        'Symbols like GC=F and SI=F track global benchmarks; local TRY quotes also reflect FX moves.',
      howToInterpret: 'Futures can diverge from spot; global risk appetite and the USD matter for commodities.',
      relatedTerms: ['Gold ounce', 'Gram gold', 'Volatility'],
    },
    {
      title: 'Rohstoff-Futures',
      shortDescription: 'Kontrakte auf künftige Lieferung bzw. Abrechnung von Rohstoffen wie Gold, Silber, Kupfer.',
      detailedDescription:
        'Symbole wie GC=F und SI=F folgen globalen Referenzpreisen; TRY-Anzeigen spiegeln auch Wechselkurse.',
      howToInterpret: 'Future-Preise können vom Spot abweichen; USD und Risikostimmung sind wichtig.',
      relatedTerms: ['Goldunze', 'Gramm Gold', 'Volatilität'],
    },
    { slug: 'emtia-vadeli-islem', category: 'MARKET_DATA', type: 'ASSET', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'NASDAQ',
      shortDescription: 'ABD’de teknoloji ve büyüme hisselerinin yoğunlaştığı borsa endeks ve piyasasıdır.',
      detailedDescription:
        'Portalda NASDAQ segmenti, Finnhub kaynaklı uluslararası hisse kotasyonlarını listeler; BIST’ten farklı işlem saatleri ve para birimi vardır.',
      howToInterpret:
        'Teknoloji ağırlığı nedeniyle volatilite BIST’e göre farklı olabilir; kur riski TRY yatırımcısı için ayrı boyuttur.',
      relatedTerms: ['Hisse senedi', 'Volatilite', 'Piyasa değeri'],
      targetTerms: ['NASDAQ', 'nasdaq', 'ABD hisse'],
    },
    {
      title: 'NASDAQ',
      shortDescription: 'A U.S. market and index cluster known for technology and growth stocks.',
      detailedDescription:
        'In the portal, the NASDAQ segment lists international quotes; trading hours and currency differ from BIST.',
      howToInterpret: 'Higher tech weight often means different volatility; FX matters for TRY-based investors.',
      relatedTerms: ['Stock', 'Volatility', 'Market cap'],
    },
    {
      title: 'NASDAQ',
      shortDescription: 'US-Börse und Indexfamilie mit Schwerpunkt Technologie und Wachstumswerten.',
      detailedDescription:
        'Im Portal listet das NASDAQ-Segment internationale Kurse; Handelszeiten und Währung unterscheiden sich von BIST.',
      howToInterpret: 'Tech-Gewichtung bringt oft andere Volatilität; FX-Risiko für TRY-Anleger beachten.',
      relatedTerms: ['Aktie', 'Volatilität', 'Marktkapitalisierung'],
    },
    { slug: 'nasdaq', category: 'MARKET_DATA', type: 'TERM', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'TEFAS',
      shortDescription: 'Türkiye’de yatırım fonlarının fiyat, performans ve bilgilerinin merkezi takip platformudur.',
      detailedDescription:
        'Fon kodları ve günlük NAV verileri fon karşılaştırmalarının temelidir; yönetim ücreti ve risk profili fon izahnamesinde yer alır.',
      howToInterpret:
        'Aynı kategorideki fonları NAV değişimi ve volatilite ile kıyaslayın; geçmiş performans geleceği garanti etmez.',
      relatedTerms: ['Fon', 'Getiri', 'Risk'],
      targetTerms: ['TEFAS', 'fon', 'NAV'],
    },
    {
      title: 'TEFAS',
      shortDescription: 'Turkey’s central platform for mutual fund prices, performance, and disclosures.',
      detailedDescription:
        'Fund codes and daily NAV are core to comparisons; fees and risk profile are in the fund prospectus.',
      howToInterpret: 'Compare peers by NAV change and volatility; past performance is not a guarantee.',
      relatedTerms: ['Fund', 'Return', 'Risk'],
    },
    {
      title: 'TEFAS',
      shortDescription: 'Zentrale türkische Plattform für Fondspreise, Performance und Fondsdaten.',
      detailedDescription:
        'Fondscodes und täglicher NAV sind Grundlage für Vergleiche; Gebühren stehen im Prospekt.',
      howToInterpret: 'Peer-Fonds über NAV-Entwicklung und Volatilität vergleichen.',
      relatedTerms: ['Fonds', 'Rendite', 'Risiko'],
    },
    { slug: 'tefas', category: 'MARKET_DATA', type: 'TERM', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'Tahvil getirisi',
      shortDescription: 'Tahvil yatırımcısının elde ettiği getiri; fiyat hareketi ve kupon ödemelerinden oluşur.',
      detailedDescription:
        'Faizler yükseldiğinde tahvil fiyatı düşer, getiri yükselir (ters ilişki). Eurobond ve devlet tahvillerinde kur riski ayrı boyuttur.',
      howToInterpret:
        'Getiriyi yıllıklandırılmış (yield) gösterimle okuyun; fiyat dalgalanması kısa vadede P&L’i etkiler.',
      relatedTerms: ['Tahvil', 'Kupon', 'Faiz', 'Eurobond'],
      targetTerms: ['Tahvil getirisi', 'yield'],
    },
    {
      title: 'Bond yield',
      shortDescription: 'Return to a bond investor from price moves and coupon payments.',
      detailedDescription:
        'When rates rise, bond prices usually fall and yields rise (inverse link). FX adds a layer for USD bonds.',
      howToInterpret: 'Focus on annualized yield; short-term price swings affect mark-to-market P&L.',
      relatedTerms: ['Bond', 'Coupon', 'Interest rate'],
    },
    {
      title: 'Anleiherendite',
      shortDescription: 'Rendite für Anleiheinvestoren aus Kursbewegung und Kuponzahlungen.',
      detailedDescription:
        'Steigende Zinsen drücken meist Kurse und erhöhen Renditen. FX-Risiko bei USD-Anleihen.',
      howToInterpret: 'Annualisierte Rendite betrachten; kurzfristige Kursschwankungen beeinflussen P&L.',
      relatedTerms: ['Anleihe', 'Kupon', 'Zins'],
    },
    { slug: 'tahvil-getirisi', category: 'MARKET_DATA', type: 'TERM', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'Kupon',
      shortDescription: 'Tahvil veya eurobond sahibine periyodik olarak ödenen faiz tutarıdır.',
      detailedDescription:
        'Kupon oranı ve ödeme takvimi, sabit getirili enstrümanların nakit akışını belirler; fiyat ise piyasa faizleriyle değişir.',
      howToInterpret:
        'Yüksek kupon nakit geliri sağlar; ancak fiyat düşüşü portföy değerini yine de azaltabilir.',
      relatedTerms: ['Tahvil', 'Tahvil getirisi', 'Faiz'],
      targetTerms: ['Kupon', 'coupon'],
    },
    {
      title: 'Coupon',
      shortDescription: 'Periodic interest paid to the holder of a bond or eurobond.',
      detailedDescription:
        'Coupon rate and schedule define cash flows; market price still moves with interest rates.',
      howToInterpret: 'Coupons provide income; price losses can still reduce portfolio value.',
      relatedTerms: ['Bond', 'Bond yield', 'Interest rate'],
    },
    {
      title: 'Kupon',
      shortDescription: 'Periodische Zinszahlung an Inhaber von Anleihen oder Eurobonds.',
      detailedDescription:
        'Kupon und Zahlungsplan definieren Cashflows; der Marktpreis folgt den Zinsen.',
      howToInterpret: 'Kupon bringt Einnahmen; Kursverluste können dennoch den Wert drücken.',
      relatedTerms: ['Anleihe', 'Anleiherendite', 'Zins'],
    },
    { slug: 'kupon', category: 'BASIC_FINANCE', type: 'TERM', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'Vade',
      shortDescription: 'Borçlanma aracının anaparasının geri ödeneceği tarih veya süredir.',
      detailedDescription:
        'Kısa vade (bono) ile uzun vade (tahvil) farklı faiz ve fiyat hassasiyeti taşır; vade uzadıkça faiz riski genelde artar.',
      howToInterpret:
        'Vade yaklaştıkça fiyat genelde paraya (itfa değerine) yaklaşır; erken satışta piyasa fiyatı geçerlidir.',
      relatedTerms: ['Hazine bonosu', 'Tahvil', 'Tahvil getirisi'],
      targetTerms: ['Vade', 'maturity'],
    },
    {
      title: 'Maturity',
      shortDescription: 'The date or horizon when principal of a debt instrument is repaid.',
      detailedDescription:
        'Short maturity (bill) vs long (bond) implies different rate sensitivity; longer duration usually means more rate risk.',
      howToInterpret: 'Near maturity, price tends toward redemption value; early sale uses market price.',
      relatedTerms: ['Treasury bill', 'Bond', 'Bond yield'],
    },
    {
      title: 'Laufzeit',
      shortDescription: 'Zeitpunkt bzw. Horizont der Rückzahlung einer Schuldverschreibung.',
      detailedDescription:
        'Kurz vs. lang bedeutet unterschiedliche Zinssensitivität; längere Laufzeit oft mehr Zinsrisiko.',
      howToInterpret: 'Gegen Laufzeitende nähert sich der Kurs oft dem Rückzahlungswert.',
      relatedTerms: ['Schatzanweisung', 'Anleihe', 'Anleiherendite'],
    },
    { slug: 'vade', category: 'BASIC_FINANCE', type: 'TERM', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'Hazine bonosu',
      shortDescription: 'Devletin kısa vadeli borçlanma aracıdır; genelde bir yıldan kısa vadelerde ihraç edilir.',
      detailedDescription:
        'Düşük kredi riski algısı nedeniyle referans getiri eğrisinin kısa ucunu oluşturur; faiz ve likidite koşullarıyla fiyatlanır.',
      howToInterpret:
        'Bono getirisi, politika faizi ve likidite beklentileriyle hareket eder; tahvil ile vade yapısını karıştırmayın.',
      relatedTerms: ['Tahvil', 'Politika faizi', 'Tahvil getirisi'],
      targetTerms: ['Hazine bonosu', 'bono'],
    },
    {
      title: 'Treasury bill',
      shortDescription: 'Short-term government debt, typically under one year.',
      detailedDescription:
        'Often viewed as low credit risk and anchors the short end of the yield curve.',
      howToInterpret: 'Bill yields track policy rate and liquidity expectations; not the same as long bonds.',
      relatedTerms: ['Bond', 'Policy rate', 'Bond yield'],
    },
    {
      title: 'Schatzanweisung',
      shortDescription: 'Kurzfristige Staatsverschuldung, typischerweise unter einem Jahr.',
      detailedDescription:
        'Gilt oft als geringes Kreditrisiko und prägt das kurze Ende der Zinsstruktur.',
      howToInterpret: 'Renditen folgen Leitzins und Liquidität; nicht mit langen Anleihen verwechseln.',
      relatedTerms: ['Anleihe', 'Leitzins', 'Anleiherendite'],
    },
    { slug: 'hazine-bonosu', category: 'MARKET_DATA', type: 'ASSET', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'RSI (14)',
      shortDescription: 'Göreceli güç endeksi; fiyat momentumunun aşırı alım/satım bölgelerini özetler.',
      detailedDescription:
        'Analiz grafiğinde 14 periyot RSI, kapanış fiyatlarından türetilir. 0–100 skalasında; 70 üzeri aşırı alım, 30 altı aşırı satım bölgesi olarak yorumlanır (tek başına sinyal değildir).',
      howToInterpret:
        'Trend güçlüyken RSI uzun süre aşırı alımda kalabilir; tek göstergeyle işlem kararı vermeyin, fiyat yapısıyla teyit edin.',
      commonMistake: 'RSI 70’i otomatik “sat” sinyali sanmak.',
      relatedTerms: ['Teknik gösterge', 'Mum grafik', 'Volatilite'],
      targetTerms: ['RSI', 'teknik analiz'],
    },
    {
      title: 'RSI (14)',
      shortDescription: 'Relative Strength Index summarizing momentum and overbought/oversold zones.',
      detailedDescription:
        '14-period RSI on the analysis chart is derived from closes. Above 70 often labeled overbought, below 30 oversold—not standalone signals.',
      howToInterpret: 'In strong trends RSI can stay overbought; confirm with price structure, not RSI alone.',
      commonMistake: 'Treating RSI 70 as an automatic sell signal.',
      relatedTerms: ['Technical indicator', 'Candlestick chart', 'Volatility'],
    },
    {
      title: 'RSI (14)',
      shortDescription: 'Relative-Strength-Index: fasst Momentum und überkauft/überverkauft zusammen.',
      detailedDescription:
        '14-Perioden-RSI auf dem Analysechart basiert auf Schlusskursen. Über 70/ unter 30 sind Zonen, keine alleinigen Signale.',
      howToInterpret: 'In starken Trends kann RSI lange überkauft bleiben; mit Kursstruktur bestätigen.',
      commonMistake: 'RSI 70 automatisch als Verkaufssignal interpretieren.',
      relatedTerms: ['Technischer Indikator', 'Kerzenchart', 'Volatilität'],
    },
    { slug: 'rsi-14', category: 'CHARTS', type: 'ANALYSIS_TOOL', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'Teknik gösterge',
      shortDescription: 'Fiyat ve hacim verisinden türetilen, grafik üzerinde okunan analiz araçlarıdır.',
      detailedDescription:
        'RSI, hareketli ortalama ve benzeri göstergeler geçmiş veriye dayanır; geleceği garanti etmez, karar destek sağlar.',
      howToInterpret:
        'Birden fazla gösterge ve zaman dilimi birlikte kullanılmalı; tek göstergeye aşırı güvenmeyin.',
      relatedTerms: ['RSI (14)', 'Mum grafik', 'Destek ve direnç'],
      targetTerms: ['Teknik gösterge', 'indicator'],
    },
    {
      title: 'Technical indicator',
      shortDescription: 'Analytics derived from price and volume, plotted on charts.',
      detailedDescription:
        'Tools like RSI and moving averages use past data; they support decisions but do not guarantee outcomes.',
      howToInterpret: 'Combine multiple indicators and time frames; avoid single-indicator trading.',
      relatedTerms: ['RSI (14)', 'Candlestick chart', 'Support and resistance'],
    },
    {
      title: 'Technischer Indikator',
      shortDescription: 'Aus Preis und Volumen abgeleitete Analysewerkzeuge im Chart.',
      detailedDescription:
        'RSI und gleitende Durchschnitte nutzen Vergangenheitsdaten; sie unterstützen, garantieren nichts.',
      howToInterpret: 'Mehrere Indikatoren und Zeitebenen kombinieren.',
      relatedTerms: ['RSI (14)', 'Kerzenchart', 'Unterstützung und Widerstand'],
    },
    { slug: 'teknik-gosterge', category: 'CHARTS', type: 'ANALYSIS_TOOL', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'İşlem hacmi',
      shortDescription: 'Belirli bir dönemde el değiştiren toplam miktar veya tutardır.',
      detailedDescription:
        'Yüksek hacim fiyat hareketinin güvenilirliğini artırabilir; düşük hacimde fiyat sıçramaları yanıltıcı olabilir.',
      howToInterpret:
        'Kırılım (breakout) anlarında hacim artışı teyit aranır; hacimsiz hareketlere temkinli yaklaşın.',
      relatedTerms: ['Likidite', 'Mum grafik', 'Volatilite'],
      targetTerms: ['Hacim', 'volume'],
    },
    {
      title: 'Trading volume',
      shortDescription: 'Total quantity or value traded over a period.',
      detailedDescription:
        'High volume can validate price moves; low volume spikes may be noisy or unreliable.',
      howToInterpret: 'Look for volume confirmation on breakouts; be cautious on thin moves.',
      relatedTerms: ['Liquidity', 'Candlestick chart', 'Volatility'],
    },
    {
      title: 'Handelsvolumen',
      shortDescription: 'Gesamtmenge oder -wert gehandelter Kontrakte/Aktien in einer Periode.',
      detailedDescription:
        'Hohes Volumen kann Kursbewegungen stützen; dünne Märkte sind volatiler.',
      howToInterpret: 'Bei Ausbrüchen Volumenanstieg als Bestätigung nutzen.',
      relatedTerms: ['Liquidität', 'Kerzenchart', 'Volatilität'],
    },
    { slug: 'islem-hacmi', category: 'MARKET_DATA', type: 'TERM', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'Destek ve direnç',
      shortDescription: 'Fiyatın historically tepki verdiği alt (destek) ve üst (direnç) bölgelerdir.',
      detailedDescription:
        'Analiz çizim araçlarıyla işaretlenir; psikoloji ve likidite kümelenmeleriyle ilişkilidir, kesin kural değildir.',
      howToInterpret:
        'Kırılım sonrası eski direnç destek olabilir; hacim ve kapanış teyidi önemlidir.',
      relatedTerms: ['Referans çizgisi', 'Mum grafik', 'Teknik gösterge'],
      targetTerms: ['Destek', 'direnç', 'support'],
    },
    {
      title: 'Support and resistance',
      shortDescription: 'Price zones where the market has historically reacted to the downside (support) or upside (resistance).',
      detailedDescription:
        'Drawn with chart tools; reflects clustering of orders and sentiment, not a physical law.',
      howToInterpret: 'After a breakout, old resistance may act as support; confirm with closes and volume.',
      relatedTerms: ['Reference line', 'Candlestick chart', 'Technical indicator'],
    },
    {
      title: 'Unterstützung und Widerstand',
      shortDescription: 'Kurszonen mit historisch häufiger Reaktion nach unten (Unterstützung) oder oben (Widerstand).',
      detailedDescription:
        'Mit Charttools markiert; spiegelt Ordercluster und Sentiment, keine feste Regel.',
      howToInterpret: 'Nach Ausbruch kann alter Widerstand zur Unterstützung werden.',
      relatedTerms: ['Referenzlinie', 'Kerzenchart', 'Technischer Indikator'],
    },
    { slug: 'destek-ve-direnç', category: 'CHARTS', type: 'TERM', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'ÜFE',
      shortDescription: 'Üretici fiyat endeksi; üretim ve toptan aşamadaki fiyat değişimini ölçer.',
      detailedDescription:
        'Yİ-ÜFE ile ilişkilidir; üretici maliyet baskısı tüketici fiyatlarına gecikmeli yansıyabilir.',
      howToInterpret:
        'ÜFE-TÜFE makası arz zinciri baskısı hakkında ipucu verir; tek veri noktasına dayanmayın.',
      relatedTerms: ['Yİ-ÜFE', 'TÜFE Endeksi', 'Enflasyon'],
      targetTerms: ['ÜFE', 'PPI'],
    },
    {
      title: 'PPI',
      shortDescription: 'Producer Price Index measuring price changes at production/wholesale stage.',
      detailedDescription:
        'Related to WPI in Turkey; producer costs may pass through to consumer prices with a lag.',
      howToInterpret: 'PPI–CPI gap hints at pipeline pressure; read trends, not one print.',
      relatedTerms: ['WPI', 'CPI index', 'Inflation'],
    },
    {
      title: 'Erzeugerpreisindex',
      shortDescription: 'Misst Preisänderungen auf Produktions- bzw. Großhandelsstufe.',
      detailedDescription:
        'Verzögerte Übertragung auf Verbraucherpreise möglich.',
      howToInterpret: 'PPI-VPI-Spread zeigt Pipeline-Druck; Trends lesen.',
      relatedTerms: ['Erzeugerpreise', 'VPI-Index', 'Inflation'],
    },
    { slug: 'ufe', category: 'TURKEY_ECONOMY', type: 'MACRO_INDICATOR', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'Cari denge',
      shortDescription: 'Ülkenin dış ticaret ve gelir/gider akışlarının net sonucudur; cari açık veya fazla üretir.',
      detailedDescription:
        'Türkiye için enerji ithalatı ve turizm gelirleri önemli bileşenlerdir; kur ve büyüme beklentilerini etkiler.',
      howToInterpret:
        'Kalıcı cari açık kur baskısı veya rezerv ihtiyacı yaratabilir; tek çeyrek verisiyle trend çıkarmayın.',
      relatedTerms: ['Döviz', 'GSYİH', 'Enflasyon'],
      targetTerms: ['Cari denge', 'cari açık'],
    },
    {
      title: 'Current account balance',
      shortDescription: 'Net of a country’s trade and income flows; surplus or deficit.',
      detailedDescription:
        'For Turkey, energy imports and tourism matter; influences FX and growth expectations.',
      howToInterpret: 'Persistent deficits can pressure the currency; read trends, not one quarter.',
      relatedTerms: ['Foreign exchange', 'GDP', 'Inflation'],
    },
    {
      title: 'Leistungsbilanz',
      shortDescription: 'Saldo aus Handel und Einkommen; Überschuss oder Defizit.',
      detailedDescription:
        'Energieimporte und Tourismus sind für die Türkei wichtig; beeinflusst FX und Wachstum.',
      howToInterpret: 'Anhaltende Defizite können den Kurs belasten.',
      relatedTerms: ['Wechselkurs', 'BIP', 'Inflation'],
    },
    { slug: 'cari-denge', category: 'TURKEY_ECONOMY', type: 'MACRO_INDICATOR', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'GSYİH',
      shortDescription: 'Gayri safi yurt içi hasıla; bir ülkede üretilen mal ve hizmetlerin toplam değeridir.',
      detailedDescription:
        'Büyüme oranı iş döngüsü ve istihdamla bağlantılıdır; hisse ve tahvil piyasalarına duyarlılık yaratır.',
      howToInterpret:
        'Reel GSYİH büyümesi nominalden anlamlıdır; per capita ile refah kıyası yapılır.',
      relatedTerms: ['Ekonomik Güven Endeksi', 'Enflasyon', 'Politika faizi'],
      targetTerms: ['GSYİH', 'GDP', 'büyüme'],
    },
    {
      title: 'GDP',
      shortDescription: 'Gross Domestic Product—the total value of goods and services produced in a country.',
      detailedDescription:
        'Growth links to the business cycle, jobs, and market sentiment for equities and bonds.',
      howToInterpret: 'Real GDP growth matters more than nominal; compare per capita for living standards.',
      relatedTerms: ['Economic confidence', 'Inflation', 'Policy rate'],
    },
    {
      title: 'BIP',
      shortDescription: 'Bruttoinlandsprodukt—Gesamtwert aller in einem Land produzierten Güter und Dienstleistungen.',
      detailedDescription:
        'Wachstum hängt mit Konjunktur, Arbeitsmarkt und Marktstimmung zusammen.',
      howToInterpret: 'Reales Wachstum ist entscheidend; pro Kopf für Wohlstand vergleichen.',
      relatedTerms: ['Wirtschaftsvertrauen', 'Inflation', 'Leitzins'],
    },
    { slug: 'gsyih', category: 'TURKEY_ECONOMY', type: 'MACRO_INDICATOR', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'Çeşitlendirme',
      shortDescription: 'Riski farklı varlık sınıflarına yayarak tek kaynağa bağımlılığı azaltma stratejisidir.',
      detailedDescription:
        'Hisse, tahvil, altın, döviz ve fon kombinasyonları farklı risk-getiri profilleri sunar; korelasyon zamanla değişir.',
      howToInterpret:
        'Çeşitlendirme riski sıfırlamaz; aynı anda tüm varlıklar düşebilir (korelasyon krizi).',
      relatedTerms: ['Portföy', 'Varlık dağılımı', 'Risk'],
      targetTerms: ['Çeşitlendirme', 'diversification'],
    },
    {
      title: 'Diversification',
      shortDescription: 'Spreading risk across asset classes to reduce reliance on a single bet.',
      detailedDescription:
        'Mixing equities, bonds, gold, FX, and funds offers different profiles; correlations shift in crises.',
      howToInterpret: 'Diversification does not eliminate risk; correlations can rise in stress.',
      relatedTerms: ['Portfolio', 'Asset allocation', 'Risk'],
    },
    {
      title: 'Diversifikation',
      shortDescription: 'Risiko auf mehrere Anlageklassen streuen, statt eine einzelne Wette.',
      detailedDescription:
        'Mix aus Aktien, Anleihen, Gold, FX und Fonds; Korrelationen ändern sich in Krisen.',
      howToInterpret: 'Diversifikation eliminiert Risiko nicht.',
      relatedTerms: ['Portfolio', 'Vermögensallokation', 'Risiko'],
    },
    { slug: 'cesitlendirme', category: 'PORTFOLIO_ANALYSIS', type: 'TERM', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'Temettü',
      shortDescription: 'Şirketin kârından hissedarlara nakit veya bedelsiz pay olarak dağıttığı paydır.',
      detailedDescription:
        'Temettü verimi = temettü / hisse fiyatı; büyüme hisseleri düşük temettü ödeyebilir. Ex-date sonrası fiyat düzeltmesi normaldir.',
      howToInterpret:
        'Sürdürülebilir temettü için nakit akışı ve borç seviyesine bakın; tek seferlik yüksek temettüye kanmayın.',
      relatedTerms: ['Hisse senedi', 'Getiri', 'Piyasa değeri'],
      targetTerms: ['Temettü', 'dividend'],
    },
    {
      title: 'Dividend',
      shortDescription: 'Cash or stock distributions of company profits to shareholders.',
      detailedDescription:
        'Dividend yield = dividend / price; growth names may pay little. Price often adjusts on ex-date.',
      howToInterpret: 'Check cash flow and debt for sustainability; beware one-off specials.',
      relatedTerms: ['Stock', 'Return', 'Market cap'],
    },
    {
      title: 'Dividende',
      shortDescription: 'Ausschüttung von Gewinnen an Aktionäre in Cash oder Aktien.',
      detailedDescription:
        'Dividendenrendite = Dividende / Kurs; Wachstumswerte zahlen oft wenig.',
      howToInterpret: 'Cashflow und Verschuldung für Nachhaltigkeit prüfen.',
      relatedTerms: ['Aktie', 'Rendite', 'Marktkapitalisierung'],
    },
    { slug: 'temettu', category: 'MARKET_DATA', type: 'TERM', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'F/K oranı',
      shortDescription: 'Fiyat/kazanç oranı; hisse fiyatının hisse başına kâra bölünmesidir.',
      detailedDescription:
        'Sektör ortalamalarıyla kıyaslanır; yüksek F/K büyüme beklentisi, düşük F/K değerleme veya düşük büyüme algısı olabilir.',
      howToInterpret:
        'Negatif kârda F/K anlamsızlaşır; tek yıllık kâr yerine normalize kâr kullanın.',
      relatedTerms: ['Hisse senedi', 'Piyasa değeri', 'Getiri'],
      targetTerms: ['F/K', 'P/E', 'fiyat kazanç'],
    },
    {
      title: 'P/E ratio',
      shortDescription: 'Price divided by earnings per share.',
      detailedDescription:
        'Compare within sectors; high P/E may mean growth expectations, low P/E value or low growth.',
      howToInterpret: 'Meaningless with negative earnings; use normalized earnings, not one-offs.',
      relatedTerms: ['Stock', 'Market cap', 'Return'],
    },
    {
      title: 'KGV',
      shortDescription: 'Kurs-Gewinn-Verhältnis: Aktienkurs geteilt durch Gewinn je Aktie.',
      detailedDescription:
        'Sektorspezifisch vergleichen; hohes KGV oft Wachstumserwartung.',
      howToInterpret: 'Bei negativem Gewinn unbrauchbar; bereinigte Gewinne nutzen.',
      relatedTerms: ['Aktie', 'Marktkapitalisierung', 'Rendite'],
    },
    { slug: 'fk-orani', category: 'PORTFOLIO_ANALYSIS', type: 'TERM', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'Beta',
      shortDescription: 'Hisse veya portföyün piyasa hareketlerine göre duyarlılık ölçüsüdür (referans: endeks).',
      detailedDescription:
        'Beta ~1 piyasa ile benzer hareket; >1 daha oynak, <1 daha sakin. Geçmiş veriye dayalıdır.',
      howToInterpret:
        'Beta kısa vadede değişir; düşük beta her zaman düşük risk demek değildir (ör. sektör riski).',
      relatedTerms: ['Risk', 'Volatilite', 'Hisse senedi'],
      targetTerms: ['Beta', 'beta'],
    },
    {
      title: 'Beta',
      shortDescription: 'Sensitivity of a stock or portfolio to broad market moves (index benchmark).',
      detailedDescription:
        'Beta near 1 moves with the market; >1 more volatile, <1 less. Based on historical data.',
      howToInterpret: 'Beta changes over time; low beta is not always low risk (sector risk remains).',
      relatedTerms: ['Risk', 'Volatility', 'Stock'],
    },
    {
      title: 'Beta',
      shortDescription: 'Sensitivität einer Aktie oder eines Portfolios gegenüber dem Gesamtmarkt.',
      detailedDescription:
        'Beta ~1 bewegt sich wie der Markt; >1 volatiler, <1 ruhiger. Historisch geschätzt.',
      howToInterpret: 'Beta ist zeitvariabel; niedriges Beta ≠ immer geringes Risiko.',
      relatedTerms: ['Risiko', 'Volatilität', 'Aktie'],
    },
    { slug: 'beta', category: 'PORTFOLIO_ANALYSIS', type: 'TERM', difficulty: 'ADVANCED' },
  ),
  card(
    {
      title: 'Kaldıraç',
      shortDescription: 'Az sermaye ile büyük pozisyon taşımayı mümkün kılan borçlanma veya türev yapısıdır.',
      detailedDescription:
        'VİOP ve marjlı işlemlerde kazanç ve kayıp oransal büyür; risk yönetimi zorunludur.',
      howToInterpret:
        'Kaldıraç getiriyi büyütür ama likidasyon/ margin call riskini de artırır; deneyimsiz kullanıcılar için tehlikelidir.',
      commonMistake: 'Kaldıraçlı ürünleri “hızlı zenginlik” aracı sanmak.',
      relatedTerms: ['VİOP', 'Risk', 'Volatilite'],
      targetTerms: ['Kaldıraç', 'leverage'],
    },
    {
      title: 'Leverage',
      shortDescription: 'Using debt or derivatives to control a larger position with less capital.',
      detailedDescription:
        'Amplifies gains and losses in futures and margin trading; requires strict risk control.',
      howToInterpret: 'Leverage raises liquidation/margin risk; dangerous for inexperienced users.',
      commonMistake: 'Seeing leveraged products as a quick wealth tool.',
      relatedTerms: ['Futures', 'Risk', 'Volatility'],
    },
    {
      title: 'Hebel',
      shortDescription: 'Fremdkapital oder Derivate, um mit wenig Eigenkapital große Positionen zu steuern.',
      detailedDescription:
        'Verstärkt Gewinne und Verluste; erfordert Risikomanagement.',
      howToInterpret: 'Erhöht Margin- und Liquidationsrisiko.',
      commonMistake: 'Hebelprodukte als schneller Reichtum missverstehen.',
      relatedTerms: ['Futures', 'Risiko', 'Volatilität'],
    },
    { slug: 'kaldirac', category: 'PORTFOLIO_ANALYSIS', type: 'TERM', difficulty: 'ADVANCED' },
  ),
  card(
    {
      title: 'Stablecoin',
      shortDescription: 'Değeri genelde bir fiat para birimine (ör. USD) sabitlenmek üzere tasarlanmış kripto varlıktır.',
      detailedDescription:
        'USDT gibi stablecoin’ler işlem çiftlerinde likidite sağlar; ancak rezerv ve düzenleyici riskleri vardır.',
      howToInterpret:
        '“1 USD’ye sabit” hedefi her zaman korunmayabilir; issuer ve denetim kalitesini ayırt edin.',
      relatedTerms: ['Kripto varlık', 'Likidite', 'Risk'],
      targetTerms: ['Stablecoin', 'USDT'],
    },
    {
      title: 'Stablecoin',
      shortDescription: 'Crypto asset designed to track a fiat currency such as USD.',
      detailedDescription:
        'Pairs like USDT/USDT provide liquidity in crypto markets; reserve and regulatory risks remain.',
      howToInterpret: 'The peg can break; assess issuer transparency and audits.',
      relatedTerms: ['Crypto asset', 'Liquidity', 'Risk'],
    },
    {
      title: 'Stablecoin',
      shortDescription: 'Krypto-Asset, das an eine Fiatwährung wie USD gekoppelt sein soll.',
      detailedDescription:
        'USDT u. a. liefern Liquidität; Reserve- und Regulierungsrisiken bleiben.',
      howToInterpret: 'Peg kann brechen; Emittent und Transparenz prüfen.',
      relatedTerms: ['Krypto-Asset', 'Liquidität', 'Risiko'],
    },
    { slug: 'stablecoin', category: 'MARKET_DATA', type: 'ASSET', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'TCMB kuru ve serbest piyasa',
      shortDescription: 'Resmi referans kurlar ile banka/serbest piyasa efektif kurları arasındaki farktır.',
      detailedDescription:
        'TCMB kurları politika ve resmi işlemlerde referans olabilir; günlük işlemde efektif alış-satış kurları geçerlidir.',
      howToInterpret:
        'Makas genişlediğinde arbitraj veya likidite stresi olabilir; tek kur satırına güvenmeyin.',
      relatedTerms: ['Döviz', 'Efektif kur', 'Spread'],
      targetTerms: ['TCMB', 'serbest piyasa', 'kur'],
    },
    {
      title: 'CBRT vs market FX rates',
      shortDescription: 'Difference between official central bank rates and market/bank effective rates.',
      detailedDescription:
        'CBRT rates are references; everyday trading uses bank effective bid/ask.',
      howToInterpret: 'Wide gaps may signal stress; do not rely on a single quote.',
      relatedTerms: ['Foreign exchange', 'Effective rate', 'Spread'],
    },
    {
      title: 'ZBTR-Kurs vs Marktkurs',
      shortDescription: 'Unterschied zwischen offiziellen Zentralbankkursen und Markt-/Bankenkursen.',
      detailedDescription:
        'ZBTR-Kurse sind Referenz; Handel nutzt effektive Geld-/Briefkurse.',
      howToInterpret: 'Große Spreads können Stress signalisieren.',
      relatedTerms: ['Wechselkurs', 'Effektiver Kurs', 'Spread'],
    },
    { slug: 'tcmb-vs-serbest-piyasa', category: 'MARKET_DATA', type: 'TERM', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'Repo',
      shortDescription: 'Menkul kıymet teminatlı kısa vadeli borçlanma işlemi; merkez bankası likidite aracıdır.',
      detailedDescription:
        'Politika faizi ile ilişkili piyasa faizlerini etkiler; bankalar ve fonlar için önemli para piyasası aracıdır.',
      howToInterpret:
        'Repo faizi yükseliyorsa likidite sıkılaşıyor olabilir; ters repo yatırımcı tarafındadır.',
      relatedTerms: ['Politika faizi', 'Faiz', 'Likidite'],
      targetTerms: ['Repo', 'ters repo'],
    },
    {
      title: 'Repo',
      shortDescription: 'Short-term collateralized borrowing in money markets; used for liquidity management.',
      detailedDescription:
        'Repo rates interact with policy rates and bank funding conditions.',
      howToInterpret: 'Rising repo rates can signal tighter liquidity; reverse repo is the investor side.',
      relatedTerms: ['Policy rate', 'Interest rate', 'Liquidity'],
    },
    {
      title: 'Repo',
      shortDescription: 'Kurzfristige besicherte Geldmarktfinanzierung; Liquiditätsinstrument.',
      detailedDescription:
        'Repo-Sätze hängen mit Leitzins und Bankliquidität zusammen.',
      howToInterpret: 'Steigende Repo-Sätze können Liquiditätsengpässe signalisieren.',
      relatedTerms: ['Leitzins', 'Zins', 'Liquidität'],
    },
    { slug: 'repo', category: 'BASIC_FINANCE', type: 'TERM', difficulty: 'ADVANCED' },
  ),
  card(
    {
      title: 'ISIN',
      shortDescription: 'Uluslararası menkul kıymet kimlik kodu; eurobond ve fonlarda enstrümanı benzersiz tanımlar.',
      detailedDescription:
        'TR… ve US… önekleri ülke ve ihraççı hakkında ipucu verir; işlem ve takas için standarttır.',
      howToInterpret:
        'Aynı ISIN farklı platformlarda aynı enstrümanı ifade eder; sembol yerine ISIN ile arama yapın.',
      relatedTerms: ['Eurobond', 'Tahvil', 'Fon'],
      targetTerms: ['ISIN', 'isin'],
    },
    {
      title: 'ISIN',
      shortDescription: 'International Securities Identification Number for bonds, funds, and equities.',
      detailedDescription:
        'Standard identifier for settlement and lookup across platforms.',
      howToInterpret: 'Same ISIN should mean the same instrument globally; search by ISIN when unsure.',
      relatedTerms: ['Eurobond', 'Bond', 'Fund'],
    },
    {
      title: 'ISIN',
      shortDescription: 'Internationale Wertpapierkennnummer zur eindeutigen Identifikation.',
      detailedDescription:
        'Standard in Abwicklung und Suche über Plattformen hinweg.',
      howToInterpret: 'Gleiche ISIN = gleiches Instrument; bei Unsicherheit ISIN nutzen.',
      relatedTerms: ['Eurobond', 'Anleihe', 'Fonds'],
    },
    { slug: 'isin', category: 'MARKET_DATA', type: 'TERM', difficulty: 'INTERMEDIATE' },
  ),
  card(
    {
      title: 'Sparkline',
      shortDescription: 'Tablo hücresi içindeki küçük mini grafik; kısa dönem fiyat yönünü özetler.',
      detailedDescription:
        'Piyasalar listesindeki trend çizgisi tam geçmiş yerine özet momentum gösterir; ayrıntı için tam grafiğe gidin.',
      howToInterpret:
        'Yön ve eğim önemlidir; eksen ölçeği olmadığı için mutlak fiyat seviyesi okunmaz.',
      relatedTerms: ['Trend skoru', 'Çizgi grafik', 'Dönem getirisi'],
      targetTerms: ['Sparkline', 'mini grafik'],
    },
    {
      title: 'Sparkline',
      shortDescription: 'Small in-cell chart summarizing recent price direction.',
      detailedDescription:
        'Trend lines in market tables compress history; open the full chart for detail.',
      howToInterpret: 'Read slope and direction; absolute levels are not shown without axes.',
      relatedTerms: ['Trend score', 'Line chart', 'Period return'],
    },
    {
      title: 'Sparkline',
      shortDescription: 'Kleines Mini-Diagramm in Tabellenzellen für Kurzzeit-Trend.',
      detailedDescription:
        'Komprimiert Verlauf; für Details vollständigen Chart öffnen.',
      howToInterpret: 'Richtung und Steigung lesen; keine absoluten Preisniveaus.',
      relatedTerms: ['Trend-Score', 'Liniendiagramm', 'Periodenrendite'],
    },
    { slug: 'sparkline', category: 'CHARTS', type: 'CHART', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'Kategori nabzı',
      shortDescription: 'Piyasa segmentlerinin (kripto, BIST, döviz…) eşit ağırlıklı ortalama 1G hareketini gösterir.',
      detailedDescription:
        'Piyasalar üst şeridindeki nabız, o anki genel risk iştahının kaba özetidir; tek enstrüman performansı değildir.',
      howToInterpret:
        'Pozitif nabız çoğu segmentin yeşil olduğu anları özetler; kendi portföyünüzle aynı olmayabilir.',
      relatedTerms: ['Dönem getirisi', 'Isı haritası', 'Volatilite'],
      targetTerms: ['Kategori nabzı', 'pulse', 'segment'],
    },
    {
      title: 'Category pulse',
      shortDescription: 'Equal-weight average 1D move per market segment (crypto, BIST, FX, etc.).',
      detailedDescription:
        'The strip on the Markets page is a coarse mood indicator, not your portfolio return.',
      howToInterpret: 'Positive pulse means most segments are up today; may differ from your holdings.',
      relatedTerms: ['Period return', 'Heat map', 'Volatility'],
    },
    {
      title: 'Kategorie-Puls',
      shortDescription: 'Gleichgewichteter durchschnittlicher 1T-Move je Marktsegment.',
      detailedDescription:
        'Grober Stimmungsindikator auf der Piyasalar-Seite, nicht Ihre Portfolio-Rendite.',
      howToInterpret: 'Positiver Puls = heute meist grüne Segmente.',
      relatedTerms: ['Periodenrendite', 'Heatmap', 'Volatilität'],
    },
    { slug: 'kategori-nabzi', category: 'MARKET_DATA', type: 'TERM', difficulty: 'BEGINNER' },
  ),
  card(
    {
      title: 'Banka kurları',
      shortDescription: 'Bankaların müşteriye sunduğu alış-satış döviz kurları; spread banka politikasına göre değişir.',
      detailedDescription:
        'Portal banka kurları ekranı farklı bankaları karşılaştırmaya yardımcı olur; efektif kur piyasa ortalamasından sapabilir.',
      howToInterpret:
        'Büyük tutarlarda spread pazarlık konusu olabilir; TCMB referansı ile karşılaştırın.',
      relatedTerms: ['Efektif kur', 'Spread', 'Döviz'],
      targetTerms: ['Banka kurları', 'bank rates'],
    },
    {
      title: 'Bank FX rates',
      shortDescription: 'Bank bid/ask quotes for customers; spreads vary by bank policy.',
      detailedDescription:
        'The bank rates page helps compare institutions; may differ from interbank mid rates.',
      howToInterpret: 'Large tickets may negotiate spreads; compare to official references.',
      relatedTerms: ['Effective rate', 'Spread', 'Foreign exchange'],
    },
    {
      title: 'Bank-Devisenkurse',
      shortDescription: 'Bank-Geld-/Briefkurse für Kunden; Spreads bankabhängig.',
      detailedDescription:
        'Vergleichsseite hilft Institute zu vergleichen.',
      howToInterpret: 'Große Beträge oft verhandelbar; mit Referenzkursen abgleichen.',
      relatedTerms: ['Effektiver Kurs', 'Spread', 'Wechselkurs'],
    },
    { slug: 'banka-kurlari', category: 'MARKET_DATA', type: 'TERM', difficulty: 'BEGINNER' },
  ),
]
