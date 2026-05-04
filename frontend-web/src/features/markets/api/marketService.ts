import { apiClient } from '../../../shared/api/client'
import type { MarketCategory, MarketInsightsResponse, MarketOverviewPageResponse } from '../../../shared/types/market'

type FetchMarketsParams = {
  page: number
  size: number
  category?: MarketCategory
  query?: string
  sort?: string
}

const categoryToQueryParam: Record<Exclude<MarketCategory, 'all'>, string> = {
  crypto: 'CRYPTO',
  stocks: 'STOCK',
  forex: 'FX',
  commodities: 'FUND',
}

type MarketPriceApiItem = {
  symbol?: string
  price?: number | string
  value?: number | string
  timestamp?: string | null
  change24h?: number | string | null
  high24h?: number | string | null
  low24h?: number | string | null
}

type FxRateApiItem = {
  symbol?: string
  bid?: number | string
  ask?: number | string
  mid?: number | string
  timestamp?: string | null
}

type HistoryPoint = {
  time?: string
  value?: number | string
}

type PeriodChanges = {
  change1D: number
  change1M: number
  change3M: number
  change6M: number
  change1Y: number
}

type SummaryItem = {
  price?: number | string
  change1D?: number
  change1M?: number
  change3M?: number
  change6M?: number
  change1Y?: number
}

type ParsedHistoryPoint = {
  time: number
  value: number
}

function toCategory(symbol: string): string {
  if (symbol.endsWith('USDT') || symbol.endsWith('USD')) return 'CRYPTO'
  if (symbol.endsWith('TRY') || symbol.includes('/')) return 'FX'
  return 'STOCK'
}

function toNumber(value: unknown, fallback: number): number {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : fallback
}

function toFreshness(timestamp: string | null | undefined): 'LIVE' | 'STALE' {
  if (!timestamp) {
    return 'STALE'
  }
  const ts = Date.parse(timestamp)
  if (!Number.isFinite(ts)) {
    return 'STALE'
  }
  return Date.now() - ts > 5 * 60 * 1000 ? 'STALE' : 'LIVE'
}

function normalizeMarketRows(items: MarketPriceApiItem[]) {
  return items
    .filter((item) => typeof item?.symbol === 'string' && item.symbol.trim().length > 0)
    .map((item) => {
      const symbol = String(item.symbol).trim().toUpperCase()
      const price = toNumber(item.price ?? item.value ?? 0, 0)
      return {
        symbol,
        name: symbol,
        price,
        timestamp: item.timestamp ?? null,
        freshness: toFreshness(item.timestamp),
        change24h: toNumber(item.change24h ?? 0, 0),
        high24h: toNumber(item.high24h ?? item.price ?? item.value ?? 0, price),
        low24h: toNumber(item.low24h ?? item.price ?? item.value ?? 0, price),
        category: toCategory(symbol),
        instrumentId: null,
      }
    })
}

function normalizeFxRows(items: FxRateApiItem[]) {
  return items
    .filter((item) => typeof item?.symbol === 'string' && item.symbol.trim().length > 0)
    .map((item) => {
      const symbol = String(item.symbol).trim().toUpperCase()
      const price = toNumber(item.mid ?? item.ask ?? item.bid ?? 0, 0)
      return {
        symbol,
        name: symbol,
        price,
        timestamp: item.timestamp ?? null,
        freshness: toFreshness(item.timestamp),
        change24h: 0,
        high24h: price,
        low24h: price,
        category: 'FX',
        instrumentId: null,
      }
    })
}

function toDateParam(value: Date): string {
  return value.toISOString().slice(0, 10)
}

function rangeStartForDays(to: Date, days: number): Date {
  const from = new Date(to)
  const inclusiveDays = Math.max(1, days)
  from.setDate(to.getDate() - (inclusiveDays - 1))
  return from
}

export async function fetchHistory(symbol: string, days: number): Promise<HistoryPoint[]> {
  const to = new Date()
  const from = rangeStartForDays(to, days)

  const response = await apiClient.get<HistoryPoint[]>('/api/market/prices/history', {
    params: {
      symbol,
      from: toDateParam(from),
      to: toDateParam(to),
    },
  })

  return Array.isArray(response.data) ? response.data : []
}

async function fetchFxHistory(symbol: string, days: number): Promise<HistoryPoint[]> {
  const to = new Date()
  const from = rangeStartForDays(to, days)

  const response = await apiClient.get<HistoryPoint[]>('/api/market/fx/history', {
    params: {
      symbol,
      from: toDateParam(from),
      to: toDateParam(to),
    },
  })

  return Array.isArray(response.data) ? response.data : []
}

function parseHistory(points: HistoryPoint[]): ParsedHistoryPoint[] {
  return points
    .map((p) => ({
      time: Date.parse(String(p.time ?? '')),
      value: toNumber(p.value, NaN),
    }))
    .filter((p) => Number.isFinite(p.time) && Number.isFinite(p.value))
    .sort((a, b) => a.time - b.time)
}

function computePeriodChanges(points: HistoryPoint[]): PeriodChanges {
  const parsed = parseHistory(points)
  if (parsed.length < 2) {
    return { change1D: 0, change1M: 0, change3M: 0, change6M: 0, change1Y: 0 }
  }
  const latest = parsed[parsed.length - 1].value
  const now = Date.now()

  const changeForDays = (days: number): number => {
    const threshold = now - days * 24 * 60 * 60 * 1000
    const baselinePoint = parsed.find((p) => p.time >= threshold) ?? parsed[0]
    if (!baselinePoint || baselinePoint.value === 0) {
      return 0
    }
    return ((latest - baselinePoint.value) / baselinePoint.value) * 100
  }

  return {
    change1D: changeForDays(1),
    change1M: changeForDays(30),
    change3M: changeForDays(90),
    change6M: changeForDays(180),
    change1Y: changeForDays(365),
  }
}

