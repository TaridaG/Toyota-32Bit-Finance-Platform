import { getPortfolioTradeFlow, getTransactionHistory } from '../api/portfolioApi'
import type { PortfolioTradeFlow, PortfolioTradeFlowPoint, TransactionHistoryItem } from '../../../shared/types/portfolio'

function normalizeTradeFlowApi(data: PortfolioTradeFlow | null | undefined, fallbackCurrency: string): PortfolioTradeFlow {
  if (!data) return { currency: fallbackCurrency, points: [] }
  const points: PortfolioTradeFlowPoint[] = Array.isArray(data.points)
    ? data.points.map((p) => ({
        transactionId: Number(p.transactionId),
        createdAt: String(p.createdAt),
        signedAmount: typeof p.signedAmount === 'number' ? p.signedAmount : Number(p.signedAmount),
      }))
    : []
  return {
    currency: data.currency || fallbackCurrency,
    points: points.filter((p) => Number.isFinite(p.signedAmount) && Number.isFinite(p.transactionId)),
  }
}

export function buildTradeFlowFromHistory(
  items: TransactionHistoryItem[],
  targetCurrency: string,
): PortfolioTradeFlow {
  const sorted = [...items].sort((a, b) => {
    const ta = Date.parse(a.createdAt)
    const tb = Date.parse(b.createdAt)
    if (ta !== tb) return ta - tb
    return a.transactionId - b.transactionId
  })
  const points: PortfolioTradeFlowPoint[] = []
  for (const row of sorted) {
    const side = row.type?.toUpperCase()
    if (side !== 'BUY' && side !== 'SELL') continue

    // Listing-currency notional (price × qty). Do not use inputAmount here: wallet currency can
    // differ from display currency (e.g. GBP dashboard, TRY-paid XAUTRY) and this path has no FX.
    const notional = Number(row.totalAmount)
    if (!Number.isFinite(notional)) continue

    const signed = side === 'BUY' ? notional : -notional
    points.push({
      transactionId: row.transactionId,
      createdAt: row.createdAt,
      signedAmount: signed,
    })
  }
  return { currency: targetCurrency, points }
}

/**
 * Preferred: GET /api/portfolio/trade-flow (currency-aware).
 * Fallback: transaction list when the endpoint is missing or errors (older backend / gateway).
 */
export async function loadTradeFlowForPortfolio(portfolioId: number, currency: string): Promise<PortfolioTradeFlow> {
  try {
    const data = await getPortfolioTradeFlow(portfolioId, currency)
    return normalizeTradeFlowApi(data, currency)
  } catch {
    try {
      const hist = await getTransactionHistory(portfolioId)
      return buildTradeFlowFromHistory(hist, currency)
    } catch {
      return { currency, points: [] }
    }
  }
}
