import type { CpiHistoryPoint } from '../api/cpiApi'
import { daysBetweenIso, parseAmountLoose } from './depositSimulator'

export { parseAmountLoose }

export type CpiIndexSeriesPoint = {
  date: string
  value: number
}

export type InflationProjection = {
  startDate: string
  endDate: string
  days: number
  months: number
  indexStart: number
  indexEnd: number
  /** I_end / I_start */
  cumulativeFactor: number
  /** (factor - 1) * 100 */
  cumulativeInflationPct: number
  /** Annualized inflation % over the period */
  annualizedInflationPct: number
  /** Nominal TL still held — purchasing power in end-period terms */
  purchasingPowerNominal: number
  /** principal - purchasingPowerNominal */
  purchasingPowerLoss: number
  /** Loss as % of principal */
  purchasingPowerLossPct: number
  /** Nominal TL needed at end to match start purchasing power */
  nominalToPreservePower: number
}

export function normalizeCpiIndexSeries(points: CpiHistoryPoint[]): CpiIndexSeriesPoint[] {
  const rows = (points ?? [])
    .map((p) => {
      const raw = p.value
      const num = typeof raw === 'number' ? raw : Number(raw)
      if (p.date == null || p.date === '' || !Number.isFinite(num) || num <= 0) return null
      const date = p.date.length >= 10 ? p.date.slice(0, 10) : p.date
      return { date, value: num }
    })
    .filter((x): x is CpiIndexSeriesPoint => x != null)
  rows.sort((a, b) => a.date.localeCompare(b.date))
  return rows
}

/** Last CPI observation on or before the given calendar day (inclusive). */
export function findCpiIndexAtOrBefore(series: CpiIndexSeriesPoint[], isoDay: string): CpiIndexSeriesPoint | null {
  if (!isoDay || series.length === 0) return null
  const target = isoDay.slice(0, 10)
  let lo = 0
  let hi = series.length - 1
  let best: CpiIndexSeriesPoint | null = null
  while (lo <= hi) {
    const mid = (lo + hi) >> 1
    const d = series[mid].date
    if (d <= target) {
      best = series[mid]
      lo = mid + 1
    } else {
      hi = mid - 1
    }
  }
  return best
}

/**
 * TÜFE endeks oranıyla satın alma gücü: I_end/I_start bileşik enflasyon.
 * Endeks serisi TCMB aylık genel TÜFE (2003=100) olmalıdır.
 */
export function computeInflationProjection(
  principal: number,
  startDate: string,
  endDate: string,
  indexSeries: CpiIndexSeriesPoint[],
): InflationProjection | null {
  if (principal <= 0 || !startDate || !endDate || startDate > endDate || indexSeries.length < 2) {
    return null
  }
  const startObs = findCpiIndexAtOrBefore(indexSeries, startDate)
  const endObs = findCpiIndexAtOrBefore(indexSeries, endDate)
  if (!startObs || !endObs || startObs.value <= 0) return null
  if (startObs.date > endObs.date) return null

  const days = daysBetweenIso(startDate, endDate)
  if (days <= 0) return null

  const cumulativeFactor = endObs.value / startObs.value
  const cumulativeInflationPct = (cumulativeFactor - 1) * 100
  const years = days / 365.25
  const annualizedInflationPct =
    years > 0 && cumulativeFactor > 0 ? (Math.pow(cumulativeFactor, 1 / years) - 1) * 100 : 0

  const purchasingPowerNominal = principal / cumulativeFactor
  const purchasingPowerLoss = principal - purchasingPowerNominal
  const purchasingPowerLossPct = principal > 0 ? (purchasingPowerLoss / principal) * 100 : 0
  const nominalToPreservePower = principal * cumulativeFactor

  const [ys, ms] = startObs.date.split('-').map(Number)
  const [ye, me] = endObs.date.split('-').map(Number)
  const months = Math.max(1, (ye - ys) * 12 + (me - ms) + 1)

  return {
    startDate: startDate.slice(0, 10),
    endDate: endDate.slice(0, 10),
    days,
    months,
    indexStart: startObs.value,
    indexEnd: endObs.value,
    cumulativeFactor,
    cumulativeInflationPct,
    annualizedInflationPct,
    purchasingPowerNominal,
    purchasingPowerLoss,
    purchasingPowerLossPct,
    nominalToPreservePower,
  }
}
