import type { EurobondHistoryPointWire, EurobondInstrumentWire } from '../api/eurobondMarketApi'
import { daysBetweenIso, parseAmountLoose } from './depositSimulator'

function toNum(v: unknown): number | null {
  if (v == null) return null
  const n = typeof v === 'number' ? v : Number(v)
  return Number.isFinite(n) ? n : null
}

function findHistPoint(points: EurobondHistoryPointWire[], isoDay: string): EurobondHistoryPointWire | undefined {
  return points.find((p) => {
    const d = p.date
    if (!d) return false
    const n = d.length >= 10 ? d.slice(0, 10) : d
    return n === isoDay
  })
}

export type EurobondPastPnl = {
  cost: number
  proceeds: number
  couponCash: number
  pnl: number
  pct: number | null
  saleDay: string
  salePx: number
}

export function computeEurobondPastPnl(params: {
  nominal: string
  purchaseDate: string
  purchasePrice: string
  saleUseLast: boolean
  saleDateStr: string
  salePriceStr: string
  maturityDateStr: string
  histPoints: EurobondHistoryPointWire[]
  selected: EurobondInstrumentWire | undefined
}): EurobondPastPnl | null {
  const N = parseAmountLoose(params.nominal)
  const pBuy = parseAmountLoose(params.purchasePrice)
  const coupon = toNum(params.selected?.couponPercent)
  if (N == null || N <= 0 || pBuy == null || pBuy <= 0 || !params.purchaseDate || coupon == null || coupon < 0) {
    return null
  }
  const sorted = [...params.histPoints]
    .filter((x) => x.date)
    .sort((a, b) => String(a.date).localeCompare(String(b.date)))
  const last = sorted[sorted.length - 1]
  const lastDay = last?.date ? String(last.date).slice(0, 10) : null
  const lastPx = toNum(last?.closePrice)
  let saleDay: string | null
  let salePx: number | null
  if (params.saleUseLast) {
    saleDay = lastDay
    salePx = lastPx
  } else {
    saleDay = params.saleDateStr.length >= 10 ? params.saleDateStr.slice(0, 10) : null
    const manualPx = parseAmountLoose(params.salePriceStr)
    salePx =
      manualPx != null && manualPx > 0
        ? manualPx
        : saleDay
          ? toNum(findHistPoint(params.histPoints, saleDay)?.closePrice)
          : null
  }
  if (!saleDay || salePx == null) return null
  if (params.purchaseDate > saleDay) return null
  const cost = (N * pBuy) / 100
  const proceeds = (N * salePx) / 100
  const matD = (params.maturityDateStr || params.selected?.maturityDate || '').slice(0, 10)
  const couponEnd = matD && saleDay > matD ? matD : saleDay
  const days = daysBetweenIso(params.purchaseDate, couponEnd)
  const semiPeriods = Math.floor(days / 182)
  const couponCash = semiPeriods * ((N * coupon) / 200)
  const pnl = proceeds - cost + couponCash
  const pct = cost > 0 ? (pnl / cost) * 100 : null
  return { cost, proceeds, couponCash, pnl, pct, saleDay, salePx }
}
