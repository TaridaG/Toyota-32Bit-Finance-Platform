import type { LineData, Time, UTCTimestamp } from 'lightweight-charts'
import { apiClient } from '../../../shared/api/client'
import type { CandlePoint } from '../../../pages/analysis/types'

type ApiResponse<T> = {
  success: boolean
  data: T
}

type AnalyticsCandleDto = {
  time: string
  open: number
  high: number
  low: number
  close: number
}

export type AnalysisRange = '1h' | '6h' | '24h' | '7d'

const RANGE_TO_INTERVAL: Record<AnalysisRange, string> = {
  '1h': '1m',
  '6h': '5m',
  '24h': '15m',
  '7d': '1h',
}

const RANGE_TO_MS: Record<AnalysisRange, number> = {
  '1h': 60 * 60 * 1000,
  '6h': 6 * 60 * 60 * 1000,
  '24h': 24 * 60 * 60 * 1000,
  '7d': 7 * 24 * 60 * 60 * 1000,
}

function toFromTo(range: AnalysisRange): { from: string; to: string } {
  const to = new Date()
  const from = new Date(to.getTime() - RANGE_TO_MS[range])
  return { from: from.toISOString(), to: to.toISOString() }
}

function toSymbol(symbol: string): string {
  return symbol.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
}

function mapCandle(dto: AnalyticsCandleDto): CandlePoint | null {
  const ts = Date.parse(dto.time)
  if (Number.isNaN(ts)) {
    return null
  }
  return {
    time: Math.floor(ts / 1000) as UTCTimestamp,
    open: dto.open,
    high: dto.high,
    low: dto.low,
    close: dto.close,
    volume: 0,
  }
}

export async function fetchCandles(symbol: string, range: AnalysisRange): Promise<CandlePoint[]> {
  const normalized = toSymbol(symbol)
  const { from, to } = toFromTo(range)
  const interval = RANGE_TO_INTERVAL[range]
  const response = await apiClient.get<ApiResponse<AnalyticsCandleDto[]>>(`/api/analytics/instruments/${normalized}/candles`, {
    params: { from, to, interval },
  })
  return (response.data.data ?? [])
    .map(mapCandle)
    .filter((item): item is CandlePoint => item != null)
    .sort((a, b) => a.time - b.time)
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
