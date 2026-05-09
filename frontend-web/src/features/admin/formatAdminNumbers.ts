/** Locale-aware formatting for admin KPIs. */

export function formatAdminInteger(value: number, language: string): string {
  const locale = pickLocale(language)
  return new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(value)
}

export function formatWowPercent(pct: number, language: string): string {
  const locale = pickLocale(language)
  const abs = Math.abs(pct)
  const fmt = new Intl.NumberFormat(locale, { maximumFractionDigits: 2, minimumFractionDigits: 0 }).format(abs)
  if (pct > 0) return `+${fmt}%`
  if (pct < 0) return `−${fmt}%`
  return `${fmt}%`
}

function pickLocale(language: string): string {
  const l = language.toLowerCase()
  if (l.startsWith('tr')) return 'tr-TR'
  if (l.startsWith('de')) return 'de-DE'
  return 'en-US'
}

export function sparklineFromDailyCounts(daily: number[]): number[] {
  if (daily.length === 0) return [0, 0]
  const max = Math.max(...daily, 1)
  return daily.map((d) => 20 + (d / max) * 75)
}
