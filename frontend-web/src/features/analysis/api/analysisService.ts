import type { LineData, Time, UTCTimestamp } from 'lightweight-charts'
import { apiClient } from '../../../shared/api/client'
import type { CandlePoint } from '../../../pages/analysis/types'
import { symbolUsesFundMarketHistory, symbolUsesFxMarketHistory, US_LISTED_ETF_TICKERS_AS_FUNDS } from '../../markets/api/marketService'

type ApiResponse<T> = {
  success: boolean
  data: T
}

type AnalyticsCandleDto = {
  openTime?: string
  candleDate?: string
  open: number
  high: number
  low: number
  close: number
  volume?: number
}

export type AnalysisRange = '1h' | '6h' | '24h' | '7d' | '30d' | '90d' | '1y' | '5y'
type HistoryPointDto = { time?: string; value?: number }

export type AnalysisHistoryKind = 'price' | 'fx' | 'fund'

export type FetchCandlesContext = {
  /** Catalog wire category (STOCK, FX, CRYPTO, FUND, METAL, …) — drives which history endpoint is used. */
  wireCategory?: string | null
}

const RANGE_TO_INTERVAL: Record<AnalysisRange, string> = {
  '1h': 'ONE_MINUTE',
  '6h': 'FIVE_MINUTES',
  '24h': 'ONE_HOUR',
  '7d': 'ONE_DAY',
  '30d': 'ONE_DAY',
  '90d': 'ONE_DAY',
  '1y': 'ONE_DAY',
  '5y': 'ONE_DAY',
}

const RANGE_TO_MS: Record<AnalysisRange, number> = {
  '1h': 60 * 60 * 1000,
  '6h': 6 * 60 * 60 * 1000,
  '24h': 24 * 60 * 60 * 1000,
  '7d': 7 * 24 * 60 * 60 * 1000,
  '30d': 30 * 24 * 60 * 60 * 1000,
  '90d': 90 * 24 * 60 * 60 * 1000,
  '1y': 365 * 24 * 60 * 60 * 1000,
  '5y': 5 * 365 * 24 * 60 * 60 * 1000,
}

const RANGE_TO_FALLBACK_POINTS: Record<AnalysisRange, number> = {
  '1h': 240,
  '6h': 240,
  '24h': 360,
  '7d': 365,
  '30d': 500,
  '90d': 800,
  '1y': 900,
  '5y': 2200,
}

const RANGE_TO_HISTORY_DAYS: Partial<Record<AnalysisRange, number>> = {
  '30d': 30,
  '90d': 90,
  '1y': 365,
  '5y': 365 * 5,
}
const DAY_MS = 24 * 60 * 60 * 1000

/** Strip non-alphanumeric chars for analytics API path segments (e.g. "BTC-USD" → "BTCUSD"). */
export function normalizeAnalysisInstrumentSymbol(symbol: string): string {
  return symbol.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
}

/**
 * Symbol as stored in market-data history tables / catalog (keeps Yahoo-style "=" and "." e.g. GC=F, BRK.B).
 * Using this for `/api/market/prices/history` is required — stripping "=" breaks futures lookups.
 */
function wireCatalogSymbol(symbol: string): string {
  return symbol.trim().toUpperCase()
}

function analyticsSymbolCandidates(raw: string): string[] {
  const wired = wireCatalogSymbol(raw)
  const stripped = normalizeAnalysisInstrumentSymbol(raw)
  if (!wired && !stripped) {
    return []
  }
  if (stripped === wired) {
    return [stripped]
  }
  return [stripped, wired]
}

function toDateParamUTC(ts: number): string {
  return new Date(ts).toISOString().slice(0, 10)
}

function dayBucketUtc(tsMs: number): number {
  const date = new Date(tsMs)
  return Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate())
}

function hourBucketUtc(tsMs: number): number {
  const d = new Date(tsMs)
  return Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate(), d.getUTCHours())
}

function resolveHistoryKind(catalogSymbol: string, wireCategory: string | null | undefined): AnalysisHistoryKind {
  const sym = catalogSymbol.trim().toUpperCase()
  if (symbolUsesFundMarketHistory(wireCategory) && !US_LISTED_ETF_TICKERS_AS_FUNDS.has(sym)) {
    return 'fund'
  }
  if (symbolUsesFxMarketHistory(catalogSymbol, wireCategory)) {
    return 'fx'
  }
  const norm = catalogSymbol.trim().toUpperCase()
  const cat = (wireCategory ?? '').trim().toUpperCase()
  if (!cat && (norm.endsWith('TRY') || norm.includes('/'))) {
    return 'fx'
  }
  return 'price'
}

function historyUrl(kind: AnalysisHistoryKind): string {
  if (kind === 'fx') {
    return '/api/market/fx/history'
  }
  if (kind === 'fund') {
    return '/api/market/funds/history'
  }
  return '/api/market/prices/history'
}

function historyParams(symbol: string, from: string, to: string, kind: AnalysisHistoryKind): Record<string, string> {
  if (kind === 'fund') {
    return { fundCode: symbol, from, to }
  }
  return { symbol, from, to }
}