async function fetchPricesSummary(symbols: string[]): Promise<Record<string, PeriodChanges & { price: number }>> {
  if (symbols.length === 0) {
    return {}
  }
  const response = await apiClient.get<Record<string, SummaryItem>>('/api/market/prices/summary', {
    params: { symbols: symbols.join(',') },
  })
  const data = response.data ?? {}
  const out: Record<string, PeriodChanges & { price: number }> = {}
  for (const symbol of symbols) {
    const item = data[symbol]
    if (!item) {
      continue
    }
    out[symbol] = {
      price: toNumber(item.price ?? 0, 0),
      change1D: toNumber(item.change1D ?? 0, 0),
      change1M: toNumber(item.change1M ?? 0, 0),
      change3M: toNumber(item.change3M ?? 0, 0),
      change6M: toNumber(item.change6M ?? 0, 0),
      change1Y: toNumber(item.change1Y ?? 0, 0),
    }
  }
  return out
}

export async function fetchMarketOverview(params: FetchMarketsParams): Promise<MarketOverviewPageResponse> {
  const category = params.category
  const query = params.query?.trim()
  const sort = params.sort?.trim()
  const response = await apiClient.get<MarketPriceApiItem[] | { data?: MarketPriceApiItem[] }>('/api/market/prices')
  const responseItems = Array.isArray(response.data)
    ? response.data
    : Array.isArray(response.data?.data)
      ? response.data.data
      : []

  const fxResponse = await apiClient.get<FxRateApiItem[] | { data?: FxRateApiItem[] }>('/api/market/fx')
  const fxResponseItems = Array.isArray(fxResponse.data)
    ? fxResponse.data
    : Array.isArray(fxResponse.data?.data)
      ? fxResponse.data.data
      : []

  const normalizedRows = [
    ...normalizeMarketRows(responseItems),
    ...normalizeFxRows(fxResponseItems),
  ]
  const filteredByCategory =
    category && category !== 'all'
      ? normalizedRows.filter((row) => row.category === categoryToQueryParam[category])
      : normalizedRows
  const filteredBySearch = query
    ? filteredByCategory.filter((row) => row.symbol.includes(query.toUpperCase()))
    : filteredByCategory

  const sorted = [...filteredBySearch]
  if (sort) {
    const [field, direction] = sort.split(',')
    if (field === 'price') {
      sorted.sort((a, b) => (direction === 'asc' ? a.price - b.price : b.price - a.price))
    } else if (field === 'change24h') {
      sorted.sort((a, b) => {
        const av = a.change24h ?? 0
        const bv = b.change24h ?? 0
        return direction === 'asc' ? av - bv : bv - av
      })
    }
  }

  const safePage = Math.max(params.page, 0)
  const safeSize = Math.max(params.size, 1)
  const start = safePage * safeSize
  const end = start + safeSize
  const basePageRows = sorted.slice(start, end)
  let summaryBySymbol: Record<string, PeriodChanges & { price: number }> = {}
  const summarySymbols = basePageRows
    .filter((row) => row.category !== 'FX')
    .map((row) => row.symbol)
  try {
    summaryBySymbol = await fetchPricesSummary(summarySymbols)
  } catch {
    summaryBySymbol = {}
  }

  const fxPeriodBySymbol: Record<string, PeriodChanges> = {}
  const fxSymbols = basePageRows
    .filter((row) => row.category === 'FX')
    .map((row) => row.symbol)
  await Promise.all(
    fxSymbols.map(async (symbol) => {
      try {
        const history = await fetchFxHistory(symbol, 365)
        fxPeriodBySymbol[symbol] = computePeriodChanges(history)
      } catch {
        fxPeriodBySymbol[symbol] = { change1D: 0, change1M: 0, change3M: 0, change6M: 0, change1Y: 0 }
      }
    }),
  )

  const content = basePageRows.map((row) => {
    const summary = row.category === 'FX'
      ? fxPeriodBySymbol[row.symbol] ?? { change1D: 0, change1M: 0, change3M: 0, change6M: 0, change1Y: 0 }
      : summaryBySymbol[row.symbol] ?? {}
    return {
      ...row,
      symbol: row.symbol,
      price: row.price ?? 0,
      timestamp: row.timestamp ?? null,
      freshness: row.freshness ?? 'STALE',
      change24h: summary.change1D ?? 0,
      change1D: summary.change1D ?? 0,
      change1M: summary.change1M ?? 0,
      change3M: summary.change3M ?? 0,
      change6M: summary.change6M ?? 0,
      change1Y: summary.change1Y ?? 0,
    }
  })
  const totalElements = sorted.length
  const totalPages = Math.max(1, Math.ceil(totalElements / safeSize))

  return {
    content,
    page: safePage,
    size: safeSize,
    totalElements,
    totalPages,
  }
}

export async function fetchMarketInsights(): Promise<MarketInsightsResponse> {
  const response = await apiClient.get<MarketPriceApiItem[] | { data?: MarketPriceApiItem[] }>('/api/market/prices')
  const responseItems = Array.isArray(response.data)
    ? response.data
    : Array.isArray(response.data?.data)
      ? response.data.data
      : []
  const normalizedRows = normalizeMarketRows(responseItems)
  return {
    topGainers: normalizedRows.slice(0, 5),
    topLosers: normalizedRows.slice(-5).reverse(),
  }
}
