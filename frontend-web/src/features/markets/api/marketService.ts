import { apiClient } from '../../../shared/api/client'
import type {
  InstrumentFundamentals,
  MarketCategory,
  MarketInsightsResponse,
  MarketOverviewPageResponse,
} from '../../../shared/types/market'
import type { SupportedCurrency } from '../../../shared/preferences/preferences'
import {
  buildFxTryHub,
  convertToDisplayCurrency,
  inferNativeQuote,
} from '../lib/marketDisplayConversion'
import {
  parseMarketSortQuery,
  SORT_DISPLAY_AMOUNT_KEY,
  sortMarketOverviewRows,
  sortRequiresPageSummaryFetch,
  sortRequiresPeriodPrefetch,
} from '../lib/marketSort'

type FetchMarketsParams = {
  page: number
  size: number
  category?: MarketCategory
  query?: string
  sort?: string
  /** Header currency: drives `displayAmount` TRY-bridge conversion on each row. */
  displayCurrency?: SupportedCurrency
}

const categoryToQueryParam: Record<Exclude<MarketCategory, 'all'>, string> = {
  crypto: 'CRYPTO',
  bist: 'STOCK',
  nasdaq: 'STOCK',
  forex: 'FX',
  metals: 'FX',
  globalFutures: 'STOCK',
  funds: 'FUND',
}

