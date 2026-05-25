import type { MarketOverviewItem } from '../../../../shared/types/market'
import { FLOATING_MARKET_PINS, type FloatingMarketPin } from './constants'

export type ResolvedFloatingPin = FloatingMarketPin & {
  price: string
  change: number
  isLive: boolean
}

function normalizeSymbol(value: string): string {
  return value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase()
}

function findMarketRow(pin: FloatingMarketPin, rows: MarketOverviewItem[]): MarketOverviewItem | undefined {
  const hints = pin.apiSymbolHints ?? [pin.symbol]
  const normalizedHints = new Set(hints.map(normalizeSymbol))
  return rows.find((row) => normalizedHints.has(normalizeSymbol(row.symbol)))
}

function formatPinPrice(row: MarketOverviewItem | undefined, pin: FloatingMarketPin, locale: string): string {
  if (!row || !Number.isFinite(row.price)) {
    return pin.fallbackPrice
  }

  const isFx = pin.icon === 'fx'
  const isUsdQuote = pin.symbol.includes('USD') || pin.symbol.includes('USDT') || pin.icon === 'metal' || pin.icon === 'stock'

  if (isFx) {
    return new Intl.NumberFormat(locale, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 4,
    }).format(row.price)
  }

  if (isUsdQuote) {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: row.price >= 1000 ? 0 : 2,
    }).format(row.price)
  }

  return new Intl.NumberFormat(locale, {
    maximumFractionDigits: row.price >= 1000 ? 0 : 2,
  }).format(row.price)
}

export function resolveFloatingPins(rows: MarketOverviewItem[], locale: string): ResolvedFloatingPin[] {
  return FLOATING_MARKET_PINS.map((pin) => {
    const row = findMarketRow(pin, rows)
    const change = row?.change1D ?? row?.change24h ?? pin.fallbackChange
    return {
      ...pin,
      price: formatPinPrice(row, pin, locale),
      change,
      isLive: row?.freshness === 'LIVE',
    }
  })
}