function sortHistoryPointsAsc(points: HistoryPointDto[]): HistoryPointDto[] {
  return [...points]
    .map((p) => ({ p, ts: p.time ? Date.parse(p.time) : NaN }))
    .filter((x) => Number.isFinite(x.ts))
    .sort((a, b) => a.ts - b.ts)
    .map((x) => x.p)
}

async function collectHistoryPoints(symbol: string, fromTs: number, toTs: number, kind: AnalysisHistoryKind): Promise<HistoryPointDto[]> {
  /** MDS history API rejects spans > 365 inclusive calendar days; keep chunks under that (DST-safe). */
  const chunkMs = 330 * DAY_MS
  let cursorFrom = fromTs
  const allPoints: HistoryPointDto[] = []
  while (cursorFrom <= toTs) {
    const cursorTo = Math.min(cursorFrom + chunkMs - 1, toTs)
    const from = toDateParamUTC(cursorFrom)
    const to = toDateParamUTC(cursorTo)
    const response = await apiClient.get<HistoryPointDto[]>(historyUrl(kind), {
      params: historyParams(symbol, from, to, kind),
    })
    const chunk = Array.isArray(response.data) ? response.data : []
    allPoints.push(...chunk)
    cursorFrom = cursorTo + 1
  }
  const sorted = sortHistoryPointsAsc(allPoints)
  if (kind === 'fund' && sorted.length === 0) {
    return collectHistoryPoints(symbol, fromTs, toTs, 'price')
  }
  return sorted
}

function bucketGranularity(range: AnalysisRange): 'hour' | 'day' {
  return range === '1h' || range === '6h' || range === '24h' ? 'hour' : 'day'
}

function historyPointsToCandles(points: HistoryPointDto[], granularity: 'hour' | 'day', maxBars: number): CandlePoint[] {
  const bucket = granularity === 'day' ? dayBucketUtc : hourBucketUtc
  const byKey = new Map<number, CandlePoint>()
  for (const item of points) {
    if (!item?.time) {
      continue
    }
    const ts = Date.parse(item.time)
    const price = Number(item.value)
    if (!Number.isFinite(ts) || !Number.isFinite(price)) {
      continue
    }
    const key = bucket(ts)
    const candleTime = Math.floor(key / 1000) as UTCTimestamp
    const existing = byKey.get(key)
    if (!existing) {
      byKey.set(key, {
        time: candleTime,
        open: price,
        high: price,
        low: price,
        close: price,
        volume: 0,
      })
      continue
    }
    existing.high = Math.max(existing.high, price)
    existing.low = Math.min(existing.low, price)
    existing.close = price
  }
  return [...byKey.values()].sort((a, b) => a.time - b.time).slice(-maxBars)
}

/** Minimum calendar span to pull when analytics has no rows (FX/funds rarely have intraday buckets). */
function backfillCalendarDays(range: AnalysisRange): number {
  switch (range) {
    case '1h':
      return 7
    case '6h':
      return 14
    case '24h':
      return 21
    case '7d':
      return 21
    case '30d':
      return 45
    case '90d':
      return 120
    case '1y':
      return 400
    case '5y':
      return 365 * 5 + 30
    default:
      return 60
  }
}

function mapCandle(dto: AnalyticsCandleDto): CandlePoint | null {
  const rawTime = dto.openTime ?? dto.candleDate
  if (!rawTime) {
    return null
  }
  const ts = Date.parse(rawTime)
  if (Number.isNaN(ts)) {
    return null
  }
  return {
    time: Math.floor(ts / 1000) as UTCTimestamp,
    open: dto.open,
    high: dto.high,
    low: dto.low,
    close: dto.close,
    volume: Number.isFinite(Number(dto.volume)) ? Number(dto.volume) : 0,
  }
}

async function fetchAnalyticsCandlesForRange(
  analyticsCandidates: string[],
  interval: string,
  fromTs: number,
): Promise<CandlePoint[]> {
  for (const sym of analyticsCandidates) {
    if (!sym) {
      continue
    }
    const pathSeg = encodeURIComponent(sym)
    try {
      const response = await apiClient.get<ApiResponse<AnalyticsCandleDto[]>>(`/api/analytics/instruments/${pathSeg}/candles`, {
        params: { interval },
      })
      const points = (response.data.data ?? [])
        .map(mapCandle)
        .filter((item): item is CandlePoint => item != null)
        .sort((a, b) => a.time - b.time)
      const windowed = points.filter((point) => point.time * 1000 >= fromTs)
      if (windowed.length > 0) {
        return windowed
      }
      if (points.length > 0) {
        return points
      }
    } catch {
      /* try next candidate symbol */
    }
  }
  return []
}

