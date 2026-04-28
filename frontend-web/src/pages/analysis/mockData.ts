import type { UTCTimestamp } from 'lightweight-charts'
import type { AssetDefinition, AssetNewsItem, CandlePoint, ChartTradeEvent, TimeRange } from './types'

export const assets: AssetDefinition[] = [
  { id: 'thy', symbol: 'THYAO', name: 'Turkish Airlines', type: 'stock', marketCap: 13_200_000_000 },
  { id: 'btc', symbol: 'BTC/USDT', name: 'Bitcoin', type: 'crypto', marketCap: 2_130_000_000_000 },
  { id: 'usdtry', symbol: 'USDTRY', name: 'US Dollar / Turkish Lira', type: 'fx' },
  { id: 'gold', symbol: 'XAUUSD', name: 'Gold Spot', type: 'commodity', marketCap: 18_000_000_000_000 },
  { id: 'bist', symbol: 'BIST100', name: 'Borsa Istanbul 100', type: 'index' },
]

const DAY = 86400

function seededRandom(seed: number) {
  let state = seed
  return () => {
    state = (state * 1664525 + 1013904223) % 4294967296
    return state / 4294967296
  }
}

/** ~2.2 years of daily OHLC (UTC day boundaries). */
function generateDailyCandles(seed: number, startPrice: number, days: number): CandlePoint[] {
  const rand = seededRandom(seed)
  const now = Math.floor(Date.now() / 1000)
  const alignEnd = Math.floor(now / DAY) * DAY
  const firstTime = alignEnd - (days - 1) * DAY
  const candles: CandlePoint[] = []
  let previousClose = startPrice

  for (let i = 0; i < days; i += 1) {
    const drift = (rand() - 0.498) * 0.028
    const noise = (rand() - 0.5) * previousClose * 0.012
    const open = previousClose
    const close = Math.max(0.01, open * (1 + drift) + noise * 0.04)
    const high = Math.max(open, close) * (1 + rand() * 0.018)
    const low = Math.min(open, close) * (1 - rand() * 0.018)
    const volume = Math.max(1000, Math.round(80_000 + rand() * 920_000))
    candles.push({
      time: (firstTime + i * DAY) as UTCTimestamp,
      open: Number(open.toFixed(4)),
      high: Number(high.toFixed(4)),
      low: Number(low.toFixed(4)),
      close: Number(close.toFixed(4)),
      volume,
    })
    previousClose = close
  }

  return candles
}

export const candleSeriesByAsset: Record<string, CandlePoint[]> = {
  thy: generateDailyCandles(7, 262.4, 800),
  btc: generateDailyCandles(13, 68400, 800),
  usdtry: generateDailyCandles(19, 37.9, 800),
  gold: generateDailyCandles(23, 2330, 800),
  bist: generateDailyCandles(29, 9200, 800),
}

function barsForRange(range: TimeRange): number {
  if (range === '1h') return 60
  if (range === '6h') return 6 * 60
  if (range === '24h') return 24 * 60
  return 7 * 24 * 60
}

export function getWindowedSeries(series: CandlePoint[], range: TimeRange) {
  const n = barsForRange(range)
  return series.slice(-n)
}

export function getPerformancePercent(series: CandlePoint[]) {
  if (series.length < 2) return 0
  const first = series[0].close
  const last = series[series.length - 1].close
  return ((last - first) / first) * 100
}

const anchorDay = (Math.floor(Date.now() / 1000 / DAY) * DAY) as UTCTimestamp

