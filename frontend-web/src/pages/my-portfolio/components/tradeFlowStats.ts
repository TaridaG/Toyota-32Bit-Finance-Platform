import type { PortfolioTradeFlowPoint } from '../../../shared/types/portfolio'
import { RANGE_TO_MS, type ValueChartRange } from './portfolioChartShared'

/** Kart üstündeki alım / satım / net — tüm işlem geçmişi. */
export function tradeFlowTotals(points: PortfolioTradeFlowPoint[]): { buy: number; sell: number; net: number } {
  let buy = 0
  let sell = 0
  for (const p of points) {
    if (p.signedAmount > 0) buy += p.signedAmount
    else sell += -p.signedAmount
  }
  return { buy, sell, net: buy - sell }
}

export function tradeFlowPeriodTotals(
  points: PortfolioTradeFlowPoint[],
  range: ValueChartRange,
): { buy: number; sell: number; net: number } {
  if (range === 'all') {
    return tradeFlowTotals(points)
  }
  const nowMs = Date.now()
  const fromSec = Math.floor((nowMs - RANGE_TO_MS[range]) / 1000)
  const nowSec = Math.floor(nowMs / 1000)
  let buy = 0
  let sell = 0
  for (const p of points) {
    const sec = Math.floor(Date.parse(p.createdAt) / 1000)
    if (!Number.isFinite(sec) || sec < fromSec || sec > nowSec) continue
    if (p.signedAmount > 0) buy += p.signedAmount
    else sell += -p.signedAmount
  }
  return { buy, sell, net: buy - sell }
}
