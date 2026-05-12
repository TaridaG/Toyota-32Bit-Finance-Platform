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

export function formatAdminDecimal(value: number, language: string, maxFractionDigits = 2): string {
  const locale = pickLocale(language)
  return new Intl.NumberFormat(locale, {
    maximumFractionDigits: maxFractionDigits,
    minimumFractionDigits: 0,
  }).format(value)
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

/** Normalizes API daily buckets to exactly seven integers (UTC series). */
export function padSevenDayInts(raw: unknown): number[] {
  const xs = Array.isArray(raw) ? raw.map((v) => (typeof v === 'number' && Number.isFinite(v) ? Math.trunc(v) : 0)) : []
  const out = [...xs]
  while (out.length < 7) out.push(0)
  return out.slice(0, 7)
}
