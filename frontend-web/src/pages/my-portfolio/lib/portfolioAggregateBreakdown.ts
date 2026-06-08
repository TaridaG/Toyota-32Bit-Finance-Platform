import { tradeFlowTotals } from '../components/tradeFlowStats'
import type {
  Portfolio,
  PortfolioOverview,
  PortfolioOverviewItem,
  PortfolioTradeFlow,
} from '../../../shared/types/portfolio'

export type SymbolPortfolioLine = {
  portfolioId: number
  portfolioName: string
  symbol: string
  value: number
  pnl: number
  quantity: number
}

export type PortfolioTotalsLine = {
  portfolioId: number
  portfolioName: string
  totalValue: number
  totalPnl: number
  dayOverDayChange: number
  tradeBuy: number
  tradeSell: number
  tradeNet: number
}

export type AggregateBreakdown = {
  symbolByKey: Record<string, SymbolPortfolioLine[]>
  categoryByKey: Record<string, SymbolPortfolioLine[]>
  portfolioTotals: PortfolioTotalsLine[]
  pnlWin: SymbolPortfolioLine[]
  pnlLose: SymbolPortfolioLine[]
}

function categoryBucket(item: PortfolioOverviewItem): string {
  const ex = typeof item.exchange === 'string' && item.exchange.trim().length > 0 ? item.exchange.trim() : null
  return ex ?? item.type ?? 'UNKNOWN'
}

function parseDecimal(value: unknown, fallback = 0): number {
  if (value == null) return fallback
  if (typeof value === 'number') return Number.isFinite(value) ? value : fallback
  if (typeof value === 'string') {
    const n = Number(value.trim().replace(/\s/g, '').replace(',', '.'))
    return Number.isFinite(n) ? n : fallback
  }
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

function portfolioLabel(portfolio: Portfolio): string {
  const name = portfolio.name?.trim()
  return name || `#${portfolio.id}`
}

type OverviewEntry = { portfolio: Portfolio; overview: PortfolioOverview | null }
type FlowEntry = { portfolio: Portfolio; flow: PortfolioTradeFlow | null }

export function buildAggregateBreakdown(
  overviews: OverviewEntry[],
  flows: FlowEntry[],
): AggregateBreakdown {
  const symbolByKey: Record<string, SymbolPortfolioLine[]> = {}
  const categoryByKey: Record<string, SymbolPortfolioLine[]> = {}
  const portfolioTotals: PortfolioTotalsLine[] = []
  const pnlWin: SymbolPortfolioLine[] = []
  const pnlLose: SymbolPortfolioLine[] = []
  const eps = 1e-9
  const categoryAcc = new Map<string, Map<number, SymbolPortfolioLine>>()

  const flowById = new Map<number, PortfolioTradeFlow | null>()
  for (const { portfolio, flow } of flows) {
    flowById.set(portfolio.id, flow)
  }

  for (const { portfolio, overview } of overviews) {
    if (!overview) continue

    const flow = flowById.get(portfolio.id)
    const trade = tradeFlowTotals(flow?.points ?? [])
    const name = portfolioLabel(portfolio)

    portfolioTotals.push({
      portfolioId: portfolio.id,
      portfolioName: name,
      totalValue: parseDecimal(overview.totalValue, 0),
      totalPnl: parseDecimal(overview.totalPnl, 0),
      dayOverDayChange: parseDecimal(overview.dayOverDayChange, 0),
      tradeBuy: trade.buy,
      tradeSell: trade.sell,
      tradeNet: trade.net,
    })

    for (const item of overview.items ?? []) {
      const symbol = (item.symbol || item.name || '').trim()
      if (!symbol) continue

      const value = Math.max(0, parseDecimal(item.value, 0))
      const pnl = parseDecimal(item.pnl, 0)
      const quantity = parseDecimal(item.quantity, 0)

      if (value <= eps) continue

      const line: SymbolPortfolioLine = {
        portfolioId: portfolio.id,
        portfolioName: name,
        symbol,
        value,
        pnl,
        quantity,
      }

      if (!symbolByKey[symbol]) symbolByKey[symbol] = []
      symbolByKey[symbol].push(line)

      const catKey = categoryBucket(item)
      let perPortfolio = categoryAcc.get(catKey)
      if (!perPortfolio) {
        perPortfolio = new Map()
        categoryAcc.set(catKey, perPortfolio)
      }
      const existing = perPortfolio.get(portfolio.id)
      if (existing) {
        existing.value += value
        existing.pnl += pnl
        existing.quantity += quantity
      } else {
        perPortfolio.set(portfolio.id, {
          portfolioId: portfolio.id,
          portfolioName: name,
          symbol: catKey,
          value,
          pnl,
          quantity,
        })
      }

      if (pnl > eps) pnlWin.push(line)
      else if (pnl < -eps) pnlLose.push(line)
    }
  }

  for (const [catKey, perPortfolio] of categoryAcc.entries()) {
    categoryByKey[catKey] = [...perPortfolio.values()]
  }

  const sortLines = (lines: SymbolPortfolioLine[]) =>
    [...lines].sort((a, b) => {
      const byName = a.portfolioName.localeCompare(b.portfolioName, undefined, { sensitivity: 'base' })
      if (byName !== 0) return byName
      return b.value - a.value
    })

  for (const key of Object.keys(symbolByKey)) {
    symbolByKey[key] = sortLines(symbolByKey[key]!)
  }
  for (const key of Object.keys(categoryByKey)) {
    categoryByKey[key] = sortLines(categoryByKey[key]!)
  }

  portfolioTotals.sort((a, b) => a.portfolioName.localeCompare(b.portfolioName, undefined, { sensitivity: 'base' }))
  pnlWin.sort((a, b) => b.pnl - a.pnl)
  pnlLose.sort((a, b) => a.pnl - b.pnl)

  return { symbolByKey, categoryByKey, portfolioTotals, pnlWin, pnlLose }
}

export function groupPnlByPortfolio(lines: SymbolPortfolioLine[]): Array<{
  portfolioId: number
  portfolioName: string
  totalPnl: number
  symbols: { symbol: string; pnl: number }[]
}> {
  const map = new Map<
    number,
    { portfolioId: number; portfolioName: string; totalPnl: number; symbols: { symbol: string; pnl: number }[] }
  >()

  for (const line of lines) {
    let group = map.get(line.portfolioId)
    if (!group) {
      group = {
        portfolioId: line.portfolioId,
        portfolioName: line.portfolioName,
        totalPnl: 0,
        symbols: [],
      }
      map.set(line.portfolioId, group)
    }
    group.totalPnl += line.pnl
    group.symbols.push({ symbol: line.symbol, pnl: line.pnl })
  }

  return [...map.values()]
    .map((g) => ({
      ...g,
      symbols: g.symbols.sort((a, b) => Math.abs(b.pnl) - Math.abs(a.pnl)),
    }))
    .sort((a, b) => Math.abs(b.totalPnl) - Math.abs(a.totalPnl))
}
