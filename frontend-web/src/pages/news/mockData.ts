import type { MarketTickerItem, NewsDataPoint, TrendItem } from './types'

export const tickerItems: MarketTickerItem[] = [
  { symbol: 'BIST 100', price: 10412.33, changePercent: 1.24, sparkline: [92, 94, 95, 93, 96, 98, 97, 99] },
  { symbol: 'BTC/USDT', price: 108452.1, changePercent: 2.13, sparkline: [70, 72, 75, 74, 77, 80, 82, 84] },
  { symbol: 'USD/TRY', price: 38.42, changePercent: -0.26, sparkline: [58, 57, 56, 55, 54, 54, 53, 52] },
]

export const newsPoints: NewsDataPoint[] = [
  {
    id: 'fp-001',
    title: 'BIST bankacilik hisselerinde gun ici alicili seyir guclendi',
    summary: 'XBANK liderliginde artan hacim, endeksin kapanisa dogru ivmesini destekledi.',
    details:
      'Kurumsal alimlarin ozellikle bankacilik ve holding gruplarinda yogunlastigi goruldu. Vade sonu yaklasirken likidite guclu kalirken, piyasa derinligindeki toparlanma risk istahini destekliyor.',
    source: 'Piyasa Masasi',
    timeAgoMinutes: 9,
    category: 'bist',
    sentiment: 'positive',
    tags: ['BIST', 'Bankacilik', 'Endeks'],
    relatedAssets: ['XBANK', 'BIST100', 'GARAN'],
    reactionPercent1h: 2.3,
    correlationNote: 'XBANK ile BIST100 arasinda yuksek pozitif korelasyon (0.84).',
    sparkline: [42, 44, 43, 47, 49, 51, 52, 53],
  },
  {
    id: 'fp-002',
    title: 'VIOP kontratlarinda kaldiracli pozisyonlar nedeniyle oynaklik artti',
    summary: 'XU030 yakin vade kontratinda sert geri cekilmeler stop akisini tetikledi.',
    details:
      'Gun ici fiyat hareketlerinde islem yogunlugu artarken, spread tarafinda genisleme dikkat cekti. Teknik seviyelerin kirilmasi, kisa vadede ekstra volatiliteye yol acabilir.',
    source: 'VIOP Wire',
    timeAgoMinutes: 22,
    category: 'viop',
    sentiment: 'negative',
    tags: ['VIOP', 'Vadeli', 'Risk'],
    relatedAssets: ['XU030F', 'USDTRYF'],
    reactionPercent1h: -1.9,
    correlationNote: 'XU030F ile BIST30 arasinda lider-golge iliskisi one cikiyor.',
    sparkline: [76, 74, 73, 71, 69, 67, 66, 64],
  },
  {
    id: 'fp-003',
    title: 'Kur piyasasi dar bantta: USD/TRY volatilitesi sinirli kaldi',
    summary: 'Merkez bankasi beklentileri sonrasinda kur tarafinda denge korunuyor.',
    details:
      'Likiditenin kontrollu seyretmesi ve swap kanalindaki fiyatlama ile kur oynakligi dusuk kaldi. Kisa vadede veri akisina bagli yon tayini bekleniyor.',
    source: 'FX Desk',
    timeAgoMinutes: 37,
    category: 'fx',
    sentiment: 'neutral',
    tags: ['Kur', 'USDTRY', 'Likidite'],
    relatedAssets: ['USDTRY', 'EURTRY', 'DXY'],
    reactionPercent1h: 0.2,
    correlationNote: 'USDTRY-DXY korelasyonu son 24 saatte zayifladi (0.31).',
    sparkline: [55, 55, 54, 54, 54, 53, 53, 53],
  },
  {
    id: 'fp-004',
    title: 'Bitcoin ETF akislari ile spot tarafta hacim artisi suruyor',
    summary: 'Kripto varliklarda spot talep artisi, ana coinlerde yukari yonu destekliyor.',
    details:
      'ETF net girislerinin devam etmesiyle birlikte spot piyasa hacmi son haftanin uzerine cikti. Altcoin tarafinda da korelasyonlu fiyatlama gucleniyor.',
    source: 'Crypto Pulse',
    timeAgoMinutes: 48,
    category: 'crypto',
    sentiment: 'positive',
    tags: ['Kripto', 'BTC', 'ETF'],
    relatedAssets: ['BTCUSDT', 'ETHUSDT', 'SOLUSDT'],
    reactionPercent1h: 3.1,
    correlationNote: 'BTC-ETH korelasyonu yuksek bantta (0.88).',
    sparkline: [61, 64, 66, 67, 69, 71, 73, 75],
  },
  {
    id: 'fp-005',
    title: 'Makro veri sonrasi gelisen piyasalarda risk dagilimi yeniden dengeleniyor',
    summary: 'Faiz beklentilerindeki revizyon, hisse/tahvil dagiliminda rotasyona neden oldu.',
    details:
      'Portfoy yoneticileri kisa vadede defansif sektor agirligini artirirken, beta yuksek varliklarda secici kalmayi surduruyor. Ons altin ve dolar endeksi arasindaki denge izleniyor.',
    source: 'Macro Insight',
    timeAgoMinutes: 71,
    category: 'macro',
    sentiment: 'negative',
    tags: ['Makro', 'Faiz', 'Rotasyon'],
    relatedAssets: ['DXY', 'XAUUSD', 'BIST30'],
    reactionPercent1h: -1.2,
    correlationNote: 'DXY guclendikce gelisen piyasa hisse akislari zayifliyor.',
    sparkline: [82, 80, 79, 77, 76, 75, 74, 73],
  },
  {
    id: 'fp-006',
    title: 'Sanayi hisselerinde bilanco beklentisi alislarini destekliyor',
    summary: 'Ihracat odakli hisselerde kapanisa dogru momentum artisi izlendi.',
    details:
      'Ozellikle enerji ve otomotiv yan sanayi tarafinda hacim destekli yukselis goruldu. Bilanco donemi yaklasirken secici hisse rotasyonu one cikiyor.',
    source: 'Sektor Notlari',
    timeAgoMinutes: 94,
    category: 'bist',
    sentiment: 'positive',
    tags: ['BIST', 'Sanayi', 'Bilanco'],
    relatedAssets: ['XUSIN', 'EREGL', 'TUPRS'],
    reactionPercent1h: 1.6,
    correlationNote: 'XUSIN ve ihracatci hisselerde beta uyumu guclu.',
    sparkline: [47, 49, 48, 50, 52, 53, 54, 56],
  },
]

export const topGainers: TrendItem[] = [
  { asset: 'GARAN', changePercent: 3.2 },
  { asset: 'BTC', changePercent: 2.9 },
  { asset: 'EREGL', changePercent: 1.8 },
  { asset: 'THYAO', changePercent: 1.4 },
]

export const topLosers: TrendItem[] = [
  { asset: 'XU030F', changePercent: -2.1 },
  { asset: 'SASA', changePercent: -1.7 },
  { asset: 'DXY', changePercent: -1.2 },
  { asset: 'USDTRYF', changePercent: -0.9 },
]
