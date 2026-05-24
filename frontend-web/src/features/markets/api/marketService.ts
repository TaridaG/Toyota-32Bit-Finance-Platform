import { apiClient } from '../../../shared/api/client'
import type {
  InstrumentFundamentals,
  MarketCategory,
  MarketInsightsResponse,
  MarketOverviewItem,
  MarketOverviewPageResponse,
} from '../../../shared/types/market'
import type { SupportedCurrency } from '../../../shared/preferences/preferences'
import {
  buildFxTryHub,
  convertToDisplayCurrency,
  inferNativeQuote,
} from '../lib/marketDisplayConversion'
import { normalizeFxQuotePrice } from '../lib/fxTryHubConversion'
import {
  parseMarketSortQuery,
  SORT_DISPLAY_AMOUNT_KEY,
  sortMarketOverviewRows,
  sortRequiresPageSummaryFetch,
  sortRequiresPeriodPrefetch,
  type MarketOverviewSortOptions,
} from '../lib/marketSort'

/** Overview enriches full segment server-side; allow longer than default API timeout. */
const MARKET_OVERVIEW_TIMEOUT_MS = 60_000

type FetchMarketsParams = {
  page: number
  size: number
  category?: MarketCategory
  query?: string
  sort?: string
  /** Header currency: drives `displayAmount` TRY-bridge conversion on each row. */
  displayCurrency?: SupportedCurrency
  signal?: AbortSignal
}

const categoryToQueryParam: Record<Exclude<MarketCategory, 'all'>, string> = {
  crypto: 'CRYPTO',
  bist: 'STOCK',
  nasdaq: 'STOCK',
  forex: 'FX',
  metals: 'FX',
  funds: 'FUND',
  bonds: 'BOND',
  eurobond: 'BOND',
}

type MarketPriceApiItem = {
  symbol?: string
  price?: number | string
  value?: number | string
  source?: string
  category?: string | null
  instrumentType?: string | null
  /** Wire may send ISO-8601 string or epoch seconds/ms as number (Jackson / proxies). */
  timestamp?: string | number | null
  change24h?: number | string | null
  high24h?: number | string | null
  low24h?: number | string | null
}

type FxRateApiItem = {
  symbol?: string
  bid?: number | string
  ask?: number | string
  mid?: number | string
  timestamp?: string | number | null
}