export const newsByAsset: AssetNewsItem[] = [
  {
    id: 'news-thy-1',
    assetId: 'thy',
    title: 'THYAO traffic update beats quarterly expectation',
    summary: 'Passenger load factor remains resilient with stronger Europe routes.',
    source: 'BIST Wire',
    impact: 'positive',
    createdAt: (anchorDay - 2 * DAY) as UTCTimestamp,
    reactionPercent1h: 1.9,
    relatedAssets: ['THYAO', 'BIST100'],
  },
  {
    id: 'news-btc-1',
    assetId: 'btc',
    title: 'Spot ETF flow accelerates into US session',
    summary: 'Institutional inflows support BTC risk appetite and derivatives premium.',
    source: 'Crypto Pulse',
    impact: 'positive',
    createdAt: (anchorDay - 5 * DAY) as UTCTimestamp,
    reactionPercent1h: 2.6,
    relatedAssets: ['BTC/USDT', 'ETH/USDT'],
  },
  {
    id: 'news-usd-1',
    assetId: 'usdtry',
    title: 'USDTRY rangebound ahead of inflation expectations survey',
    summary: 'Short-term volatility remains muted while liquidity remains balanced.',
    source: 'FX Terminal',
    impact: 'neutral',
    createdAt: (anchorDay - 9 * DAY) as UTCTimestamp,
    reactionPercent1h: 0.1,
    relatedAssets: ['USDTRY', 'EURTRY'],
  },
  {
    id: 'news-gold-1',
    assetId: 'gold',
    title: 'Gold eases as real yields tick higher',
    summary: 'Profit-taking emerges after a multi-day rally in precious metals.',
    source: 'Macro Desk',
    impact: 'negative',
    createdAt: (anchorDay - 12 * DAY) as UTCTimestamp,
    reactionPercent1h: -1.2,
    relatedAssets: ['XAUUSD', 'DXY'],
  },
  {
    id: 'news-bist-1',
    assetId: 'bist',
    title: 'Banking basket drags index into late session',
    summary: 'Weakness in large financials weighs on broad index performance.',
    source: 'Borsa Not',
    impact: 'negative',
    createdAt: (anchorDay - 18 * DAY) as UTCTimestamp,
    reactionPercent1h: -1.6,
    relatedAssets: ['BIST100', 'XBANK'],
  },
]

export const tradeEventsByAsset: Record<string, ChartTradeEvent[]> = {
  thy: [
    { id: 'tr-thy-1', time: (anchorDay - 4 * DAY) as UTCTimestamp, title: 'Desk rebalance — buy', side: 'buy' },
    { id: 'tr-thy-2', time: (anchorDay - 11 * DAY) as UTCTimestamp, title: 'Hedge trim — sell', side: 'sell' },
    { id: 'tr-thy-3', time: (anchorDay - 30 * DAY) as UTCTimestamp, title: 'Program buy — VWAP', side: 'buy' },
  ],
  btc: [
    { id: 'tr-btc-1', time: (anchorDay - 3 * DAY) as UTCTimestamp, title: 'Breakout chase — buy', side: 'buy' },
    { id: 'tr-btc-2', time: (anchorDay - 16 * DAY) as UTCTimestamp, title: 'Risk-off unwind — sell', side: 'sell' },
  ],
  usdtry: [
    { id: 'tr-usd-1', time: (anchorDay - 6 * DAY) as UTCTimestamp, title: 'Corp flow — buy TRY', side: 'sell' },
    { id: 'tr-usd-2', time: (anchorDay - 22 * DAY) as UTCTimestamp, title: 'Importer hedge — buy USD', side: 'buy' },
  ],
  gold: [
    { id: 'tr-au-1', time: (anchorDay - 7 * DAY) as UTCTimestamp, title: 'ETF creation — buy', side: 'buy' },
    { id: 'tr-au-2', time: (anchorDay - 19 * DAY) as UTCTimestamp, title: 'CTA exit — sell', side: 'sell' },
  ],
  bist: [
    { id: 'tr-bist-1', time: (anchorDay - 8 * DAY) as UTCTimestamp, title: 'Index roll — buy', side: 'buy' },
    { id: 'tr-bist-2', time: (anchorDay - 25 * DAY) as UTCTimestamp, title: 'Rebalance — sell', side: 'sell' },
  ],
}
