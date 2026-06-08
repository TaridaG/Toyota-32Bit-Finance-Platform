import type { TradePaymentCurrency, TransactionHistoryItem } from '../../../shared/types/portfolio'

const TRADE_PAYMENT_CURRENCIES: readonly TradePaymentCurrency[] = ['TRY', 'USD', 'EUR', 'GBP', 'JPY', 'AED']
const HISTORY_QUOTE_ISO = new Set(['TRY', 'USD', 'EUR', 'GBP', 'JPY', 'AED'])

function parseApiDecimal(value: unknown, fallback: number): number {
  if (value == null) return fallback
  if (typeof value === 'string') {
    const normalized = value.trim().replace(/\s/g, '').replace(',', '.')
    const n = Number(normalized)
    return Number.isFinite(n) ? n : fallback
  }
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

function normalizeToTradePaymentCurrency(raw: string | null | undefined): TradePaymentCurrency | null {
  const u = (raw ?? '').trim().toUpperCase()
  if (u === 'USDT') return 'USD'
  if ((TRADE_PAYMENT_CURRENCIES as readonly string[]).includes(u)) {
    return u as TradePaymentCurrency
  }
  return null
}

function inferInstrumentQuoteCurrency(symbol: string): 'TRY' | 'USD' | 'EUR' {
  const s = symbol.trim().toUpperCase()
  if (s.endsWith('TRY')) return 'TRY'
  if (s.endsWith('EUR')) return 'EUR'
  return 'USD'
}

function resolveHistoryQuoteCurrency(row: TransactionHistoryItem): string {
  const q = row.quoteCurrency?.trim().toUpperCase()
  if (q && HISTORY_QUOTE_ISO.has(q)) return q
  return inferInstrumentQuoteCurrency(row.instrumentSymbol)
}

function formatDecimalForLocale(value: number, language: string, maxFractionDigits: number): string {
  return new Intl.NumberFormat(language, {
    maximumFractionDigits: maxFractionDigits,
    minimumFractionDigits: 0,
  }).format(value)
}

function formatFxLeg(from: string, to: string, rate: number, language: string): string {
  const rateStr = formatDecimalForLocale(rate, language, 8)
  return `1 ${from} = ${rateStr} ${to}`
}

/**
 * Formats the transaction history "Alım kuru" column.
 * Prefers API-resolved {@code fxDisplay*} fields; falls back to legacy {@code fxRateUsed}.
 */
export function formatTxFxLegLabel(row: TransactionHistoryItem, language: string): string {
  const displayRate = parseApiDecimal(row.fxDisplayRate, Number.NaN)
  const displayFrom = row.fxDisplayFrom?.trim().toUpperCase()
  const displayTo = row.fxDisplayTo?.trim().toUpperCase()
  if (
    displayFrom &&
    displayTo &&
    Number.isFinite(displayRate) &&
    displayRate > 0
  ) {
    return formatFxLeg(displayFrom, displayTo, displayRate, language)
  }

  const rate = parseApiDecimal(row.fxRateUsed, Number.NaN)
  if (!Number.isFinite(rate) || rate <= 0) return '—'
  const pay = normalizeToTradePaymentCurrency(row.inputCurrency)
  const quote = resolveHistoryQuoteCurrency(row)
  if (pay == null) return '—'
  if (pay === quote) return '—'
  return formatFxLeg(pay, quote, rate, language)
}