type InstrumentCatalogItem = {
  id?: number | string
  symbol?: string
  name?: string
  exchange?: string | null
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

/**
 * US-listed ETF tickers shown under the "funds" strip; their time series is ingested as **equity** prices
 * (`mds_market_price_history`), not TEFAS NAV (`mds_fund_nav_history`). Analysis must read price history for these.
 */
export const US_LISTED_ETF_TICKERS_AS_FUNDS = new Set(['VOO', 'VTI', 'QQQ', 'IVV', 'SPY'])
const SPOT_METAL_SYMBOLS = new Set(['XAUTRY', 'XAGTRY', 'XPTTRY', 'XPDTRY', 'XCUTRY'])
const METAL_SYMBOLS = SPOT_METAL_SYMBOLS
type InstrumentMetadata = {
  id: number
  name: string | null
  /** From finance-api instrument catalog (e.g. BIST, NASDAQ). */
  exchange: string | null
}

let instrumentMetadataBySymbolCache: Record<string, InstrumentMetadata> | null = null
let instrumentMetadataBySymbolPromise: Promise<Record<string, InstrumentMetadata>> | null = null

/** Wire symbols (e.g. Yahoo BIST `GARAN.IS`) vs catalog keys (`GARAN`). */
function resolveInstrumentMeta(
  symbol: string,
  bySymbol: Record<string, InstrumentMetadata>,
): InstrumentMetadata | null {
  const up = symbol.trim().toUpperCase()
  const direct = bySymbol[up]
  if (direct) return direct
  if (up.endsWith('.IS')) {
    return bySymbol[up.slice(0, -3)] ?? null
  }
  return null
}

function toCategory(symbol: string, item?: MarketPriceApiItem): string {
  const symUp = symbol.trim().toUpperCase()
  if (symUp.startsWith('FUND_')) {
    return 'FUND'
  }
  if (symUp.startsWith('TRBOND')) {
    return 'BOND'
  }
  const hinted =
    (item?.instrumentType ?? item?.category ?? '')
      .toString()
      .trim()
      .toUpperCase()
  if (hinted === 'FUND' || hinted === 'BOND' || hinted === 'FX' || hinted === 'CRYPTO' || hinted === 'STOCK') {
    return hinted
  }
  if (METAL_SYMBOLS.has(symbol)) return 'METAL'
  if (US_LISTED_ETF_TICKERS_AS_FUNDS.has(symbol)) return 'FUND'
  if (symbol.endsWith('USDT') || symbol.endsWith('USD')) return 'CRYPTO'
  if (symbol.endsWith('TRY') || symbol.includes('/')) return 'FX'
  return 'STOCK'
}

function toNumber(value: unknown, fallback: number): number {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : fallback
}

/** Parse backend {@link Instant} or epoch values to epoch milliseconds, or null if unusable. */
function parseWireInstantMs(value: string | number | null | undefined): number | null {
  if (value == null) {
    return null
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    const n = value
    return n < 1e12 ? Math.round(n * 1000) : Math.round(n)
  }
  const s = String(value).trim()
  if (!s) {
    return null
  }
  const parsed = Date.parse(s)
  return Number.isFinite(parsed) ? parsed : null
}

/**
 * "Delayed" badge: exchange-quoted assets often carry last-trade time (15m delayed tape, overnight close, etc.).
 * A 5-minute wall-clock rule falsely marks almost everything delayed. TEFAS NAV is daily — use a multi-day window.
 */
function maxFreshAgeMsForSymbol(symbol: string): number {
  const sym = symbol.trim().toUpperCase()
  if (sym.startsWith('FUND_') || sym.startsWith('TRBOND')) {
    return 5 * 24 * 60 * 60 * 1000
  }
  if (sym.endsWith('USDT') || sym.endsWith('USD') || sym.endsWith('TRY') || sym.includes('/')) {
    return 30 * 60 * 1000
  }
  return 45 * 60 * 1000
}

function toFreshness(timestamp: string | number | null | undefined, symbol: string): 'LIVE' | 'STALE' {
  const ts = parseWireInstantMs(timestamp)
  if (ts == null) {
    return 'STALE'
  }
  return Date.now() - ts > maxFreshAgeMsForSymbol(symbol) ? 'STALE' : 'LIVE'
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
        timestamp: item.timestamp != null ? String(item.timestamp) : null,
        freshness: toFreshness(item.timestamp, symbol),
        change24h: toNumber(item.change24h ?? 0, 0),
        high24h: toNumber(item.high24h ?? item.price ?? item.value ?? 0, price),
        low24h: toNumber(item.low24h ?? item.price ?? item.value ?? 0, price),
        category: toCategory(symbol, item),
        instrumentId: null,
        listedExchange: null,
      }
    })
}