export async function fetchCandles(symbol: string, range: AnalysisRange, ctx?: FetchCandlesContext): Promise<CandlePoint[]> {
  const historySym = wireCatalogSymbol(symbol)
  if (!historySym) {
    return []
  }
  const analyticsCandidates = analyticsSymbolCandidates(symbol)
  const wire = ctx?.wireCategory ?? null
  const kind = resolveHistoryKind(historySym, wire)
  const interval = RANGE_TO_INTERVAL[range]
  const fromTs = Date.now() - RANGE_TO_MS[range]
  const historyDays = RANGE_TO_HISTORY_DAYS[range]

  if (historyDays) {
    const historyToTs = Date.now() - DAY_MS
    const historyFromTs = historyToTs - (historyDays - 1) * DAY_MS
    const historyPoints = await fetchHistoryCandles(historySym, range, historyFromTs, historyToTs, kind)
    if (historyPoints.length > 0) {
      return historyPoints
    }
  }

  const points = await fetchAnalyticsCandlesForRange(analyticsCandidates, interval, fromTs)
  const windowed = points.filter((point) => point.time * 1000 >= fromTs)
  if (windowed.length > 0) {
    return windowed
  }
  if (points.length > 0) {
    return points
  }

  if (interval === 'ONE_DAY' && shouldUseHistoryFallback(points, range)) {
    const historyToTs = Date.now() - DAY_MS
    const historyFromTs = historyDays ? historyToTs - (historyDays - 1) * DAY_MS : fromTs
    const historyFallback = await fetchHistoryCandles(historySym, range, historyFromTs, historyToTs, kind)
    if (historyFallback.length > 0) {
      return historyFallback
    }
  }

  const fromMarket = await candlesFromMarketHistory(historySym, range, fromTs, Date.now(), kind)
  if (fromMarket.length > 0) {
    const w = fromMarket.filter((point) => point.time * 1000 >= fromTs)
    return w.length > 0 ? w : fromMarket
  }

  const fallbackPoints = RANGE_TO_FALLBACK_POINTS[range]
  return points.slice(Math.max(0, points.length - fallbackPoints))
}

function shouldUseHistoryFallback(points: CandlePoint[], range: AnalysisRange): boolean {
  const minPoints = range === '1y' ? 90 : range === '5y' ? 180 : 45
  return points.length < minPoints
}

async function candlesFromMarketHistory(
  symbol: string,
  range: AnalysisRange,
  _windowFromTs: number,
  windowToTs: number,
  kind: AnalysisHistoryKind,
): Promise<CandlePoint[]> {
  const spanDays = backfillCalendarDays(range)
  const historyFromTs = windowToTs - spanDays * DAY_MS
  const raw = await collectHistoryPoints(symbol, historyFromTs, windowToTs, kind)
  if (raw.length === 0) {
    return []
  }
  const granularity = bucketGranularity(range)
  const maxBars = Math.min(5000, Math.max(120, spanDays * (granularity === 'hour' ? 24 : 1) + 50))
  return historyPointsToCandles(raw, granularity, maxBars)
}

async function fetchHistoryCandles(
  symbol: string,
  range: AnalysisRange,
  fromTs: number,
  toTs: number,
  kind: AnalysisHistoryKind,
): Promise<CandlePoint[]> {
  const targetDays = RANGE_TO_HISTORY_DAYS[range]
  if (!targetDays) {
    return []
  }
  const raw = await collectHistoryPoints(symbol, fromTs, toTs, kind)
  if (raw.length === 0) {
    return []
  }
  const candles = historyPointsToCandles(raw, 'day', Math.max(targetDays + 10, 120))
  return candles.sort((a, b) => a.time - b.time).slice(-Math.max(targetDays + 10, 120))
}

function calculateMovingAverage(candles: CandlePoint[], period: number): LineData<Time>[] {
  return candles.map((bar, index) => {
    const start = Math.max(0, index - period + 1)
    const slice = candles.slice(start, index + 1)
    const avg = slice.reduce((acc, cur) => acc + cur.close, 0) / slice.length
    return { time: bar.time, value: Number(avg.toFixed(6)) }
  })
}

function calculateRsi(candles: CandlePoint[], period = 14): LineData<Time>[] {
  if (candles.length < 2) {
    return []
  }
  const result: LineData<Time>[] = []
  let gains = 0
  let losses = 0
  for (let i = 1; i < candles.length; i += 1) {
    const diff = candles[i].close - candles[i - 1].close
    gains += diff > 0 ? diff : 0
    losses += diff < 0 ? Math.abs(diff) : 0
    if (i < period) {
      continue
    }
    if (i > period) {
      const prevDiff = candles[i - period + 1].close - candles[i - period].close
      gains -= prevDiff > 0 ? prevDiff : 0
      losses -= prevDiff < 0 ? Math.abs(prevDiff) : 0
    }
    const avgGain = gains / period
    const avgLoss = losses / period
    const rs = avgLoss === 0 ? 100 : avgGain / avgLoss
    const rsi = 100 - 100 / (1 + rs)
    result.push({ time: candles[i].time, value: Number(rsi.toFixed(4)) })
  }
  return result
}

export function buildIndicators(candles: CandlePoint[]): { ma20: LineData<Time>[]; ma50: LineData<Time>[]; rsi: LineData<Time>[] } {
  return {
    ma20: calculateMovingAverage(candles, 20),
    ma50: calculateMovingAverage(candles, 50),
    rsi: calculateRsi(candles, 14),
  }
}
