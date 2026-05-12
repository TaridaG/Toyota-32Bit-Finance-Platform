/** Admin latency → short status i18n key (threshold 2s). */
export function latencyHealthStatusKey(sec: number): string {
  if (!Number.isFinite(sec) || sec < 0) return 'dashboard.kpi.latencyHealthUnknown'
  if (sec < 0.5) return 'dashboard.kpi.latencyHealthExcellent'
  if (sec < 1.0) return 'dashboard.kpi.latencyHealthGood'
  if (sec < 1.5) return 'dashboard.kpi.latencyHealthOk'
  if (sec < 2.0) return 'dashboard.kpi.latencyHealthSlow'
  return 'dashboard.kpi.latencyHealthCritical'
}

/**
 * Seven “thermometer” segments: left stays greener at low latency; hue shifts toward red as latency
 * approaches 2s and from left to right (user-requested per-bar increase toward red).
 */
export function latencyHealthSegmentColor(sec: number, index: number, total: number): string {
  if (!Number.isFinite(sec) || sec < 0) return 'hsl(142 18% 32%)'
  const r = Math.min(1, Math.max(0, sec / 2))
  const denom = Math.max(1, total - 1)
  const skew = r * (0.2 + 0.8 * (index / denom))
  const hue = 138 * (1 - skew)
  const light = 44 + skew * 14
  const sat = 52 + skew * 38
  return `hsl(${hue} ${sat}% ${light}%)`
}
