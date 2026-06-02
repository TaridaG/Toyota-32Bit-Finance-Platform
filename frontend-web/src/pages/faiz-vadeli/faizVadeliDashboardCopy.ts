/** UI-only demo copy for the Faiz/Vadeli dashboard (no live data). */

export type FaizVadeliLocale = 'tr' | 'en' | 'de'

export type StatIconId = 'bank' | 'coins' | 'doc' | 'spread' | 'bell' | 'metal'

export type StatCardCopy = {
  /** When set, dashboard replaces mock value with live TCMB policy rate from `/api/rates/policy-rate/latest`. */
  statSlot?: 'policy_rate' | 'tl_deposit' | 'tahvil' | 'repo' | 'eurobond' | 'inflation'
  icon: StatIconId
  title: string
  value: string
  sub1: string
  sub2?: string
  delta?: string
  deltaTone?: 'positive' | 'negative' | 'neutral'
  badge?: string
}

export type FaizVadeliDashboardCopy = {
  stats: StatCardCopy[]
}

const TR: FaizVadeliDashboardCopy = {
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
      title: 'VİOP Faiz/Tahvil',
      value: '%39,67',
      sub1: 'Yakın vade faiz türevi',
      delta: '0,00',
      deltaTone: 'positive',
    },
    {
      statSlot: 'repo',
      icon: 'doc',
      title: 'repo',
      value: '—',
      sub1: '',
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
      statSlot: 'inflation',
      icon: 'bell',
      title: 'Enflasyon',
      value: '%69,80',
      sub1: 'Nisan 2024',
      delta: '+1,20',
      deltaTone: 'negative',
    },
  ],
}

const EN: FaizVadeliDashboardCopy = {
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
      title: 'VIOP rates/bond',
      value: '39.67%',
      sub1: 'Near-contract rate derivative',
      delta: '0.00',
      deltaTone: 'positive',
    },
    {
      statSlot: 'repo',
      icon: 'doc',
      title: 'Repo',
      value: '—',
      sub1: '',
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
      statSlot: 'inflation',
      icon: 'bell',
      title: 'Inflation',
      value: '69.80%',
      sub1: 'April 2024',
      delta: '+1.20',
      deltaTone: 'negative',
    },
  ],
}

const DE: FaizVadeliDashboardCopy = {
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
      title: 'VIOP Zins/Anleihe',
      value: '39,67 %',
      sub1: 'Zinsderivat mit naher Faelligkeit',
      delta: '0,00',
      deltaTone: 'positive',
    },
    {
      statSlot: 'repo',
      icon: 'doc',
      title: 'Repo',
      value: '—',
      sub1: '',
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
      statSlot: 'inflation',
      icon: 'bell',
      title: 'Inflation',
      value: '69,80 %',
      sub1: 'April 2024',
      delta: '+1,20',
      deltaTone: 'negative',
    },
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
