/** UI-only demo copy for the Faiz/Vadeli dashboard (no live data). */

export type FaizVadeliLocale = 'tr' | 'en' | 'de'

export type StatIconId = 'bank' | 'coins' | 'doc' | 'spread' | 'bell'

export type StatCardCopy = {
  /** When set, dashboard replaces mock value with live TCMB policy rate from `/api/rates/policy-rate/latest`. */
  statSlot?: 'policy_rate' | 'tl_deposit' | 'tahvil' | 'eurobond'
  icon: StatIconId
  title: string
  value: string
  sub1: string
  sub2?: string
  delta?: string
  deltaTone?: 'positive' | 'negative' | 'neutral'
  badge?: string
}

export type TabId = 'deposit' | 'bond' | 'auction' | 'real'

export type TabCopy = { id: TabId; label: string }

export type FaizVadeliDashboardCopy = {
  tabPlaceholder: string
  tabsAria: string
  stats: StatCardCopy[]
  tabs: TabCopy[]
}

const TR: FaizVadeliDashboardCopy = {
  tabPlaceholder: 'Bu sekme için ek düzen, veri bağlandığında genişletilebilir.',
  tabsAria: 'Faiz ve vadeli görünümleri',
  stats: [
    {
      statSlot: 'policy_rate',
      icon: 'bank',
      title: 'TCMB Politika Faizi',
      value: '%50,00',
      sub1: 'Son Karar: 23 Mayıs 2024',
      badge: 'Sabit',
    },
    {
      statSlot: 'tl_deposit',
      icon: 'coins',
      title: 'TL Mevduat',
      value: '%48,70',
      sub1: 'Önceki: %48,10',
      delta: '+0,60',
      deltaTone: 'positive',
    },
    {
      statSlot: 'tahvil',
      icon: 'doc',
      title: 'tahvil',
      value: '%39,67',
      sub1: '1G Değişim: 0 bp',
      delta: '0,00',
      deltaTone: 'positive',
    },
    {
      icon: 'doc',
      title: 'repo',
      value: '%28,40',
      sub1: '1G Değişim: -12 bp',
      delta: '-0,12',
      deltaTone: 'negative',
    },
    {
      statSlot: 'eurobond',
      icon: 'spread',
      title: 'eurobond',
      value: '+11,27',
      sub1: 'Önceki: +11,15',
      delta: '+0,12',
      deltaTone: 'positive',
    },
    {
      icon: 'bell',
      title: 'Yıllık Enflasyon (TÜFE)',
      value: '%69,80',
      sub1: 'Nisan 2024',
      delta: '+1,20',
      deltaTone: 'negative',
    },
  ],
  tabs: [
    { id: 'deposit', label: 'Mevduat' },
    { id: 'bond', label: 'Tahvil / Bono' },
    { id: 'auction', label: 'Hazine İhaleleri' },
    { id: 'real', label: 'Reel Getiri' },
  ],
}

const EN: FaizVadeliDashboardCopy = {
  tabPlaceholder: 'Additional layout for this tab can ship once data is wired.',
  tabsAria: 'Interest and term deposit views',
  stats: [
    {
      statSlot: 'policy_rate',
      icon: 'bank',
      title: 'CBRT policy rate',
      value: '50.00%',
      sub1: 'Last decision: 23 May 2024',
      badge: 'Fixed',
    },
    {
      statSlot: 'tl_deposit',
      icon: 'coins',
      title: 'TRY deposits (≤1Y, stock)',
      value: '48.70%',
      sub1: 'Previous: 48.10%',
      delta: '+0.60',
      deltaTone: 'positive',
    },
    {
      statSlot: 'tahvil',
      icon: 'doc',
      title: 'Treasury yield',
      value: '39.67%',
      sub1: '1D change: 0 bp',
      delta: '0.00',
      deltaTone: 'positive',
    },
    {
      icon: 'doc',
      title: 'Repo',
      value: '28.40%',
      sub1: '1D change: −12 bp',
      delta: '−0.12',
      deltaTone: 'negative',
    },
    {
      statSlot: 'eurobond',
      icon: 'spread',
      title: 'Eurobond',
      value: '+11.27',
      sub1: 'Previous: +11.15',
      delta: '+0.12',
      deltaTone: 'positive',
    },
    {
      icon: 'bell',
      title: 'Annual CPI inflation',
      value: '69.80%',
      sub1: 'April 2024',
      delta: '+1.20',
      deltaTone: 'negative',
    },
  ],
  tabs: [
    { id: 'deposit', label: 'Deposits' },
    { id: 'bond', label: 'Bonds / bills' },
    { id: 'auction', label: 'Treasury auctions' },
    { id: 'real', label: 'Real yield' },
  ],
}

const DE: FaizVadeliDashboardCopy = {
  tabPlaceholder: 'Erweiterte Ansichten für diesen Tab folgen mit Datenanbindung.',
  tabsAria: 'Ansichten Zinsen und Laufzeiten',
  stats: [
    {
      statSlot: 'policy_rate',
      icon: 'bank',
      title: 'Leitzins (Zentralbank)',
      value: '50,00 %',
      sub1: 'Letzte Entscheidung: 23. Mai 2024',
      badge: 'Unverändert',
    },
    {
      statSlot: 'tl_deposit',
      icon: 'coins',
      title: 'TRY-Einlagen (≤1J, Bestand)',
      value: '48,70 %',
      sub1: 'Zuvor: 48,10 %',
      delta: '+0,60',
      deltaTone: 'positive',
    },
    {
      statSlot: 'tahvil',
      icon: 'doc',
      title: 'Staatsanleihe',
      value: '39,67 %',
      sub1: '1T-Änderung: 0 bp',
      delta: '0,00',
      deltaTone: 'positive',
    },
    {
      icon: 'doc',
      title: 'Repo',
      value: '28,40 %',
      sub1: '1T-Änderung: −12 bp',
      delta: '−0,12',
      deltaTone: 'negative',
    },
    {
      statSlot: 'eurobond',
      icon: 'spread',
      title: 'Eurobond',
      value: '+11,27',
      sub1: 'Zuvor: +11,15',
      delta: '+0,12',
      deltaTone: 'positive',
    },
    {
      icon: 'bell',
      title: 'Inflation (VPI, jährlich)',
      value: '69,80 %',
      sub1: 'April 2024',
      delta: '+1,20',
      deltaTone: 'negative',
    },
  ],
  tabs: [
    { id: 'deposit', label: 'Einlagen' },
    { id: 'bond', label: 'Anleihen' },
    { id: 'auction', label: 'Auktionen' },
    { id: 'real', label: 'Realrendite' },
  ],
}

const COPIES: Record<FaizVadeliLocale, FaizVadeliDashboardCopy> = { tr: TR, en: EN, de: DE }

export function resolveFaizVadeliLocale(lang: string | undefined): FaizVadeliLocale {
  const l = (lang ?? 'en').toLowerCase()
  if (l.startsWith('tr')) return 'tr'
  if (l.startsWith('de')) return 'de'
  return 'en'
}

export function getFaizVadeliDashboardCopy(lang: string | undefined): FaizVadeliDashboardCopy {
  return COPIES[resolveFaizVadeliLocale(lang)]
}
