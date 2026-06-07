import type { PortalPageKey } from '../types/infoCards'

export type PortalPageElementKind = 'BUTTON' | 'LABEL' | 'TEXT' | 'TAB' | 'STAT' | 'COLUMN'

export interface PortalPageElement {
  id: string
  /** Same matchKey on multiple pages = one shared trigger in help mode */
  matchKey: string
  pageKey: PortalPageKey
  label: string
  term: string
  kind: PortalPageElementKind
  section?: string
  /** i18n key for admin pick / multi-locale card titles (namespace defaults to common) */
  labelI18nKey?: string
  labelI18nNs?: string
}

function el(
  pageKey: PortalPageKey,
  matchKey: string,
  label: string,
  kind: PortalPageElementKind,
  section?: string,
  term = label,
  labelI18nKey?: string,
  labelI18nNs?: string,
): PortalPageElement {
  const slug = matchKey.replace(/[^a-z0-9]+/gi, '-').toLowerCase()
  return {
    id: `${pageKey}:${slug}`,
    matchKey,
    pageKey,
    label,
    term,
    kind,
    section,
    labelI18nKey,
    labelI18nNs,
  }
}

export const PORTAL_PAGE_ELEMENTS: PortalPageElement[] = [
  // Banka kurları
  el('BANK_RATES', 'spread', 'Spread', 'LABEL', 'Kur tablosu'),
  el('BANK_RATES', 'doviz', 'Döviz', 'LABEL', 'Kur tablosu'),
  el('BANK_RATES', 'efektif-kur', 'Efektif kur', 'LABEL', 'Kur tablosu'),
  el('BANK_RATES', 'alis', 'Alış', 'COLUMN', 'Kur tablosu'),
  el('BANK_RATES', 'satis', 'Satış', 'COLUMN', 'Kur tablosu'),
  el('BANK_RATES', 'para-birimi', 'Para birimi', 'COLUMN', 'Kur tablosu'),
  el('BANK_RATES', 'guncelle', 'Kurları yenile', 'BUTTON', 'Araç çubuğu'),

  // Faiz / Vadeli
  el('FAIZ_VADELI', 'policy-rate', 'TCMB Politika Faizi', 'STAT', 'Özet kartları', 'Politika faizi'),
  el('FAIZ_VADELI', 'tl-mevduat', 'TL Mevduat', 'STAT', 'Özet kartları'),
  el('FAIZ_VADELI', 'tahvil', 'Tahvil', 'STAT', 'Özet kartları'),
  el('FAIZ_VADELI', 'tr-bono', 'TR Bono', 'STAT', 'Özet kartları', 'TR Bono'),
  el('FAIZ_VADELI', 'repo', 'Repo', 'STAT', 'Özet kartları'),
  el('FAIZ_VADELI', 'eurobond', 'Eurobond', 'STAT', 'Özet kartları'),
  el('FAIZ_VADELI', 'enflasyon', 'Yıllık Enflasyon (TÜFE)', 'STAT', 'Özet kartları', 'TÜFE'),
  el('FAIZ_VADELI', 'tab-deposit', 'Mevduat', 'TAB', 'Sekmeler'),
  el('FAIZ_VADELI', 'tab-bond', 'Tahvil / Bono', 'TAB', 'Sekmeler'),
  el('FAIZ_VADELI', 'tab-auction', 'Hazine ihaleleri', 'TAB', 'Sekmeler'),
  el('FAIZ_VADELI', 'tab-real', 'Reel getiri', 'TAB', 'Sekmeler'),

  // Piyasalar
  el('MARKETS', 'symbol', 'Sembol', 'COLUMN', 'Tablo', 'Sembol'),
  el('MARKETS', 'price', 'Fiyat', 'COLUMN', 'Tablo'),
  el('MARKETS', 'change-1d', '1G Değişim', 'COLUMN', 'Tablo', '1G'),
  el('MARKETS', 'volume', 'Hacim', 'COLUMN', 'Tablo'),
  el('MARKETS', 'market-cap', 'Piyasa değeri', 'COLUMN', 'Tablo'),
  el('MARKETS', 'category-equity', 'Hisse', 'BUTTON', 'Kategori filtresi', 'Hisse', 'categories.stocks', 'markets'),
  el('MARKETS', 'category-bond', 'Tahvil', 'BUTTON', 'Kategori filtresi', 'Tahvil', 'categories.bonds', 'markets'),
  el('MARKETS', 'category-fx', 'Döviz', 'BUTTON', 'Kategori filtresi', 'Döviz', 'categories.forex', 'markets'),
  el('MARKETS', 'category-commodity', 'Emtia', 'BUTTON', 'Kategori filtresi', 'Emtia', 'categories.metals', 'markets'),
  el('MARKETS', 'category-crypto', 'Kripto', 'BUTTON', 'Kategori filtresi', 'Kripto', 'categories.crypto', 'markets'),
  el('MARKETS', 'category-bist', 'BIST', 'BUTTON', 'Kategori filtresi', 'BIST', 'categories.bist', 'markets'),
  el('MARKETS', 'category-nasdaq', 'NASDAQ', 'BUTTON', 'Kategori filtresi', 'NASDAQ', 'categories.nasdaq', 'markets'),
  el('MARKETS', 'search', 'Sembol ara', 'TEXT', 'Araç çubuğu'),
  el('MARKETS', 'watchlist', 'İzleme listesi', 'BUTTON', 'Araç çubuğu'),

  // Portföy
  el('PORTFOLIO', 'total-value', 'Toplam değer', 'STAT', 'Özet'),
  el('PORTFOLIO', 'daily-pnl', 'Günlük kâr/zarar', 'STAT', 'Özet'),
  el('PORTFOLIO', 'allocation', 'Dağılım', 'LABEL', 'Özet'),
  el('PORTFOLIO', 'positions', 'Pozisyonlar', 'TAB', 'Sekmeler'),
  el('PORTFOLIO', 'history', 'İşlem geçmişi', 'TAB', 'Sekmeler'),
  el('PORTFOLIO', 'watchlist', 'İzleme listesi', 'TAB', 'Sekmeler'),

  // Türkiye ekonomisi
  el('TURKEY_ECONOMY', 'gdp', 'GSYİH', 'STAT', 'Makro göstergeler'),
  el('TURKEY_ECONOMY', 'inflation', 'Enflasyon', 'STAT', 'Makro göstergeler', 'TÜFE'),
  el('TURKEY_ECONOMY', 'unemployment', 'İşsizlik', 'STAT', 'Makro göstergeler'),
  el('TURKEY_ECONOMY', 'current-account', 'Cari denge', 'STAT', 'Makro göstergeler'),
  el('TURKEY_ECONOMY', 'fx-reserves', 'Döviz rezervleri', 'STAT', 'Makro göstergeler'),

  // Analiz
  el('ANALYSIS', 'screener', 'Tarama', 'TAB', 'Sekmeler'),
  el('ANALYSIS', 'compare', 'Karşılaştır', 'TAB', 'Sekmeler'),
  el('ANALYSIS', 'technical', 'Teknik analiz', 'TAB', 'Sekmeler'),
  el('ANALYSIS', 'fundamental', 'Temel analiz', 'LABEL', 'Araçlar'),

  // Haberler
  el('NEWS', 'headline', 'Manşet', 'LABEL', 'Liste'),
  el('NEWS', 'source', 'Kaynak', 'COLUMN', 'Liste'),
  el('NEWS', 'filter-market', 'Piyasa haberleri', 'BUTTON', 'Filtre'),

  // Profil / ayarlar
  el('PROFILE', 'settings', 'Profil ayarları', 'LABEL', 'Hesap'),
  el('PROFILE', 'avatar', 'Profil fotoğrafı', 'LABEL', 'Hesap'),
  el('PROFILE', 'preferences', 'Tercihler', 'LABEL', 'Hesap'),

  // Simülasyon
  el('SIMULATION', 'scenario', 'Senaryo', 'LABEL', 'Form'),
  el('SIMULATION', 'run', 'Simülasyonu çalıştır', 'BUTTON', 'Form'),
  el('SIMULATION', 'reset', 'Sıfırla', 'BUTTON', 'Form'),

  // Özet paneli (/app/dashboard)
  el('DASHBOARD', 'portfolio-snapshot', 'Portföy özeti', 'STAT', 'Özet'),
  el('DASHBOARD', 'market-movers', 'Günün hareketleri', 'STAT', 'Özet'),

  // Finansal okuryazarlık
  el('FINANCIAL_LITERACY', 'search-terms', 'Terim ara', 'TEXT', 'Üst alan'),
  el('FINANCIAL_LITERACY', 'category-filter', 'Kategori filtresi', 'BUTTON', 'Filtreler'),
  el('FINANCIAL_LITERACY', 'difficulty-filter', 'Zorluk filtresi', 'BUTTON', 'Filtreler'),

  // Admin
  el('ADMIN', 'overview', 'Yönetim özeti', 'LABEL', 'KPI'),
  el('ADMIN_KPI_USERS', 'total-users', 'Toplam kullanıcı', 'STAT', 'KPI'),
  el('ADMIN_KPI_PORTFOLIOS', 'active-portfolios', 'Aktif portföyler', 'STAT', 'KPI'),
  el('ADMIN_KPI_MARKETS', 'market-streams', 'Piyasa akışları', 'STAT', 'KPI'),
  el('ADMIN_KPI_NEWS', 'news-sources', 'Haber kaynakları', 'STAT', 'KPI'),
  el('ADMIN_KPI_LATENCY', 'avg-latency', 'Ortalama gecikme', 'STAT', 'KPI'),
  el('INFO_CARDS', 'list', 'Bilgi kartı listesi', 'LABEL', 'Bilgi kartları'),
]

const byId = new Map(PORTAL_PAGE_ELEMENTS.map((e) => [e.id, e]))

export function getPortalPageElementById(id: string): PortalPageElement | undefined {
  return byId.get(id)
}

export function getPortalPageElementsByIds(ids: string[]): PortalPageElement[] {
  return ids.map((id) => byId.get(id)).filter((e): e is PortalPageElement => Boolean(e))
}
