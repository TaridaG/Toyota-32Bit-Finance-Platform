import type { SupportedCurrency } from '../../../shared/preferences/preferences'
import type { MarketCategory, MarketOverviewItem } from '../../../shared/types/market'
import {
  championFromRow,
  type MarketChampion,
  type MarketChampionsSnapshot,
} from '../../../pages/markets/lib/marketChampions'
import { apiClient } from '../../../shared/api/client'
import { fetchMarketOverviewPage } from './marketService'

const WEEK_CANDIDATE_SIZE = 60

type SummaryWithWeek = {
  change1W?: number
}

async function fetchPricesSummaryChange1W(symbols: string[]): Promise<Record<string, SummaryWithWeek>> {
  if (symbols.length === 0) {
    return {}
  }
  const SUMMARY_REQUEST_CHUNK = 40
  const out: Record<string, SummaryWithWeek> = {}
  for (let i = 0; i < symbols.length; i += SUMMARY_REQUEST_CHUNK) {
    const chunk = symbols.slice(i, i + SUMMARY_REQUEST_CHUNK)
    try {
      const response = await apiClient.get<Record<string, { change1W?: number }>>('/api/v1/market/prices/summary', {
        params: { symbols: chunk.join(',') },
      })
      const data = response.data ?? {}
      for (const symbol of chunk) {
        const item = data[symbol]
        if (item && Number.isFinite(item.change1W)) {
          out[symbol] = { change1W: item.change1W }
        }
      }
    } catch {
      /* skip chunk */
    }
  }
  return out
}

function pickWeekChampion(
  rows: MarketOverviewItem[],
  summaries: Record<string, SummaryWithWeek>,
): MarketChampion | null {
  let best: MarketChampion | null = null
  const nameBySymbol = new Map(rows.map((row) => [row.symbol, row.name]))
  for (const [symbol, summary] of Object.entries(summaries)) {
    const change1W = summary.change1W
    if (change1W == null || !Number.isFinite(change1W)) {
      continue
    }
    if (!best || change1W > best.changePct) {
      best = {
        symbol,
        name: nameBySymbol.get(symbol) ?? symbol,
        changePct: change1W,
      }
    }
  }
  return best
}

export async function fetchMarketChampions(
  displayCurrency: SupportedCurrency,
  category: MarketCategory = 'all',
): Promise<MarketChampionsSnapshot> {
  const base = {
    page: 0,
    category,
    query: '',
    displayCurrency,
  }

  const [dayPage, monthPage, yearPage, dayLeaders, monthLeaders] = await Promise.all([
    fetchMarketOverviewPage({ ...base, size: 1, sort: 'change1D,desc' }),
    fetchMarketOverviewPage({ ...base, size: 1, sort: 'change1M,desc' }),
    fetchMarketOverviewPage({ ...base, size: 1, sort: 'change1Y,desc' }),
    fetchMarketOverviewPage({ ...base, size: WEEK_CANDIDATE_SIZE, sort: 'change1D,desc' }),
    fetchMarketOverviewPage({ ...base, size: WEEK_CANDIDATE_SIZE, sort: 'change1M,desc' }),
  ])

  const dayRow = dayPage.content[0]
  const monthRow = monthPage.content[0]
  const yearRow = yearPage.content[0]

  const candidateRows = [...dayLeaders.content, ...monthLeaders.content]
  const symbols = [...new Set(candidateRows.map((row) => row.symbol))]
  const weekSummaries = await fetchPricesSummaryChange1W(symbols)

  return {
    day: championFromRow(dayRow, dayRow?.change1D ?? dayRow?.change24h),
    week: pickWeekChampion(candidateRows, weekSummaries),
    month: championFromRow(monthRow, monthRow?.change1M),
    year: championFromRow(yearRow, yearRow?.change1Y),
  }
}