function normalizeFxRows(items: FxRateApiItem[]) {
  return items
    .filter((item) => typeof item?.symbol === 'string' && item.symbol.trim().length > 0)
    .map((item) => {
      const symbol = String(item.symbol).trim().toUpperCase()
      const price = normalizeFxQuotePrice(symbol, toNumber(item.mid ?? item.ask ?? item.bid ?? 0, 0))
      return {
        symbol,
        name: symbol,
        price,
        source: 'TCMB',
        timestamp: item.timestamp != null ? String(item.timestamp) : null,
        freshness: toFreshness(item.timestamp, symbol),
        change24h: 0,
        high24h: price,
        low24h: price,
        category: 'FX',
        instrumentId: null,
        listedExchange: null,
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
        const exchangeRaw = item?.exchange
        const exchange =
          typeof exchangeRaw === 'string' && exchangeRaw.trim().length > 0
            ? exchangeRaw.trim().toUpperCase()
            : null
        if (symbol && Number.isFinite(id)) {
          map[symbol] = { id, name, exchange }
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
  /** Resolved from instrument catalog when wire symbol matches (incl. `*.IS` → base). */
  listedExchange: string | null
}

function filterRowsByCategory(rows: CatalogRow[], category?: MarketCategory): CatalogRow[] {
  if (!category || category === 'all') {
    return rows
  }
  if (category === 'bist') {
    return rows.filter((row) => {
      if (row.category !== 'STOCK') return false
      const sym = row.symbol.trim().toUpperCase()
      return (
        row.source === 'YAHOO' ||
        row.listedExchange === 'BIST' ||
        sym.endsWith('.IS')
      )
    })
  }
  if (category === 'nasdaq') {
    return rows.filter(
      (row) =>
        row.category === 'STOCK' &&
        (row.source === 'FINNHUB' ||
          row.listedExchange === 'NASDAQ' ||
          row.listedExchange === 'FINNHUB'),
    )
  }
  if (category === 'forex') {
    return rows.filter((row) => row.category === 'FX' && !METAL_SYMBOLS.has(row.symbol))
  }
  if (category === 'metals') {
    return rows.filter((row) => SPOT_METAL_SYMBOLS.has(row.symbol))
  }
  if (category === 'bonds') {
    return rows.filter((row) => {
      const sym = row.symbol.trim().toUpperCase()
      return row.category === 'BOND' || sym.startsWith('TRBOND')
    })
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
 * FX pairs and spot metals in TRY use `/api/market/fx/history` (not price history).
 * Futures/metals priced as equities still use price history.
 */
export function symbolUsesFxMarketHistory(symbol: string, wireCategory: string | null | undefined): boolean {
  const sym = symbol.trim().toUpperCase()
  if (SPOT_METAL_SYMBOLS.has(sym)) {
    return true
  }
  return (wireCategory ?? '').trim().toUpperCase() === 'FX'
}

function rowUsesFxHistory(row: CatalogRow): boolean {
  return symbolUsesFxMarketHistory(row.symbol, row.category)
}

export function symbolUsesFundMarketHistory(wireCategory: string | null | undefined): boolean {
  return (wireCategory ?? '').trim().toUpperCase() === 'FUND'
}

async function enrichCatalogRowsWithPeriodMetrics(rows: CatalogRow[]): Promise<{
  rows: CatalogRowWithPeriods[]
  summaryBySymbol: Record<string, PeriodChanges & { price: number }>
  fxPeriodBySymbol: Record<string, PeriodChanges>
}> {
  const summaryRows = rows.filter((r) => !rowUsesFxHistory(r))
  const fxRows = rows.filter((r) => rowUsesFxHistory(r))
  let summaryBySymbol: Record<string, PeriodChanges & { price: number }> = {}
  try {
    summaryBySymbol = summaryRows.length > 0 ? await fetchMarketPricesSummary(summaryRows.map((r) => r.symbol)) : {}
  } catch {
    summaryBySymbol = {}
  }
  const fxPeriodBySymbol: Record<string, PeriodChanges> = {}
  await Promise.all(
    fxRows.map(async (row) => {
      try {
        const history = await fetchFxHistory(row.symbol, 365)
        fxPeriodBySymbol[row.symbol] = computePeriodChanges(history, row.symbol)
      } catch {
        fxPeriodBySymbol[row.symbol] = { change1D: 0, change1M: 0, change3M: 0, change6M: 0, change1Y: 0 }
      }
    }),
  )
  const merged: CatalogRowWithPeriods[] = rows.map((row) => {
    if (rowUsesFxHistory(row)) {
      let s = fxPeriodBySymbol[row.symbol] ?? { change1D: 0, change1M: 0, change3M: 0, change6M: 0, change1Y: 0 }
      if (SPOT_METAL_SYMBOLS.has(row.symbol)) {
        const fromSummary = summaryBySymbol[row.symbol]
        if (fromSummary) {
          const sumP = fromSummary.change1D ** 2 + fromSummary.change1M ** 2 + fromSummary.change1Y ** 2
          const fxP = s.change1D ** 2 + s.change1M ** 2 + s.change1Y ** 2
          if (sumP > fxP) {
            const { price: _p, ...rest } = fromSummary
            s = rest
          }
        }
      }
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

function parseHistory(points: HistoryPoint[], symbol?: string): ParsedHistoryPoint[] {
  const sym = symbol?.trim().toUpperCase()
  return points
    .map((p) => ({
      time: Date.parse(String(p.time ?? '')),
      value: sym ? normalizeFxQuotePrice(sym, toNumber(p.value, NaN)) : toNumber(p.value, NaN),
    }))
    .filter((p) => Number.isFinite(p.time) && Number.isFinite(p.value))
    .sort((a, b) => a.time - b.time)
}

function computePeriodChanges(points: HistoryPoint[], symbol?: string): PeriodChanges {
  const parsed = parseHistory(points, symbol)
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

const SUMMARY_REQUEST_CHUNK = 40

export async function fetchMarketPricesSummary(
  symbols: string[],
): Promise<Record<string, PeriodChanges & { price: number }>> {
  if (symbols.length === 0) {
    return {}
  }
  const out: Record<string, PeriodChanges & { price: number }> = {}
  for (let i = 0; i < symbols.length; i += SUMMARY_REQUEST_CHUNK) {
    const chunk = symbols.slice(i, i + SUMMARY_REQUEST_CHUNK)
    try {
      const response = await apiClient.get<Record<string, SummaryItem>>('/api/market/prices/summary', {
        // Encode '=' in symbols (e.g. BRK.B) — raw query strings split on '=' otherwise.
        params: { symbols: chunk.join(',') },
        paramsSerializer: (params) => {
          const search = new URLSearchParams()
          for (const [key, value] of Object.entries(params)) {
            if (value != null && value !== '') {
              search.set(key, String(value))
            }
          }
          return search.toString()
        },
      })
      const data = response.data ?? {}
      for (const symbol of chunk) {
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
    } catch {
      /* skip chunk — avoid one bad batch failing the whole markets table */
    }
  }
  return out
}

type MarketOverviewWirePage = {
  content?: OverviewWireRow[]
  page?: number
  size?: number
  totalElements?: number
  totalPages?: number
}

type OverviewWireRow = {
  symbol: string
  name?: string
  nativePrice?: number | string | null
  price?: number | string | null
  change24h?: number | string | null
  change1D?: number | string | null
  change1M?: number | string | null
  change3M?: number | string | null
  change6M?: number | string | null
  change1Y?: number | string | null
  trendScore?: number | string | null
  trendLabel?: string | null
  high24h?: number | string | null
  low24h?: number | string | null
  category?: string | null
  instrumentId?: number | string | null
  volume24h?: number | string | null
  openInterest?: number | string | null
  dayOpen?: number | string | null
  dayHigh?: number | string | null
  dayLow?: number | string | null
  exchangeName?: string | null
  underlyingSymbol?: string | null
  contractExpiry?: string | null
  linkedSpotSymbol?: string | null
  spotSpreadPct?: number | string | null
  spotSpreadAbs?: number | string | null
}

function toNullableNumber(value: unknown): number | null {
  if (value == null || value === '') {
    return null
  }
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

function mapTrendLabel(raw: string | null | undefined): MarketOverviewItem['trendLabel'] {
  const u = (raw ?? '').trim().toUpperCase()
  if (u === 'WEAK') return 'WEAK'
  if (u === 'STRONG') return 'STRONG'
  if (u === 'VERY_STRONG') return 'VERY_STRONG'
  if (u === 'NEUTRAL') return 'NEUTRAL'
  return null
}

function mapOverviewWireItem(row: OverviewWireRow): MarketOverviewItem {
  const hasNative = row.nativePrice != null && row.nativePrice !== ''
  const rawNative = hasNative ? toNumber(row.nativePrice, 0) : toNumber(row.price ?? 0, 0)
  const nativeNum = normalizeFxQuotePrice(row.symbol, rawNative)
  const rawDisplay =
    hasNative && row.price != null && row.price !== '' ? toNumber(row.price, 0) : null
  const displayNum = rawDisplay != null ? normalizeFxQuotePrice(row.symbol, rawDisplay) : null
  const cat = (row.category ?? '').trim().toUpperCase() || null
  const idRaw = row.instrumentId
  let instrumentId: number | null = null
  if (idRaw != null && idRaw !== '') {
    const n = Number(idRaw)
    instrumentId = Number.isFinite(n) ? n : null
  }
  return {
    symbol: row.symbol,
    name: row.name ?? row.symbol,
    price: nativeNum,
    nativeQuote: inferNativeQuote(row.symbol, cat, undefined, null),
    displayAmount: displayNum,
    timestamp: null,
    freshness: 'LIVE',
    change24h: toNullableNumber(row.change24h),
    change1D: toNullableNumber(row.change1D),
    change1M: toNullableNumber(row.change1M),
    change3M: toNullableNumber(row.change3M),
    change6M: toNullableNumber(row.change6M),
    change1Y: toNullableNumber(row.change1Y),
    trendScore: toNullableNumber(row.trendScore),
    trendLabel: mapTrendLabel(row.trendLabel),
    trendPercentile: null,
    trendRelativeWeekly: null,
    high24h: toNullableNumber(row.high24h),
    low24h: toNullableNumber(row.low24h),
    category: row.category ?? null,
    exchange: row.exchangeName ?? null,
    instrumentId,
    volume24h: toNullableNumber(row.volume24h),
    openInterest: toNullableNumber(row.openInterest),
    dayOpen: toNullableNumber(row.dayOpen),
    dayHigh: toNullableNumber(row.dayHigh),
    dayLow: toNullableNumber(row.dayLow),
    exchangeName: row.exchangeName ?? null,
    underlyingSymbol: row.underlyingSymbol ?? null,
    contractExpiry: row.contractExpiry ?? null,
    linkedSpotSymbol: row.linkedSpotSymbol ?? null,
    spotSpreadPct: toNullableNumber(row.spotSpreadPct),
    spotSpreadAbs: toNullableNumber(row.spotSpreadAbs),
  }
}

const overviewCategoryQuery: Record<MarketCategory, string> = {
  all: 'ALL',
  crypto: 'CRYPTO',
  bist: 'BIST',
  nasdaq: 'NASDAQ',
  forex: 'FOREX',
  metals: 'METALS',
  funds: 'FUNDS',
  bonds: 'BONDS',
  eurobond: 'BONDS',
}

/** Paginated markets table backed by finance-api {@code GET /api/market/overview} (server-side segment + sort + pagination). */
export async function fetchMarketOverviewPage(params: FetchMarketsParams): Promise<MarketOverviewPageResponse> {
  const page = Math.max(params.page ?? 0, 0)
  const size = Math.max(params.size ?? 10, 1)
  const category = params.category ?? 'all'
  const requestParams: Record<string, string | number> = {
    page,
    size,
    category: overviewCategoryQuery[category],
  }
  const q = params.query?.trim()
  if (q) {
    requestParams.q = q
  }
  const sort = params.sort?.trim()
  if (sort) {
    requestParams.sort = sort
  }
  const { data: root } = await apiClient.get<{ success?: boolean; data?: MarketOverviewWirePage }>(
    '/api/market/overview',
    {
      params: requestParams,
      headers: params.displayCurrency ? { 'X-Currency': params.displayCurrency } : undefined,
      timeout: MARKET_OVERVIEW_TIMEOUT_MS,
      signal: params.signal,
    },
  )
  const body = root?.data
  if (!body || !Array.isArray(body.content)) {
    return { content: [], page, size, totalElements: 0, totalPages: 0 }
  }
  return {
    content: body.content.map(mapOverviewWireItem),
    page: body.page ?? page,
    size: body.size ?? size,
    totalElements: body.totalElements ?? 0,
    totalPages: body.totalPages ?? 0,
  }
}

/** Fetches `/api/market/prices` + `/api/market/fx` and applies the same filters as the overview table. */
const catalogSnapshotInflight = new Map<string, Promise<MarketCatalogSnapshot>>()

function catalogSnapshotKey(category?: MarketCategory, query?: string): string {
  return `${category ?? 'all'}|${query?.trim().toLowerCase() ?? ''}`
}

export async function fetchMarketCatalogSnapshot(params: {
  category?: MarketCategory
  query?: string
}): Promise<MarketCatalogSnapshot> {
  const key = catalogSnapshotKey(params.category, params.query)
  const inflight = catalogSnapshotInflight.get(key)
  if (inflight) {
    return inflight
  }
  const promise = fetchMarketCatalogSnapshotImpl(params).finally(() => {
    catalogSnapshotInflight.delete(key)
  })
  catalogSnapshotInflight.set(key, promise)
  return promise
}

async function fetchMarketCatalogSnapshotImpl(params: {
  category?: MarketCategory
  query?: string
}): Promise<MarketCatalogSnapshot> {
  const category = params.category
  const query = params.query?.trim()

  let responseItems: MarketPriceApiItem[] = []
  try {
    const response = await apiClient.get<MarketPriceApiItem[] | { data?: MarketPriceApiItem[] }>('/api/market/prices')
    responseItems = Array.isArray(response.data)
      ? response.data
      : Array.isArray(response.data?.data)
        ? response.data.data
        : []
  } catch {
    responseItems = []
  }

  let fxResponseItems: FxRateApiItem[] = []
  try {
    const fxResponse = await apiClient.get<FxRateApiItem[] | { data?: FxRateApiItem[] }>('/api/market/fx')
    fxResponseItems = Array.isArray(fxResponse.data)
      ? fxResponse.data
      : Array.isArray(fxResponse.data?.data)
        ? fxResponse.data.data
        : []
  } catch {
    fxResponseItems = []
  }

  const [instrumentMetadataBySymbol, normalizedRows] = await Promise.all([
    fetchInstrumentMetadataBySymbolMap(),
    Promise.resolve([
    ...normalizeMarketRows(responseItems),
    ...normalizeFxRows(fxResponseItems),
    ]),
  ])
  const normalizedRowsWithInstrumentId: CatalogRow[] = normalizedRows.map((row) => {
    const meta = resolveInstrumentMeta(row.symbol, instrumentMetadataBySymbol)
    return {
      ...row,
      name: meta?.name ?? row.name,
      instrumentId: meta?.id ?? null,
      listedExchange: meta?.exchange ?? null,
    }
  })
  const filteredByCategory = filterRowsByCategory(normalizedRowsWithInstrumentId, category)
  const filteredBySearch = query
    ? filteredByCategory.filter((row) => row.symbol.includes(query.toUpperCase()))
    : filteredByCategory

  return { filteredRows: filteredBySearch, fxResponseItems }
}

function inferPriceSourceFromExchange(exchange: string | null): string | null {
  if (!exchange) {
    return null
  }
  const ex = exchange.trim().toUpperCase()
  if (ex === 'BIST' || ex.includes('ISTANBUL')) {
    return 'YAHOO'
  }
  if (ex === 'NASDAQ' || ex === 'NYSE' || ex === 'FINNHUB') {
    return 'FINNHUB'
  }
  return null
}

function catalogRowFromInstrument(symbol: string, meta: InstrumentMetadata): CatalogRow {
  const sym = symbol.trim().toUpperCase()
  return {
    symbol: sym,
    name: meta.name ?? sym,
    price: 0,
    source: inferPriceSourceFromExchange(meta.exchange),
    timestamp: null,
    freshness: 'STALE',
    change24h: 0,
    high24h: 0,
    low24h: 0,
    category: toCategory(sym),
    instrumentId: meta.id,
    listedExchange: meta.exchange,
  }
}

/**
 * Analysis instrument picker: merges `/api/instruments` with live `/api/market/prices`
 * so symbols without a recent tick still appear (aligned with Piyasalar segment filters).
 */
export async function fetchAnalysisInstrumentCatalog(params: {
  category?: MarketCategory
  query?: string
}): Promise<MarketCatalogSnapshot> {
  const snapshot = await fetchMarketCatalogSnapshot(params)
  const metaMap = await fetchInstrumentMetadataBySymbolMap()
  const seen = new Set(snapshot.filteredRows.map((row) => normalizeCatalogSymbol(row.symbol)))
  const extras: CatalogRow[] = []
  for (const [rawSymbol, meta] of Object.entries(metaMap)) {
    const norm = normalizeCatalogSymbol(rawSymbol)
    if (!norm || seen.has(norm)) {
      continue
    }
    seen.add(norm)
    extras.push(catalogRowFromInstrument(rawSymbol, meta))
  }
  if (extras.length === 0) {
    return snapshot
  }
  const merged = filterRowsByCategory([...snapshot.filteredRows, ...extras], params.category)
  const filteredBySearch = params.query?.trim()
    ? merged.filter((row) => row.symbol.includes(params.query!.trim().toUpperCase()))
    : merged
  return { filteredRows: filteredBySearch, fxResponseItems: snapshot.fxResponseItems }
}

export type CategoryPerformanceRow = {
  symbol: string
  name: string
  weeklyPct: number
  monthlyPct: number
  yearlyPct: number
}

function weeklyPctFromPeriodRow(row: CatalogRowWithPeriods): number {
  if (row.change1M != null && Number.isFinite(row.change1M)) {
    return row.change1M / 4
  }
  return row.change1D ?? row.change24h ?? 0
}

/** Loads period % changes for all instruments in an analysis segment (BIST, NASDAQ, …). */
export async function fetchCategoryPerformanceRows(category: MarketCategory): Promise<CategoryPerformanceRow[]> {
  const snapshot = await fetchAnalysisInstrumentCatalog({
    category: category === 'all' ? undefined : category,
  })
  const { rows } = await enrichCatalogRowsWithPeriodMetrics(snapshot.filteredRows)
  return rows
    .filter((row) => (row.price ?? 0) > 0)
    .map((row) => ({
      symbol: row.symbol.trim().toUpperCase(),
      name: row.name?.trim() || row.symbol,
      weeklyPct: weeklyPctFromPeriodRow(row),
      monthlyPct: row.change1M ?? 0,
      yearlyPct: row.change1Y ?? 0,
    }))
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
      const nq = inferNativeQuote(r.symbol, r.category ?? null, r.source, r.listedExchange)
      const amt =
        hub != null ? convertToDisplayCurrency(r.price ?? 0, nq, displayCurrency, hub) : null
      return { ...r, [SORT_DISPLAY_AMOUNT_KEY]: amt }
    })
  }

  rowsForSort = enrichContextualTrendScores(rowsForSort)

  const sortOptions: MarketOverviewSortOptions | undefined =
    params.category === 'funds' ? { groupCanonicalFundsFirst: true } : undefined
  const sorted = sortMarketOverviewRows(rowsForSort, sort, sortOptions)

  const safePage = Math.max(params.page, 0)
  const safeSize = Math.max(params.size, 1)
  const start = safePage * safeSize
  const end = start + safeSize
  const basePageRows = sorted.slice(start, end)

  if (sortRequiresPageSummaryFetch(sortMetricField)) {
    const summarySymbols = basePageRows.filter((row) => !rowUsesFxHistory(row)).map((row) => row.symbol)
    try {
      summaryBySymbol = summarySymbols.length > 0 ? await fetchMarketPricesSummary(summarySymbols) : {}
    } catch {
      summaryBySymbol = {}
    }
    const fxSymbols = basePageRows.filter((row) => rowUsesFxHistory(row)).map((row) => row.symbol)
    await Promise.all(
      fxSymbols.map(async (symbol) => {
        try {
          const history = await fetchFxHistory(symbol, 365)
          fxPeriodBySymbol[symbol] = computePeriodChanges(history, symbol)
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
    const summary = rowUsesFxHistory(row as CatalogRow)
      ? fxPeriodBySymbol[row.symbol] ?? { change1D: 0, change1M: 0, change3M: 0, change6M: 0, change1Y: 0 }
      : summaryBySymbol[row.symbol] ?? {}
    const nativeQuote = inferNativeQuote(row.symbol, row.category ?? null, row.source, row.listedExchange)
    const displayAmount =
      fxHub != null ? convertToDisplayCurrency(row.price ?? 0, nativeQuote, displayCurrency, fxHub) : null
    return {
      ...row,
      symbol: row.symbol,
      price: row.price ?? 0,
      nativeQuote,
      displayAmount,
      timestamp: row.timestamp ?? null,
      freshness: row.freshness ?? 'LIVE',
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
  return fetchMarketOverviewPage(params)
}

function normalizeCatalogSymbol(symbol: string): string {
  return symbol.trim().replace(/[^A-Za-z0-9]/g, '').toUpperCase()
}

/**
 * Resolves a single instrument for deep links (e.g. news → analysis) when it is absent from
 * paginated {@link fetchMarketOverviewPage} (TCMB FX pairs live under `/api/market/fx`).
 */
export async function fetchMarketOverviewItemBySymbol(
  symbol: string,
  displayCurrency: SupportedCurrency = 'USD',
): Promise<MarketOverviewItem | null> {
  const normalized = normalizeCatalogSymbol(symbol)
  if (!normalized) {
    return null
  }
  try {
    const snapshot = await fetchMarketCatalogSnapshot({ query: normalized })
    const row = snapshot.filteredRows.find((item) => normalizeCatalogSymbol(item.symbol) === normalized)
    if (!row) {
      return null
    }
    const focused: MarketCatalogSnapshot = {
      filteredRows: [row],
      fxResponseItems: snapshot.fxResponseItems,
    }
    const page = await buildMarketOverviewFromCatalog(focused, {
      page: 0,
      size: 1,
      category: 'all',
      displayCurrency,
    })
    return (
      page.content.find((item) => normalizeCatalogSymbol(item.symbol) === normalized) ?? page.content[0] ?? null
    )
  } catch {
    return null
  }
}

export async function fetchMarketInsights(): Promise<MarketInsightsResponse> {
  const [gainersPage, losersPage] = await Promise.all([
    fetchMarketOverviewPage({
      page: 0,
      size: 5,
      category: 'all',
      query: '',
      sort: 'change1D,desc',
      displayCurrency: 'USD',
    }),
    fetchMarketOverviewPage({
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

/** Segment strip on Markets — same order as category filter chips (excludes "all"). */
export const MARKETS_PULSE_CATEGORIES: Exclude<MarketCategory, 'all'>[] = [
  'crypto',
  'bist',
  'nasdaq',
  'forex',
  'metals',
  'funds',
]

export type MarketCategoryPulseItem = {
  category: Exclude<MarketCategory, 'all'>
  /**
   * Equal-weight arithmetic mean of each instrument's 1D % move (aligned with Markets table 1D column).
   * Served by market-data-service {@code /api/market/segments/pulse} (short TTL cache server-side).
   */
  meanChange1D: number | null
  count: number
  advancingCount: number
}

export type MarketCategoryPulseOverall = {
  meanChange1D: number | null
  count: number
  advancingCount: number
}

export type MarketCategoryPulsePayload = {
  overall: MarketCategoryPulseOverall
  segments: MarketCategoryPulseItem[]
}

type SegmentPulseApiRow = {
  segment: string
  meanChange1D: number | null
  count: number
  advancingCount: number
}

type SegmentPulseApiOverall = {
  meanChange1D: number | null
  count: number
  advancingCount: number
}

type SegmentPulseApiResponse = {
  overall?: SegmentPulseApiOverall | null
  segments: SegmentPulseApiRow[]
  generatedAt: string
}

function mapOverall(raw: SegmentPulseApiOverall | null | undefined): MarketCategoryPulseOverall {
  if (!raw) {
    return { meanChange1D: null, count: 0, advancingCount: 0 }
  }
  const mean = raw.meanChange1D
  return {
    meanChange1D: mean != null && Number.isFinite(mean) ? mean : null,
    count: Number(raw.count) || 0,
    advancingCount: Number(raw.advancingCount) || 0,
  }
}

/** Delegates to market-data-service; do not poll more aggressively than the UI hook (~45s). */
export async function fetchMarketsCategoryPulse(): Promise<MarketCategoryPulsePayload> {
  const { data } = await apiClient.get<SegmentPulseApiResponse>('/api/market/segments/pulse')
  const rows = Array.isArray(data?.segments) ? data.segments : []
  const bySeg = new Map(rows.map((r) => [r.segment, r]))
  const segments = MARKETS_PULSE_CATEGORIES.map((category) => {
    const row = bySeg.get(category)
    if (!row) {
      return { category, meanChange1D: null, count: 0, advancingCount: 0 }
    }
    const mean = row.meanChange1D
    return {
      category,
      meanChange1D: mean != null && Number.isFinite(mean) ? mean : null,
      count: Number(row.count) || 0,
      advancingCount: Number(row.advancingCount) || 0,
    }
  })
  return { overall: mapOverall(data?.overall), segments }
}

/** Spot FX vs TRY: no equity fundamentals; avoid /fundamentals 404 when BFF/MDS catalog lags prices. */
function isTryFxFundamentalsLocal(symbol: string): boolean {
  const s = symbol.trim().toUpperCase()
  if (!s.endsWith('TRY') || s.length <= 3) return false
  const base = s.slice(0, -3)
  if (!/^[A-Z0-9]+$/.test(base)) return false
  if (base.length === 3 && /^X[A-Z]{2}$/.test(base)) return false
  return true
}

function syntheticTryFxFundamentals(symbol: string): InstrumentFundamentals {
  const s = symbol.trim().toUpperCase()
  return {
    symbol: s,
    provider: 'INTERNAL_META',
    providerSymbol: s,
    companyName: `${s} Exchange Rate`,
    country: null,
    currency: 'TRY',
    exchange: null,
    ipoDate: null,
    industry: 'Foreign Exchange',
    website: null,
    marketCapitalization: null,
    sharesOutstanding: null,
    peTtm: null,
    epsTtm: null,
    fetchedAt: new Date().toISOString(),
    cacheHit: false,
    annualStatements: [],
  }
}

/** TEFAS instruments are NAV-based; equity fundamentals APIs return 404 — use a stable placeholder instead of calling MDS. */
function isCanonicalTefasFundSymbol(symbol: string): boolean {
  const s = symbol.trim().toUpperCase()
  return s.startsWith('FUND_') && s.length > 'FUND_'.length
}

function syntheticTefasFundFundamentals(symbol: string): InstrumentFundamentals {
  const s = symbol.trim().toUpperCase()
  const code = s.startsWith('FUND_') ? s.slice('FUND_'.length) : s
  return {
    symbol: s,
    provider: 'INTERNAL_META',
    providerSymbol: s,
    companyName: code,
    country: 'TR',
    currency: 'TRY',
    exchange: 'TEFAS',
    ipoDate: null,
    industry: 'Investment Fund',
    website: null,
    marketCapitalization: null,
    sharesOutstanding: null,
    peTtm: null,
    epsTtm: null,
    fetchedAt: new Date().toISOString(),
    cacheHit: false,
    annualStatements: [],
  }
}

export async function fetchInstrumentFundamentals(symbol: string, forceRefresh = false): Promise<InstrumentFundamentals> {
  if (isTryFxFundamentalsLocal(symbol)) {
    void forceRefresh
    return syntheticTryFxFundamentals(symbol)
  }
  if (isCanonicalTefasFundSymbol(symbol)) {
    void forceRefresh
    return syntheticTefasFundFundamentals(symbol)
  }
  const response = await apiClient.get<InstrumentFundamentals>(`/api/market/instruments/${encodeURIComponent(symbol.trim())}/fundamentals`, {
    params: { forceRefresh },
  })
  return response.data
}
