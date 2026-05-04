import type { LineData, Time, UTCTimestamp } from 'lightweight-charts'
import { apiClient } from '../../../shared/api/client'
import type { CandlePoint } from '../../../pages/analysis/types'

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

function toSymbol(symbol: string): string {
  return symbol.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
}

function toDateParamUTC(ts: number): string {
  return new Date(ts).toISOString().slice(0, 10)
}

function dayBucketUtc(tsMs: number): number {
  const date = new Date(tsMs)
  return Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate())
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

export async function fetchCandles(symbol: string, range: AnalysisRange): Promise<CandlePoint[]> {
  const normalized = toSymbol(symbol)
  const interval = RANGE_TO_INTERVAL[range]
  const fromTs = Date.now() - RANGE_TO_MS[range]
  const historyDays = RANGE_TO_HISTORY_DAYS[range]

  if (historyDays) {
    const historyToTs = Date.now() - DAY_MS
    const historyFromTs = historyToTs - (historyDays - 1) * DAY_MS
    const historyPoints = await fetchHistoryCandles(normalized, range, historyFromTs, historyToTs)
    if (historyPoints.length > 0) {
      return historyPoints
    }
  }

  const response = await apiClient.get<ApiResponse<AnalyticsCandleDto[]>>(`/api/analytics/instruments/${normalized}/candles`, {
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
  if (interval === 'ONE_DAY' && shouldUseHistoryFallback(points, range)) {
    const historyToTs = Date.now() - DAY_MS
    const historyFromTs = historyDays ? historyToTs - (historyDays - 1) * DAY_MS : fromTs
    const historyFallback = await fetchHistoryCandles(normalized, range, historyFromTs, historyToTs)
    if (historyFallback.length > 0) {
      return historyFallback
    }
  }
  const fallbackPoints = RANGE_TO_FALLBACK_POINTS[range]
  return points.slice(Math.max(0, points.length - fallbackPoints))
}

function shouldUseHistoryFallback(points: CandlePoint[], range: AnalysisRange): boolean {
  const minPoints = range === '1y' ? 90 : range === '5y' ? 180 : 45
  return points.length < minPoints
}

async function fetchHistoryCandles(symbol: string, range: AnalysisRange, fromTs: number, toTs: number): Promise<CandlePoint[]> {
  const targetDays = RANGE_TO_HISTORY_DAYS[range]
  if (!targetDays) {
    return []
  }
  // Backend validates (to-from+1) <= 365 days for each request.
  // Keep each chunk safely within 365-day inclusive window.
  const chunkMs = 364 * DAY_MS
  let cursorFrom = fromTs
  const allPoints: HistoryPointDto[] = []
  while (cursorFrom <= toTs) {
    const cursorTo = Math.min(cursorFrom + chunkMs - 1, toTs)
    const response = await apiClient.get<HistoryPointDto[]>('/api/market/prices/history', {
      params: {
        symbol,
        from: toDateParamUTC(cursorFrom),
        to: toDateParamUTC(cursorTo),
      },
    })
    const chunk = Array.isArray(response.data) ? response.data : []
    allPoints.push(...chunk)
    cursorFrom = cursorTo + 1
  }
  if (allPoints.length === 0) {
    return []
  }

  const byDay = new Map<number, CandlePoint>()
  for (const item of allPoints) {
    if (!item?.time) continue
    const ts = Date.parse(item.time)
    const price = Number(item.value)
    if (!Number.isFinite(ts) || !Number.isFinite(price)) continue
    const day = dayBucketUtc(ts)
    const candleTime = Math.floor(day / 1000) as UTCTimestamp
    const existing = byDay.get(day)
    if (!existing) {
      byDay.set(day, {
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

  return [...byDay.values()]
    .sort((a, b) => a.time - b.time)
    .slice(-Math.max(targetDays + 10, 120))
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