type MarketPriceApiItem = {
  symbol?: string
  price?: number | string
  value?: number | string
  source?: string
  category?: string | null
  instrumentType?: string | null
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

type InstrumentCatalogItem = {
  id?: number | string
  symbol?: string
  name?: string
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

const FUND_SYMBOLS = new Set(['VOO', 'VTI', 'QQQ', 'IVV', 'SPY'])
const SPOT_METAL_SYMBOLS = new Set(['XAUTRY', 'XAGTRY', 'XPTTRY', 'XPDTRY', 'XCUTRY'])
const METAL_FUTURES_SYMBOLS = new Set(['GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F'])
const METAL_SYMBOLS = new Set([...SPOT_METAL_SYMBOLS, ...METAL_FUTURES_SYMBOLS])
type InstrumentMetadata = {
  id: number
  name: string | null
}

let instrumentMetadataBySymbolCache: Record<string, InstrumentMetadata> | null = null
let instrumentMetadataBySymbolPromise: Promise<Record<string, InstrumentMetadata>> | null = null

function toCategory(symbol: string, item?: MarketPriceApiItem): string {
  const hinted =
    (item?.instrumentType ?? item?.category ?? '')
      .toString()
      .trim()
      .toUpperCase()
  if (hinted === 'FUND' || hinted === 'BOND' || hinted === 'FX' || hinted === 'CRYPTO' || hinted === 'STOCK') {
    return hinted
  }
  if (METAL_SYMBOLS.has(symbol)) return 'METAL'
  if (FUND_SYMBOLS.has(symbol)) return 'FUND'
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
        source: item.source ? String(item.source).trim().toUpperCase() : null,
        timestamp: item.timestamp ?? null,
        freshness: toFreshness(item.timestamp),
        change24h: toNumber(item.change24h ?? 0, 0),
        high24h: toNumber(item.high24h ?? item.price ?? item.value ?? 0, price),
        low24h: toNumber(item.low24h ?? item.price ?? item.value ?? 0, price),
        category: toCategory(symbol, item),
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
        source: 'TCMB',
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

function toInstrumentMapPayload(
  responseData:
    | { success?: boolean; data?: InstrumentCatalogItem[] | null }
    | InstrumentCatalogItem[]
    | null
    | undefined,
): InstrumentCatalogItem[] {
  if (Array.isArray(responseData)) {
    return responseData
  }
  if (Array.isArray(responseData?.data)) {
    return responseData.data
  }
  return []
}

async function fetchInstrumentMetadataBySymbolMap(): Promise<Record<string, InstrumentMetadata>> {
  if (instrumentMetadataBySymbolCache) {
    return instrumentMetadataBySymbolCache
  }
  if (instrumentMetadataBySymbolPromise) {
    return instrumentMetadataBySymbolPromise
  }
  instrumentMetadataBySymbolPromise = apiClient
    .get<{ success?: boolean; data?: InstrumentCatalogItem[] } | InstrumentCatalogItem[]>('/api/instruments')
    .then(({ data }) => {
      const items = toInstrumentMapPayload(data)
      const map: Record<string, InstrumentMetadata> = {}
      items.forEach((item) => {
        const symbol = typeof item?.symbol === 'string' ? item.symbol.trim().toUpperCase() : ''
        const id = Number(item?.id)
        const name = typeof item?.name === 'string' && item.name.trim().length > 0 ? item.name.trim() : null
        if (symbol && Number.isFinite(id)) {
          map[symbol] = { id, name }
        }
      })
      instrumentMetadataBySymbolCache = map
      return map
    })
    .catch(() => ({}))
    .finally(() => {
      instrumentMetadataBySymbolPromise = null
    })
  return instrumentMetadataBySymbolPromise
}

export type CatalogRow = {
  symbol: string
  name: string
  price: number
  source: string | null
  timestamp: string | null
  freshness: 'LIVE' | 'STALE'
  change24h: number
  high24h: number
  low24h: number
  category: string
  instrumentId: number | null
}

function filterRowsByCategory(rows: CatalogRow[], category?: MarketCategory): CatalogRow[] {
  if (!category || category === 'all') {
    return rows
  }
  if (category === 'bist') {
    return rows.filter((row) => row.category === 'STOCK' && row.source === 'YAHOO')
  }
  if (category === 'nasdaq') {
    return rows.filter((row) => row.category === 'STOCK' && row.source === 'FINNHUB')
  }
  if (category === 'forex') {
    return rows.filter((row) => row.category === 'FX' && !METAL_SYMBOLS.has(row.symbol))
  }
  if (category === 'metals') {
    return rows.filter((row) => SPOT_METAL_SYMBOLS.has(row.symbol))
  }
  if (category === 'globalFutures') {
    return rows.filter((row) => METAL_FUTURES_SYMBOLS.has(row.symbol))
  }
  return rows.filter((row) => row.category === categoryToQueryParam[category])
}

/** Cached slice of the wire catalog after category + search filters (prices + FX mids). */
export type MarketCatalogSnapshot = {
  filteredRows: CatalogRow[]
  fxResponseItems: FxRateApiItem[]
}

type TrendMeta = {
  trendScore?: number
  trendLabel?: 'WEAK' | 'NEUTRAL' | 'STRONG' | 'VERY_STRONG'
  trendPercentile?: number
  trendRelativeWeekly?: number
}

type CatalogRowWithPeriods = CatalogRow & {
  change1D: number
  change1M: number
  change3M: number
  change6M: number
  change1Y: number
} & TrendMeta

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}

function median(values: number[]): number {
  if (values.length === 0) return 0
  const sorted = [...values].sort((a, b) => a - b)
  const mid = Math.floor(sorted.length / 2)
  return sorted.length % 2 === 0 ? (sorted[mid - 1] + sorted[mid]) / 2 : sorted[mid]
}

function stddev(values: number[], mean: number): number {
  if (values.length === 0) return 0
  const variance = values.reduce((acc, value) => acc + (value - mean) ** 2, 0) / values.length
  return Math.sqrt(variance)
}

function weeklySignal(row: { change1M?: number | null; change1D?: number | null; change24h?: number | null }): number {
  // Prefer 1M-based weekly estimate for consistency across instruments; fallback to 1D.
  if (row.change1M != null && Number.isFinite(row.change1M)) {
    return row.change1M / 4
  }
  return row.change1D ?? row.change24h ?? 0
}

function labelForTrendScore(score: number): 'WEAK' | 'NEUTRAL' | 'STRONG' | 'VERY_STRONG' {
  if (score < 35) return 'WEAK'
  if (score < 65) return 'NEUTRAL'
  if (score < 85) return 'STRONG'
  return 'VERY_STRONG'
}

function enrichContextualTrendScores<T extends CatalogRow & Partial<CatalogRowWithPeriods>>(rows: T[]): (T & TrendMeta)[] {
  if (rows.length === 0) return rows
  const signals = rows.map((row) => weeklySignal(row))
  const med = median(signals)
  const mean = signals.reduce((acc, value) => acc + value, 0) / signals.length
  const sigma = Math.max(stddev(signals, mean), 0.0001)
  const dispersion = Math.max(
    median(signals.map((value) => Math.abs(value - med))),
    0.0001,
  )
  const breadth = signals.filter((value) => value > 0).length / signals.length
  const sortedSignals = [...signals].sort((a, b) => a - b)
  return rows.map((row) => {
    const signal = weeklySignal(row)
    const belowOrEqual = sortedSignals.filter((value) => value <= signal).length
    const percentile = (belowOrEqual / sortedSignals.length) * 100
    const relativeWeekly = signal - med
    const z = (signal - mean) / sigma
    const percentileScore = percentile
    const relativeScore = clamp(50 + (relativeWeekly / dispersion) * 20, 0, 100)
    const anomalyScore = clamp(50 + z * 10, 0, 100)
    const breadthAdjust = signal >= 0 ? (0.5 - breadth) * 15 : -(0.5 - breadth) * 15
    const trendScore = clamp(
      percentileScore * 0.5 + relativeScore * 0.3 + anomalyScore * 0.2 + breadthAdjust,
      0,
      100,
    )
    return {
      ...row,
      trendScore,
      trendLabel: labelForTrendScore(trendScore),
      trendPercentile: percentile,
      trendRelativeWeekly: relativeWeekly,
    }
  })
}

/**
 * Loads 1D–1Y metrics for every visible catalog row (one summary batch + parallel FX history).
 * Required before sorting by any period column so ordering matches displayed values.
 */
async function enrichCatalogRowsWithPeriodMetrics(rows: CatalogRow[]): Promise<{
  rows: CatalogRowWithPeriods[]
  summaryBySymbol: Record<string, PeriodChanges & { price: number }>
  fxPeriodBySymbol: Record<string, PeriodChanges>
}> {
  const nonFx = rows.filter((r) => r.category !== 'FX')
  const fxRows = rows.filter((r) => r.category === 'FX')
  let summaryBySymbol: Record<string, PeriodChanges & { price: number }> = {}
  try {
    summaryBySymbol = nonFx.length > 0 ? await fetchPricesSummary(nonFx.map((r) => r.symbol)) : {}
  } catch {
    summaryBySymbol = {}
  }
  const fxPeriodBySymbol: Record<string, PeriodChanges> = {}
  await Promise.all(
    fxRows.map(async (row) => {
      try {
        const history = await fetchFxHistory(row.symbol, 365)
        fxPeriodBySymbol[row.symbol] = computePeriodChanges(history)
      } catch {
        fxPeriodBySymbol[row.symbol] = { change1D: 0, change1M: 0, change3M: 0, change6M: 0, change1Y: 0 }
      }
    }),
  )
  const merged: CatalogRowWithPeriods[] = rows.map((row) => {
    if (row.category === 'FX') {
      const s = fxPeriodBySymbol[row.symbol] ?? { change1D: 0, change1M: 0, change3M: 0, change6M: 0, change1Y: 0 }
      return {
        ...row,
        change24h: s.change1D,
        change1D: s.change1D,
        change1M: s.change1M,
        change3M: s.change3M,
        change6M: s.change6M,
        change1Y: s.change1Y,
      }
    }
    const s = summaryBySymbol[row.symbol]
    const change1D = s?.change1D ?? row.change24h ?? 0
    return {
      ...row,
      change24h: change1D,
      change1D,
      change1M: s?.change1M ?? 0,
      change3M: s?.change3M ?? 0,
      change6M: s?.change6M ?? 0,
      change1Y: s?.change1Y ?? 0,
    }
  })
  return { rows: merged, summaryBySymbol, fxPeriodBySymbol }
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

/** Fetches `/api/market/prices` + `/api/market/fx` and applies the same filters as the overview table. */
export async function fetchMarketCatalogSnapshot(params: {
  category?: MarketCategory
  query?: string
}): Promise<MarketCatalogSnapshot> {
  const category = params.category
  const query = params.query?.trim()
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

  const [instrumentMetadataBySymbol, normalizedRows] = await Promise.all([
    fetchInstrumentMetadataBySymbolMap(),
    Promise.resolve([
    ...normalizeMarketRows(responseItems),
    ...normalizeFxRows(fxResponseItems),
    ]),
  ])
  const normalizedRowsWithInstrumentId: CatalogRow[] = normalizedRows.map((row) => ({
    ...row,
    name: instrumentMetadataBySymbol[row.symbol]?.name ?? row.name,
    instrumentId: instrumentMetadataBySymbol[row.symbol]?.id ?? null,
  }))
  const filteredByCategory = filterRowsByCategory(normalizedRowsWithInstrumentId, category)
  const filteredBySearch = query
    ? filteredByCategory.filter((row) => row.symbol.includes(query.toUpperCase()))
    : filteredByCategory

  return { filteredRows: filteredBySearch, fxResponseItems }
}

/** Pure follow-up on an in-memory catalog: sort, optional period prefetch, pagination, row shaping. */
export async function buildMarketOverviewFromCatalog(
  snapshot: MarketCatalogSnapshot,
  params: FetchMarketsParams,
): Promise<MarketOverviewPageResponse> {
  const sort = params.sort?.trim()
  const displayCurrency: SupportedCurrency = params.displayCurrency ?? 'USD'
  const fxResponseItems = snapshot.fxResponseItems

  const { field: sortMetricField } = parseMarketSortQuery(sort)
  let summaryBySymbol: Record<string, PeriodChanges & { price: number }> = {}
  let fxPeriodBySymbol: Record<string, PeriodChanges> = {}
  let rowsForSort: Array<CatalogRow | CatalogRowWithPeriods> = [...snapshot.filteredRows]

  if (sortRequiresPeriodPrefetch(sortMetricField)) {
    const loaded = await enrichCatalogRowsWithPeriodMetrics(rowsForSort as CatalogRow[])
    rowsForSort = loaded.rows
    summaryBySymbol = loaded.summaryBySymbol
    fxPeriodBySymbol = loaded.fxPeriodBySymbol
  }

  if (sortMetricField === 'displayAmount') {
    const hub = buildFxTryHub(fxResponseItems)
    rowsForSort = rowsForSort.map((r) => {
      const nq = inferNativeQuote(r.symbol, r.category ?? null)
      const amt =
        hub != null ? convertToDisplayCurrency(r.price ?? 0, nq, displayCurrency, hub) : null
      return { ...r, [SORT_DISPLAY_AMOUNT_KEY]: amt }
    })
  }

  rowsForSort = enrichContextualTrendScores(rowsForSort)

  const sorted = sortMarketOverviewRows(rowsForSort, sort)

  const safePage = Math.max(params.page, 0)
  const safeSize = Math.max(params.size, 1)
  const start = safePage * safeSize
  const end = start + safeSize
  const basePageRows = sorted.slice(start, end)

  if (sortRequiresPageSummaryFetch(sortMetricField)) {
    const summarySymbols = basePageRows.filter((row) => row.category !== 'FX').map((row) => row.symbol)
    try {
      summaryBySymbol = summarySymbols.length > 0 ? await fetchPricesSummary(summarySymbols) : {}
    } catch {
      summaryBySymbol = {}
    }
    const fxSymbols = basePageRows.filter((row) => row.category === 'FX').map((row) => row.symbol)
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
  }

  const fxHub = buildFxTryHub(fxResponseItems)

  const content = basePageRows.map((rawRow) => {
    const { [SORT_DISPLAY_AMOUNT_KEY]: _sortMetric, ...row } = rawRow as typeof rawRow &
      Record<string, number | null | undefined>
    const trendAwareRow = row as CatalogRowWithPeriods & TrendMeta
    const summary =
      row.category === 'FX'
        ? fxPeriodBySymbol[row.symbol] ?? { change1D: 0, change1M: 0, change3M: 0, change6M: 0, change1Y: 0 }
        : summaryBySymbol[row.symbol] ?? {}
    const nativeQuote = inferNativeQuote(row.symbol, row.category ?? null)
    const displayAmount =
      fxHub != null ? convertToDisplayCurrency(row.price ?? 0, nativeQuote, displayCurrency, fxHub) : null
    return {
      ...row,
      symbol: row.symbol,
      price: row.price ?? 0,
      nativeQuote,
      displayAmount,
      timestamp: row.timestamp ?? null,
      freshness: row.freshness ?? 'STALE',
      change24h: summary.change1D ?? 0,
      change1D: summary.change1D ?? 0,
      change1M: summary.change1M ?? 0,
      change3M: summary.change3M ?? 0,
      change6M: summary.change6M ?? 0,
      change1Y: summary.change1Y ?? 0,
      trendScore: trendAwareRow.trendScore ?? null,
      trendLabel: trendAwareRow.trendLabel ?? null,
      trendPercentile: trendAwareRow.trendPercentile ?? null,
      trendRelativeWeekly: trendAwareRow.trendRelativeWeekly ?? null,
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

export async function fetchMarketOverview(params: FetchMarketsParams): Promise<MarketOverviewPageResponse> {
  const snapshot = await fetchMarketCatalogSnapshot({
    category: params.category,
    query: params.query,
  })
  return buildMarketOverviewFromCatalog(snapshot, params)
}

export async function fetchMarketInsights(): Promise<MarketInsightsResponse> {
  const snapshot = await fetchMarketCatalogSnapshot({
    category: 'all',
    query: '',
  })
  const [gainersPage, losersPage] = await Promise.all([
    buildMarketOverviewFromCatalog(snapshot, {
      page: 0,
      size: 5,
      category: 'all',
      query: '',
      sort: 'change1D,desc',
      displayCurrency: 'USD',
    }),
    buildMarketOverviewFromCatalog(snapshot, {
      page: 0,
      size: 5,
      category: 'all',
      query: '',
      sort: 'change1D,asc',
      displayCurrency: 'USD',
    }),
  ])
  return {
    topGainers: gainersPage.content,
    topLosers: losersPage.content,
  }
}

export async function fetchInstrumentFundamentals(symbol: string, forceRefresh = false): Promise<InstrumentFundamentals> {
  const response = await apiClient.get<InstrumentFundamentals>(`/api/market/instruments/${symbol}/fundamentals`, {
    params: { forceRefresh },
  })
  return response.data
}
