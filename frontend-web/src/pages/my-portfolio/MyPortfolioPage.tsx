import {
  Suspense,
  lazy,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type MouseEvent as ReactMouseEvent,
  type PointerEvent as ReactPointerEvent,
} from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { FavoriteNewsInsightCard } from './components/FavoriteNewsInsightCard'
import { TargetsInsightCard } from './components/TargetsInsightCard'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { fetchMarketOverview } from '../../features/markets/api/marketService'
import { inferNativeQuote } from '../../features/markets/lib/marketDisplayConversion'
import {
  buyTrade,
  createPortfolio,
  deletePortfolio,
  deleteTransaction,
  getInstrumentsCatalogForTradePicker,
  getMyPortfolioOverview,
  getPortfolioHoldingsAsOf,
  getPortfolioPerformanceSeries,
  getPortfolioSnapshots,
  getPortfolios,
  getSalesAnalysisPage,
  getTransactionHistory,
  getTransactionHistoryPage,
  patchPortfolioAmountsHidden,
  previewTrade,
  sellTrade,
} from '../../features/portfolio/api/portfolioApi'
import type {
  AcquisitionFxRatesSnapshot,
  Portfolio,
  PortfolioOverview,
  PortfolioOverviewItem,
  PortfolioPerformanceSeries,
  PortfolioTradeFlow,
  PortfolioValueSnapshot,
  PurchaseMode,
  TradeInputMode,
  TradePaymentCurrency,
  TradePreview,
  TransactionHistoryFilters,
  TransactionHistoryItem,
  SalesAnalysisFilters,
  SalesAnalysisItem,
} from '../../shared/types/portfolio'
import type { MarketOverviewPageResponse } from '../../shared/types/market'
import { AllocationDonut, type AllocationCategoryGroup, type AllocationDonutRow } from './components/AllocationDonut'
import { PnlSplitDonut } from './components/PnlSplitDonut'
import { PortfolioHistorySparkline } from './components/PortfolioHistorySparkline'
import { tradeFlowTotals } from './components/tradeFlowStats'
import { PortfolioValueHistoryChart, type PortfolioChartMetric } from './components/PortfolioValueHistoryChart'
import { RANGE_TO_MS, VALUE_CHART_RANGES, valueChartRangeLabel, type ValueChartRange } from './components/portfolioChartShared'
import { loadTradeFlowForPortfolio } from '../../features/portfolio/lib/loadTradeFlowForPortfolio'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'
import { MyAnalysisPanel } from '../my-analysis/MyAnalysisPanel'
import { MyNewsPanel } from '../my-news/MyNewsPanel'
import { PortfolioGoalsPanel } from './components/PortfolioGoalsPanel'
import { fetchTlDepositIndexLatest, type TlDepositIndexLatestResponse } from '../faiz-vadeli/api/tlDepositApi'
import { getTlDepositMaturityCodeFromInstrumentSymbol } from '../faiz-vadeli/lib/tlDepositMaturity'

const MyPortfolioWatchlistSection = lazy(async () => {
  const mod = await import('./components/MyPortfolioWatchlistSection')
  return { default: mod.MyPortfolioWatchlistSection }
})

const MAX_USER_PORTFOLIOS = 5
const CREATE_PORTFOLIO_SELECT_VALUE = '__create_portfolio__'
/** Aggregate "Genel Bakış" in portfolio picker; not a real DB id. */
const ALL_PORTFOLIOS_ID = -1
const MASKED_MONEY_LABEL = '••••'

function PortfolioDashboardGlyph({
  kind,
}: {
  kind: 'performance' | 'value' | 'range' | 'currency' | 'history'
}) {
  switch (kind) {
    case 'performance':
      return (
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <path
            d="M2.5 10.5 5.6 7.4l2.2 2.2 4.7-5.1"
            fill="none"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.6"
          />
          <path
            d="M10.7 4.5h3v3"
            fill="none"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.6"
          />
        </svg>
      )
    case 'value':
      return (
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <rect x="2.5" y="8.5" width="2.2" height="4.5" rx="0.8" fill="currentColor" />
          <rect x="6.9" y="5.8" width="2.2" height="7.2" rx="0.8" fill="currentColor" opacity="0.9" />
          <rect x="11.3" y="3.2" width="2.2" height="9.8" rx="0.8" fill="currentColor" opacity="0.78" />
        </svg>
      )
    case 'range':
      return (
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <circle cx="8" cy="8" r="5.25" fill="none" stroke="currentColor" strokeWidth="1.5" />
          <path
            d="M8 5.2v3.1l2.1 1.4"
            fill="none"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.5"
          />
        </svg>
      )
    case 'currency':
      return (
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <path
            d="M5.3 4.1c.8-.7 1.8-1 3-1 2.1 0 3.5 1 3.5 2.5 0 3.3-5.7 1.7-5.7 4.5 0 1.2 1.1 2 2.9 2 1 0 2-.2 3-.8"
            fill="none"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.4"
          />
          <path d="M8 2.3v11.4" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.4" />
        </svg>
      )
    case 'history':
      return (
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <path
            d="M3 11.5h10"
            fill="none"
            stroke="currentColor"
            strokeLinecap="round"
            strokeWidth="1.4"
            opacity="0.5"
          />
          <circle cx="4.2" cy="8.4" r="1.1" fill="currentColor" />
          <circle cx="8" cy="6.5" r="1.1" fill="currentColor" opacity="0.85" />
          <circle cx="11.8" cy="4.8" r="1.1" fill="currentColor" opacity="0.7" />
        </svg>
      )
  }
}

/** Read selected portfolio from `?portfolio=` (numeric id or `all` / omitted = Genel Bakış). */
function portfolioIdFromSearchParams(params: URLSearchParams): number {
  const raw = params.get('portfolio')?.trim()
  if (!raw || raw === 'all' || raw === 'overview') {
    return ALL_PORTFOLIOS_ID
  }
  const id = Number(raw)
  if (Number.isFinite(id) && id > 0) {
    return Math.trunc(id)
  }
  return ALL_PORTFOLIOS_ID
}

function resolvePortfolioSelection(preferred: number | null, rows: Portfolio[]): number | null {
  if (rows.length === 0) {
    return null
  }
  if (preferred != null && preferred > 0 && rows.some((p) => p.id === preferred)) {
    return preferred
  }
  if (preferred === ALL_PORTFOLIOS_ID) {
    return ALL_PORTFOLIOS_ID
  }
  return ALL_PORTFOLIOS_ID
}

const EMPTY_MARKET_OVERVIEW: MarketOverviewPageResponse = {
  content: [],
  page: 0,
  size: 0,
  totalElements: 0,
  totalPages: 0,
}

function mergePortfolioValueSnapshots(seriesList: PortfolioValueSnapshot[][]): PortfolioValueSnapshot[] {
  type Agg = { totalCost: number; totalValue: number; unrealizedPnl: number; createdAt: string }
  const map = new Map<string, Agg>()
  let userId = ''
  for (const series of seriesList) {
    for (const s of series) {
      if (!userId && s.userId) userId = s.userId
      const day = s.createdAt.slice(0, 10)
      const cur = map.get(day)
      if (!cur) {
        map.set(day, {
          totalCost: Number(s.totalCost),
          totalValue: Number(s.totalValue),
          unrealizedPnl: Number(s.unrealizedPnl),
          createdAt: s.createdAt,
        })
      } else {
        cur.totalCost += Number(s.totalCost)
        cur.totalValue += Number(s.totalValue)
        cur.unrealizedPnl += Number(s.unrealizedPnl)
      }
    }
  }
  return [...map.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([, v], i) => ({
      id: -(i + 1),
      userId,
      totalCost: v.totalCost,
      totalValue: v.totalValue,
      unrealizedPnl: v.unrealizedPnl,
      createdAt: v.createdAt,
      externalPortfolioId: null,
    }))
}

function maskedNumberFormatShim(): Intl.NumberFormat {
  return { format: () => MASKED_MONEY_LABEL } as unknown as Intl.NumberFormat
}

const DOD_EPS = 1e-6

/** Coerce API decimals (number, string, null) so day-over-day line never falls through to "—" unnecessarily. */
function parseApiDecimal(value: unknown, fallback: number): number {
  if (value == null) return fallback
  if (typeof value === 'number') return Number.isFinite(value) ? value : fallback
  if (typeof value === 'string') {
    const normalized = value.trim().replace(/\s/g, '').replace(',', '.')
    const n = Number(normalized)
    return Number.isFinite(n) ? n : fallback
  }
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

/** Listing currency for price/totalAmount when API omits quoteCurrency (older payloads). */
function inferInstrumentQuoteCurrency(symbol: string): 'TRY' | 'USD' | 'EUR' {
  const s = symbol.trim().toUpperCase()
  if (s.endsWith('TRY')) return 'TRY'
  if (s.endsWith('EUR')) return 'EUR'
  return 'USD'
}

const RECENT_TX_PREVIEW_SIZE = 8

const EMPTY_TX_FILTERS: TransactionHistoryFilters = {
  symbol: '',
  type: '',
  purchaseMode: '',
  inputCurrency: '',
  fromDate: '',
  toDate: '',
}

function sortTransactionsNewestFirst(rows: TransactionHistoryItem[]): TransactionHistoryItem[] {
  return [...rows].sort((a, b) => {
    const ta = new Date(a.acquiredAt ?? a.createdAt).getTime()
    const tb = new Date(b.acquiredAt ?? b.createdAt).getTime()
    return tb - ta
  })
}

const DISTRIBUTION_TOP_N = 8
const DISTRIBUTION_BAR_COLORS = [
  'my-portfolio-dot-blue',
  'my-portfolio-dot-purple',
  'my-portfolio-dot-pink',
  'my-portfolio-dot-green',
  'my-portfolio-dot-amber',
  'my-portfolio-dot-cyan',
  'my-portfolio-dot-indigo',
  'my-portfolio-dot-teal',
] as const

type DistributionBarSeg = { key: string; widthPct: number; colorClass: string }
type DistributionRow = {
  symbol: string
  value: number
  sharePct: number
  colorClass: string
  quantity: number
  totalCost: number
  avgBuyPrice: number
  currentPrice: number
  pnlPercent: number
}

function formatHoldingQuantity(q: number, locale: string): string {
  if (!Number.isFinite(q)) return '—'
  if (Math.abs(q - Math.round(q)) < 1e-9) return String(Math.round(q))
  return new Intl.NumberFormat(locale, { maximumFractionDigits: 6 }).format(q)
}

type AllocationSortKey =
  | 'symbol'
  | 'quantity'
  | 'cost'
  | 'avgBuyPrice'
  | 'currentPrice'
  | 'weight'
  | 'value'
  | 'pnl'
type AllocationSortDir = 'asc' | 'desc'

function buildPortfolioDistribution(overview: PortfolioOverview | null): {
  bar: DistributionBarSeg[]
  topList: DistributionRow[]
  fullList: DistributionRow[]
} {
  if (!overview?.items?.length) {
    return { bar: [], topList: [], fullList: [] }
  }
  const totalVal = Math.max(parseApiDecimal(overview.totalValue, 0), 1e-12)
  const rows = overview.items
    .map((it) => {
      const quantity = parseApiDecimal(it.quantity, 0)
      const avgBuyPrice = parseApiDecimal(it.avgBuyPrice, 0)
      const currentPrice = parseApiDecimal(it.currentPrice, 0)
      const totalCost = Math.max(0, quantity * avgBuyPrice)
      return {
        symbol: it.symbol,
        value: Math.max(0, parseApiDecimal(it.value, 0)),
        quantity,
        avgBuyPrice,
        currentPrice,
        totalCost,
        pnlPercent: parseApiDecimal(it.pnlPercent, 0),
      }
    })
    .filter((r) => r.value > 0)
    .map((r) => ({ ...r, sharePct: (r.value / totalVal) * 100 }))
    .sort((a, b) => b.value - a.value)

  if (rows.length === 0) {
    return { bar: [], topList: [], fullList: [] }
  }

  const top = rows.slice(0, DISTRIBUTION_TOP_N)
  const tail = rows.slice(DISTRIBUTION_TOP_N)
  const tailPct = tail.reduce((s, r) => s + r.sharePct, 0)

  const bar: DistributionBarSeg[] = top.map((r, i) => ({
    key: r.symbol,
    widthPct: r.sharePct,
    colorClass: DISTRIBUTION_BAR_COLORS[i % DISTRIBUTION_BAR_COLORS.length],
  }))
  if (tailPct > 0.04) {
    bar.push({
      key: '__other__',
      widthPct: tailPct,
      colorClass: 'my-portfolio-dot-other',
    })
  }

  const barSum = bar.reduce((s, b) => s + b.widthPct, 0)
  if (barSum > 0) {
    bar.forEach((b) => {
      b.widthPct = (b.widthPct / barSum) * 100
    })
  }

  const topList: DistributionRow[] = top.map((r, i) => ({
    symbol: r.symbol,
    value: r.value,
    sharePct: r.sharePct,
    quantity: r.quantity,
    totalCost: r.totalCost,
    avgBuyPrice: r.avgBuyPrice,
    currentPrice: r.currentPrice,
    pnlPercent: r.pnlPercent,
    colorClass: DISTRIBUTION_BAR_COLORS[i % DISTRIBUTION_BAR_COLORS.length],
  }))

  const fullList: DistributionRow[] = rows.map((r, i) => ({
    symbol: r.symbol,
    value: r.value,
    sharePct: r.sharePct,
    quantity: r.quantity,
    totalCost: r.totalCost,
    avgBuyPrice: r.avgBuyPrice,
    currentPrice: r.currentPrice,
    pnlPercent: r.pnlPercent,
    colorClass: i < DISTRIBUTION_BAR_COLORS.length ? DISTRIBUTION_BAR_COLORS[i] : 'my-portfolio-dot-other',
  }))

  return { bar, topList, fullList }
}

function buildAllocationCategoryGroups(
  overview: PortfolioOverview | null,
  t: (key: string) => string,
): AllocationCategoryGroup[] {
  if (!overview?.items?.length) return []
  const items = overview.items.filter((it) => parseApiDecimal(it.value, 0) > 0)
  if (items.length === 0) return []
  const tv = Math.max(parseApiDecimal(overview.totalValue, 0), 1e-12)

  const bucket = (it: PortfolioOverviewItem) => {
    const ex = typeof it.exchange === 'string' && it.exchange.trim().length > 0 ? it.exchange.trim() : null
    return ex ?? it.type ?? 'UNKNOWN'
  }

  const map = new Map<string, PortfolioOverviewItem[]>()
  for (const it of items) {
    const k = bucket(it)
    if (!map.has(k)) map.set(k, [])
    map.get(k)!.push(it)
  }

  const groups: AllocationCategoryGroup[] = []
  for (const [key, groupItems] of map.entries()) {
    const gv = groupItems.reduce((s, it) => s + parseApiDecimal(it.value, 0), 0)
    const shareOfPortfolio = (gv / tv) * 100
    const sorted = [...groupItems].sort((a, b) => parseApiDecimal(b.value, 0) - parseApiDecimal(a.value, 0))
    const denom = Math.max(gv, 1e-12)
    const rows: AllocationDonutRow[] = sorted.map((it, i) => {
      const v = parseApiDecimal(it.value, 0)
      return {
        symbol: it.symbol,
        value: v,
        sharePct: (v / denom) * 100,
        portfolioSharePct: (v / tv) * 100,
        colorClass: DISTRIBUTION_BAR_COLORS[i % DISTRIBUTION_BAR_COLORS.length],
      }
    })
    const kEx = `allocation.exchange.${key}`
    const ex = t(kEx)
    const label =
      ex !== kEx
        ? ex
        : (() => {
            const kTy = `allocation.instrumentType.${key}`
            const ty = t(kTy)
            return ty !== kTy ? ty : key
          })()
    groups.push({ key, label, shareOfPortfolio, rows, holdings: groupItems })
  }
  groups.sort((a, b) => b.shareOfPortfolio - a.shareOfPortfolio)
  return groups
}

function computeCategoryDayMetrics(
  holdings: PortfolioOverviewItem[],
  totalValueToday: number,
  totalPriorValue: number | null,
): { valueChange1dPct: number | null; weightChangePp1d: number | null } {
  const tv = Math.max(totalValueToday, 1e-12)
  const pt = totalPriorValue != null && totalPriorValue > 1e-12 ? totalPriorValue : null
  if (pt == null) return { valueChange1dPct: null, weightChangePp1d: null }

  const positive = holdings.filter((h) => parseApiDecimal(h.value, 0) > 0)
  if (positive.length === 0) return { valueChange1dPct: null, weightChangePp1d: null }

  const allPrior = positive.every(
    (h) => h.priorDayValue != null && Number.isFinite(parseApiDecimal(h.priorDayValue, NaN)),
  )
  if (!allPrior) return { valueChange1dPct: null, weightChangePp1d: null }

  const sumToday = positive.reduce((s, h) => s + parseApiDecimal(h.value, 0), 0)
  const sumPrior = positive.reduce((s, h) => s + parseApiDecimal(h.priorDayValue as number, 0), 0)
  if (sumPrior <= 1e-12) return { valueChange1dPct: null, weightChangePp1d: null }

  const valueChange1dPct = ((sumToday - sumPrior) / sumPrior) * 100
  const wToday = (sumToday / tv) * 100
  const wPrior = (sumPrior / pt) * 100
  const weightChangePp1d = wToday - wPrior

  return { valueChange1dPct, weightChangePp1d }
}

function buildInstrumentDonutRows(
  fullList: DistributionRow[],
  formatSymbolLabel: (symbol: string) => string,
): AllocationDonutRow[] {
  return fullList.map((r) => ({
    rowKey: r.symbol,
    symbol: r.symbol,
    displaySymbol: formatSymbolLabel(r.symbol),
    value: r.value,
    sharePct: r.sharePct,
    colorClass: r.colorClass,
  }))
}

function buildCategoryDonutRows(overview: PortfolioOverview | null, t: (key: string) => string): AllocationDonutRow[] {
  if (!overview) return []
  const groups = buildAllocationCategoryGroups(overview, t)
  const tv = Math.max(parseApiDecimal(overview.totalValue, 0), 1e-12)
  const dodRaw = overview.dayOverDayChange
  const priorTotal =
    dodRaw != null && Number.isFinite(parseApiDecimal(dodRaw, NaN)) ? tv - parseApiDecimal(dodRaw, 0) : null

  return groups.map((g, i) => {
    const m = computeCategoryDayMetrics(g.holdings, tv, priorTotal)
    return {
      rowKey: g.key,
      symbol: g.label,
      value: g.rows.reduce((s, r) => s + r.value, 0),
      sharePct: g.shareOfPortfolio,
      colorClass: DISTRIBUTION_BAR_COLORS[i % DISTRIBUTION_BAR_COLORS.length],
      categoryValueChange1dPct: m.valueChange1dPct,
      categoryWeightChangePp1d: m.weightChangePp1d,
    }
  })
}

const sidebarMainKeys = ['dashboard', 'markets', 'portfolio', 'allocation', 'salesAnalysis'] as const
const sidebarSecondaryKeys = ['news', 'analysis', 'targets', 'watchlist', 'settings'] as const
const PORTFOLIO_SECTIONS = new Set<string>([...sidebarMainKeys, ...sidebarSecondaryKeys])

function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() =>
    typeof window !== 'undefined' ? window.matchMedia(query).matches : false,
  )

  useEffect(() => {
    const media = window.matchMedia(query)
    const onChange = () => setMatches(media.matches)
    onChange()
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [query])

  return matches
}
type MarketOptionAssetType = 'STOCK' | 'FX' | 'METAL' | 'CRYPTO' | 'FUND' | 'DEPOSIT' | 'BOND'
type MarketOptionAssetTypeFilter = 'ALL' | MarketOptionAssetType

const MARKET_OPTION_ASSET_TYPE_ORDER: readonly MarketOptionAssetType[] = ['STOCK', 'FX', 'METAL', 'CRYPTO', 'FUND', 'DEPOSIT', 'BOND']

function normalizeMarketOptionAssetType(raw: string | null | undefined, symbol: string): MarketOptionAssetType {
  const type = (raw ?? '').trim().toUpperCase()
  const sym = symbol.trim().toUpperCase()
  if (sym.startsWith('TRBOND') || type === 'BOND' || type === 'EUROBOND' || type === 'BILL' || type === 'TBILL') {
    return 'BOND'
  }
  if (sym.startsWith('FUND_') || type === 'FUND' || type === 'ETF' || type === 'MUTUAL_FUND' || type === 'MUTUAL FUND') {
    return 'FUND'
  }
  if (type === 'DEPOSIT' || sym.startsWith('TLDEP_')) {
    return 'DEPOSIT'
  }
  if (type === 'CRYPTO' || type === 'COIN' || type === 'TOKEN') {
    return 'CRYPTO'
  }
  if (type === 'METAL' || type === 'COMMODITY' || /^(XAU|XAG|XPT|XPD|XCU)/.test(sym)) {
    return 'METAL'
  }
  if (type === 'FX' || type === 'FOREX' || type === 'CURRENCY') {
    return 'FX'
  }
  return 'STOCK'
}

type MarketOption = {
  instrumentId: number
  symbol: string
  name: string
  nativeQuote: string | null
  assetType: MarketOptionAssetType
}

type SellHoldingOption = MarketOption & {
  holdingQty: number
  avgBuyPrice: number
}

const TRADE_PAYMENT_CURRENCIES: readonly TradePaymentCurrency[] = ['TRY', 'USD', 'EUR', 'GBP', 'JPY', 'AED']

function normalizeToTradePaymentCurrency(raw: string | null | undefined): TradePaymentCurrency | null {
  const u = (raw ?? '').trim().toUpperCase()
  if (u === 'USDT') return 'USD'
  if ((TRADE_PAYMENT_CURRENCIES as readonly string[]).includes(u)) {
    return u as TradePaymentCurrency
  }
  return null
}

function tradePaymentCurrencyLabel(c: TradePaymentCurrency): string {
  return c === 'TRY' ? 'TRY (TL)' : c
}

function currencySymbolPrefix(iso: TradePaymentCurrency): string {
  switch (iso) {
    case 'TRY':
      return '₺'
    case 'USD':
      return '$'
    case 'EUR':
      return '€'
    case 'GBP':
      return '£'
    case 'JPY':
      return '¥'
    case 'AED':
      return 'د.إ\u00A0'
    default:
      return ''
  }
}

function formatDecimalForLocale(value: number, language: string, maxFractionDigits: number): string {
  return new Intl.NumberFormat(language, {
    maximumFractionDigits: maxFractionDigits,
    minimumFractionDigits: 0,
  }).format(value)
}

const HISTORY_QUOTE_ISO = new Set(['TRY', 'USD', 'EUR', 'GBP', 'JPY', 'AED'])

/** Listing / quote currency for history row (API `quoteCurrency` when present). */
function resolveHistoryQuoteCurrency(row: TransactionHistoryItem): string {
  const q = row.quoteCurrency?.trim().toUpperCase()
  if (q && HISTORY_QUOTE_ISO.has(q)) return q
  return inferInstrumentQuoteCurrency(row.instrumentSymbol)
}

/**
 * Effective acquisition FX: DB `fx_rate_used` = units of listing currency per 1 unit of payment currency.
 * Shown as "1 USD = 45,37 TRY" when cross; "—" when missing or same currency.
 */
function formatTxFxLegLabel(row: TransactionHistoryItem, language: string): string {
  const rate = parseApiDecimal(row.fxRateUsed, Number.NaN)
  if (!Number.isFinite(rate) || rate <= 0) return '—'
  const pay = normalizeToTradePaymentCurrency(row.inputCurrency)
  const quote = resolveHistoryQuoteCurrency(row)
  if (pay == null) return '—'
  if (pay === quote) return '—'
  const rateStr = formatDecimalForLocale(rate, language, 8)
  return `1 ${pay} = ${rateStr} ${quote}`
}

function formatMoneyPrefixed(
  value: number | null | undefined,
  iso: TradePaymentCurrency,
  language: string,
  maxFractionDigits: number,
): string {
  if (value == null || !Number.isFinite(value)) return '—'
  return `${currencySymbolPrefix(iso)}${formatDecimalForLocale(value, language, maxFractionDigits)}`
}

function buildTradePaymentCurrencyOptions(instrumentDefault: TradePaymentCurrency): TradePaymentCurrency[] {
  const arr = [...TRADE_PAYMENT_CURRENCIES]
  arr.sort((a, b) => {
    if (a === instrumentDefault) return -1
    if (b === instrumentDefault) return 1
    return a.localeCompare(b)
  })
  return arr
}

type InstrumentPerformance = {
  change1D: number | null
  change1M: number | null
  change3M: number | null
  change6M: number | null
  change1Y: number | null
}

function toUtcStartOfDay(input: string): string | undefined {
  const raw = input.trim()
  if (!raw) return undefined
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    return `${raw}T00:00:00Z`
  }
  const parts = raw.split(/[./-]/).map((p) => p.trim())
  if (parts.length === 3) {
    const [a, b, c] = parts
    if (a.length === 2 && b.length === 2 && c.length === 4) {
      return `${c}-${b}-${a}T00:00:00Z`
    }
  }
  return undefined
}

/** ISO instant (UTC) → GG.AA.YYYY for short notices. */
/** Market overview may use Yahoo wire symbols (GARAN.IS) while catalog uses GARAN. */
function marketOverviewSymbolMatches(overviewSymbol: string, catalogSymbol: string): boolean {
  const o = overviewSymbol.trim().toUpperCase()
  const c = catalogSymbol.trim().toUpperCase()
  if (!o || !c) return false
  if (o === c) return true
  return o === `${c}.IS` || c === `${o}.IS`
}

function formatIsoDateUtcToTrLabel(iso: string): string {
  const day = iso.trim().slice(0, 10)
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(day)
  if (!m) return day
  return `${m[3]}.${m[2]}.${m[1]}`
}

function applyPastPreviewUi(
  preview: TradePreview,
  purchaseMode: PurchaseMode,
  setAcquiredAt: (v: string) => void,
  setPastDateRollNotice: (v: string | null) => void,
) {
  if (purchaseMode !== 'PAST') return
  const eff = preview.effectiveAcquiredAt
  if (typeof eff === 'string' && eff.length >= 10) {
    setAcquiredAt(eff.slice(0, 10))
  }
  if (preview.pastDateRolledToEarliestData === true && typeof eff === 'string' && eff.length >= 10) {
    setPastDateRollNotice(formatIsoDateUtcToTrLabel(eff))
  } else {
    setPastDateRollNotice(null)
  }
}

function extractApiErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error != null) {
    const e = error as {
      message?: string
      response?: { data?: { error?: { message?: string } } }
    }
    const apiMessage = e.response?.data?.error?.message
    if (typeof apiMessage === 'string' && apiMessage.trim().length > 0) {
      return apiMessage
    }
    if (typeof e.message === 'string' && e.message.trim().length > 0) {
      return e.message
    }
  }
  return ''
}

function formatDepositMaturityLabel(
  symbol: string | null | undefined,
  fallbackName: string | null | undefined,
  t: (key: string, options?: Record<string, unknown>) => string,
): string {
  const maturityCode = getTlDepositMaturityCodeFromInstrumentSymbol(symbol)
  if (!maturityCode) return fallbackName?.trim() || symbol?.trim() || '—'
  return t(`marketsAdd.depositMaturity.${maturityCode}`, {
    defaultValue: fallbackName?.trim() || symbol?.trim() || maturityCode,
  })
}

function SidebarItemIcon({ item }: { item: string }) {
  switch (item) {
    case 'dashboard':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <rect x="3" y="3" width="8" height="8" rx="2" />
          <rect x="13" y="3" width="8" height="5" rx="2" />
          <rect x="13" y="10" width="8" height="11" rx="2" />
          <rect x="3" y="13" width="8" height="8" rx="2" />
        </svg>
      )
    case 'markets':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M4 18h16" />
          <path d="M6 15l3-4 3 2 4-6 2 2" />
          <circle cx="6" cy="15" r="1.2" />
          <circle cx="9" cy="11" r="1.2" />
          <circle cx="12" cy="13" r="1.2" />
          <circle cx="16" cy="7" r="1.2" />
          <circle cx="18" cy="9" r="1.2" />
        </svg>
      )
    case 'portfolio':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <rect x="3" y="6" width="18" height="14" rx="3" />
          <path d="M3 11h18" />
          <path d="M8 3h8" />
        </svg>
      )
    case 'allocation':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="1.6" opacity="0.35" />
          <path
            d="M12 3a9 9 0 0 1 8.48 5.95L12 12z"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinejoin="round"
          />
          <path
            d="M20.48 8.95A9 9 0 0 1 12 21V12z"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinejoin="round"
            opacity="0.85"
          />
          <path
            d="M12 21A9 9 0 0 1 3.52 8.95L12 12z"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinejoin="round"
            opacity="0.55"
          />
        </svg>
      )
    case 'salesAnalysis':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M4 19h16" />
          <path d="M7 16V9" />
          <path d="M12 16V5" />
          <path d="M17 16v-4" />
        </svg>
      )
    case 'news':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <rect x="4" y="4" width="16" height="16" rx="2" />
          <path d="M8 8h8" />
          <path d="M8 12h8" />
          <path d="M8 16h5" />
        </svg>
      )
    case 'analysis':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M4 19V5" />
          <path d="M4 19h16" />
          <rect x="7" y="12" width="3" height="5" rx="1" />
          <rect x="12" y="9" width="3" height="8" rx="1" />
          <rect x="17" y="6" width="3" height="11" rx="1" />
        </svg>
      )
    case 'targets':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <circle cx="12" cy="12" r="8" />
          <circle cx="12" cy="12" r="4" />
          <circle cx="12" cy="12" r="1.2" />
        </svg>
      )
    case 'watchlist':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M6 4h12v16l-6-3-6 3z" />
        </svg>
      )
    case 'settings':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <circle cx="12" cy="12" r="3.2" />
          <path d="M19.4 15a1 1 0 0 0 .2 1.1l.1.1a1 1 0 0 1 0 1.4l-1 1a1 1 0 0 1-1.4 0l-.1-.1a1 1 0 0 0-1.1-.2 1 1 0 0 0-.6.9V20a1 1 0 0 1-1 1h-1.4a1 1 0 0 1-1-1v-.1a1 1 0 0 0-.6-.9 1 1 0 0 0-1.1.2l-.1.1a1 1 0 0 1-1.4 0l-1-1a1 1 0 0 1 0-1.4l.1-.1a1 1 0 0 0 .2-1.1 1 1 0 0 0-.9-.6H4a1 1 0 0 1-1-1v-1.4a1 1 0 0 1 1-1h.1a1 1 0 0 0 .9-.6 1 1 0 0 0-.2-1.1l-.1-.1a1 1 0 0 1 0-1.4l1-1a1 1 0 0 1 1.4 0l.1.1a1 1 0 0 0 1.1.2 1 1 0 0 0 .6-.9V4a1 1 0 0 1 1-1h1.4a1 1 0 0 1 1 1v.1a1 1 0 0 0 .6.9 1 1 0 0 0 1.1-.2l.1-.1a1 1 0 0 1 1.4 0l1 1a1 1 0 0 1 0 1.4l-.1.1a1 1 0 0 0-.2 1.1 1 1 0 0 0 .9.6H20a1 1 0 0 1 1 1v1.4a1 1 0 0 1-1 1h-.1a1 1 0 0 0-.9.6z" />
        </svg>
      )
    default:
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <circle cx="12" cy="12" r="8" />
        </svg>
      )
  }
}

function formatAcquisitionFxCell(n: number | null | undefined, fmt: Intl.NumberFormat): string {
  if (n == null || !Number.isFinite(n)) return '—'
  return fmt.format(n)
}

function AcquisitionFxPanel({
  snap,
  purchaseMode,
}: {
  snap: AcquisitionFxRatesSnapshot
  purchaseMode: PurchaseMode
}) {
  const { t, i18n } = useTranslation('portfolio')
  const fmt = useMemo(() => new Intl.NumberFormat(i18n.language, { maximumFractionDigits: 6 }), [i18n.language])
  const rows: [string, number | null | undefined][] = [
    ['USDTRY', snap.usdTry],
    ['EURTRY', snap.eurTry],
    ['GBPTRY', snap.gbpTry],
    ['JPYTRY', snap.jpyTry],
    ['AEDTRY', snap.aedTry],
    ['EURUSD', snap.eurUsd],
    ['GBPUSD', snap.gbpUsd],
    ['JPYUSD', snap.jpyUsd],
  ]
  return (
    <div className="my-portfolio-acquisition-fx">
      <h5>{t('marketsAdd.fxPanel.title')}</h5>
      <div className="my-portfolio-acquisition-fx-asof">
        <span>{purchaseMode === 'PAST' ? t('marketsAdd.fxPanel.sourcePast') : t('marketsAdd.fxPanel.sourceNow')}</span>
        <code>{snap.fxAsOfIso}</code>
      </div>
      <table className="my-portfolio-acquisition-fx-table">
        <thead>
          <tr>
            <th>{t('marketsAdd.fxPanel.pair')}</th>
            <th>{t('marketsAdd.fxPanel.mid')}</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(([k, v]) => (
            <tr key={k}>
              <td>{k}</td>
              <td>{formatAcquisitionFxCell(v, fmt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="my-portfolio-acquisition-fx-note">
        {t('marketsAdd.fxPanel.note')}
      </p>
    </div>
  )
}

export function MyPortfolioPage() {
  const { t, i18n } = useTranslation('portfolio')
  const [searchParams, setSearchParams] = useSearchParams()
  const [isDarkTheme, setIsDarkTheme] = useState(() =>
    typeof document !== 'undefined' && document.documentElement.getAttribute('data-theme') === 'dark',
  )
  const [portfolios, setPortfolios] = useState<Portfolio[]>([])
  const [selectedPortfolioId, setSelectedPortfolioId] = useState<number | null>(() =>
    portfolioIdFromSearchParams(searchParams),
  )
  const [tradeTargetPortfolioId, setTradeTargetPortfolioId] = useState<number | null>(null)
  const [showCreatePortfolioModal, setShowCreatePortfolioModal] = useState(false)
  const [newPortfolioName, setNewPortfolioName] = useState('')
  const [portfolioActionLoading, setPortfolioActionLoading] = useState(false)
  const [portfolioActionError, setPortfolioActionError] = useState<string | null>(null)
  const [portfolioSettingsError, setPortfolioSettingsError] = useState<string | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<{ id: number; name: string } | null>(null)
  const [deletePortfolioSubmitting, setDeletePortfolioSubmitting] = useState(false)
  const [amountsHiddenSaving, setAmountsHiddenSaving] = useState(false)
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const isMobileNav = useMediaQuery('(max-width: 920px)')
  const sectionFromUrl = searchParams.get('section')
  const [activeSection, setActiveSection] = useState<string>(() =>
    sectionFromUrl && PORTFOLIO_SECTIONS.has(sectionFromUrl) ? sectionFromUrl : 'dashboard',
  )

  const selectPortfolio = useCallback(
    (id: number) => {
      setSelectedPortfolioId(id)
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          if (id === ALL_PORTFOLIOS_ID) {
            next.delete('portfolio')
          } else {
            next.set('portfolio', String(id))
          }
          return next
        },
        { replace: true },
      )
    },
    [setSearchParams],
  )

  const selectPortfolioSection = (section: string) => {
    setActiveSection(section)
    if (isMobileNav) {
      setMobileNavOpen(false)
    }
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        if (section === 'dashboard') {
          next.delete('section')
        } else {
          next.set('section', section)
        }
        return next
      },
      { replace: true },
    )
  }

  useEffect(() => {
    if (!isMobileNav) {
      setMobileNavOpen(false)
    }
  }, [isMobileNav])

  useEffect(() => {
    if (!isMobileNav || !mobileNavOpen) {
      return undefined
    }

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMobileNavOpen(false)
      }
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', onKeyDown)

    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', onKeyDown)
    }
  }, [isMobileNav, mobileNavOpen])

  useEffect(() => {
    const fromUrl = searchParams.get('section')
    if (fromUrl && PORTFOLIO_SECTIONS.has(fromUrl)) {
      setActiveSection(fromUrl)
      return
    }
    if (!fromUrl) {
      setActiveSection('dashboard')
    }
  }, [searchParams])
  const [marketOptions, setMarketOptions] = useState<MarketOption[]>([])
  const [marketLoading, setMarketLoading] = useState(false)
  const [marketCatalogError, setMarketCatalogError] = useState<string | null>(null)
  const [tradeSaving, setTradeSaving] = useState(false)
  const [tradeError, setTradeError] = useState<string | null>(null)
  /** Alim onizlemesi (/api/trades/preview) hatalari — NOW modunda da gorunur (onceki: catch sessizdi). */
  const [tradePreviewError, setTradePreviewError] = useState<string | null>(null)
  /** Son basarili /api/trades/preview yaniti (onizleme panelinde kur tablosu icin). */
  const [lastTradePreview, setLastTradePreview] = useState<TradePreview | null>(null)
  const [tradeSuccess, setTradeSuccess] = useState<string | null>(null)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [historyPagedEndpointAvailable, setHistoryPagedEndpointAvailable] = useState(true)
  const [history, setHistory] = useState<TransactionHistoryItem[]>([])
  const [historyPage, setHistoryPage] = useState(0)
  const [historySize] = useState(20)
  const [historyTotalPages, setHistoryTotalPages] = useState(0)
  const [historyTotalElements, setHistoryTotalElements] = useState(0)
  const [historyFilters, setHistoryFilters] = useState<TransactionHistoryFilters>({
    symbol: '',
    type: '',
    purchaseMode: '',
    inputCurrency: '',
    fromDate: '',
    toDate: '',
  })
  const [appliedHistoryFilters, setAppliedHistoryFilters] = useState<TransactionHistoryFilters>({
    symbol: '',
    type: '',
    purchaseMode: '',
    inputCurrency: '',
    fromDate: '',
    toDate: '',
  })
  const [overview, setOverview] = useState<PortfolioOverview | null>(null)
  const [performanceSeries, setPerformanceSeries] = useState<PortfolioPerformanceSeries | null>(null)
  const [performanceSeriesHydrated, setPerformanceSeriesHydrated] = useState(false)
  const [valueChartMetric, setValueChartMetric] = useState<PortfolioChartMetric>('value')
  const [valueChartRange, setValueChartRange] = useState<ValueChartRange>('1w')
  const [tradeFlow, setTradeFlow] = useState<PortfolioTradeFlow | null>(null)
  const [tradeFlowHydrated, setTradeFlowHydrated] = useState(false)
  const [recentTxPreview, setRecentTxPreview] = useState<TransactionHistoryItem[]>([])
  const [recentTxLoading, setRecentTxLoading] = useState(false)
  const [allocationSortKey, setAllocationSortKey] = useState<AllocationSortKey>('value')
  const [allocationSortDir, setAllocationSortDir] = useState<AllocationSortDir>('desc')
  const [, setValueSnapshots] = useState<PortfolioValueSnapshot[]>([])
  const [selectedInstrumentId, setSelectedInstrumentId] = useState<number | null>(null)
  const [selectedAssetType, setSelectedAssetType] = useState<MarketOptionAssetTypeFilter>('ALL')
  const [instrumentQuery, setInstrumentQuery] = useState('')
  const [purchaseMode, setPurchaseMode] = useState<PurchaseMode>('NOW')
  const [inputMode, setInputMode] = useState<TradeInputMode>('LOTS')
  const [lots, setLots] = useState('1')
  const [amount, setAmount] = useState('')
  const [tradePaymentCurrency, setTradePaymentCurrency] = useState<TradePaymentCurrency>('USD')
  const [acquiredAt, setAcquiredAt] = useState('')
  const [unitPrice, setUnitPrice] = useState('')
  const [unitPriceUsed, setUnitPriceUsed] = useState<number | null>(null)
  const [manualUnitPriceRequired, setManualUnitPriceRequired] = useState(false)
  const [pastDateRollNotice, setPastDateRollNotice] = useState<string | null>(null)
  const [depositRatePreview, setDepositRatePreview] = useState<TlDepositIndexLatestResponse | null>(null)
  const [depositRateLoading, setDepositRateLoading] = useState(false)
  const [depositPastDateError, setDepositPastDateError] = useState<string | null>(null)
  const [lastEditedField, setLastEditedField] = useState<'lots' | 'amount'>('lots')
  const [isPreviewStep, setIsPreviewStep] = useState(false)
  const [tradeSide, setTradeSide] = useState<'BUY' | 'SELL'>('BUY')
  const isSellFlow = tradeSide === 'SELL'
  const [sellHoldingsOverview, setSellHoldingsOverview] = useState<PortfolioOverview | null>(null)
  const [sellHoldingsLoading, setSellHoldingsLoading] = useState(false)
  const [deleteTxConfirm, setDeleteTxConfirm] = useState<TransactionHistoryItem | null>(null)
  const [deleteTxSubmitting, setDeleteTxSubmitting] = useState(false)
  const [deleteTxError, setDeleteTxError] = useState<string | null>(null)
  const [salesAnalysisLoading, setSalesAnalysisLoading] = useState(false)
  const [salesAnalysisError, setSalesAnalysisError] = useState<string | null>(null)
  const [salesAnalysisRows, setSalesAnalysisRows] = useState<SalesAnalysisItem[]>([])
  const [salesAnalysisPage, setSalesAnalysisPage] = useState(0)
  const [salesAnalysisTotalPages, setSalesAnalysisTotalPages] = useState(0)
  const [salesAnalysisTotalElements, setSalesAnalysisTotalElements] = useState(0)
  const [salesAnalysisFilters, setSalesAnalysisFilters] = useState<SalesAnalysisFilters>({
    symbol: '',
    fromDate: '',
    toDate: '',
  })
  const [appliedSalesAnalysisFilters, setAppliedSalesAnalysisFilters] = useState<SalesAnalysisFilters>({
    symbol: '',
    fromDate: '',
    toDate: '',
  })
  const [instrumentPerformance, setInstrumentPerformance] = useState<InstrumentPerformance>({
    change1D: null,
    change1M: null,
    change3M: null,
    change6M: null,
    change1Y: null,
  })
  useEffect(() => {
    if (purchaseMode === 'NOW') {
      setPastDateRollNotice(null)
    }
  }, [purchaseMode])

  useEffect(() => {
    setLastTradePreview(null)
  }, [selectedInstrumentId])

  useDocumentTitle(t('titleDoc'))
  const { currency: displayCurrency } = useAppPreferences()
  /** Portfolio overview, dağılım, trade-flow ve ilgili market istekleri için `X-Currency` (üstteki para seçimi). */
  const valuationCurrency = displayCurrency

  const currencyFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 2,
      }),
    [i18n.language],
  )

  const dashboardCurrency = overview?.currency ?? 'USD'
  const dashboardCurrencyFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency: dashboardCurrency,
        maximumFractionDigits: 2,
      }),
    [i18n.language, dashboardCurrency],
  )

  const dashboardDayChangeFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency: dashboardCurrency,
        maximumFractionDigits: 2,
        signDisplay: 'exceptZero',
      }),
    [i18n.language, dashboardCurrency],
  )

  const dashboardPctFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        maximumFractionDigits: 2,
        minimumFractionDigits: 0,
        signDisplay: 'exceptZero',
      }),
    [i18n.language],
  )

  const percentFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        maximumFractionDigits: 2,
        signDisplay: 'always',
      }),
    [i18n.language],
  )

  const sharePctDisplay = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        maximumFractionDigits: 1,
        minimumFractionDigits: 0,
      }),
    [i18n.language],
  )

  const isAggregatePortfolioView = selectedPortfolioId === ALL_PORTFOLIOS_ID

  const effectiveTradePortfolioId = useMemo(() => {
    if (isAggregatePortfolioView) return tradeTargetPortfolioId
    return selectedPortfolioId != null && selectedPortfolioId > 0 ? selectedPortfolioId : null
  }, [isAggregatePortfolioView, tradeTargetPortfolioId, selectedPortfolioId])

  const overviewApiPortfolioId = useMemo(() => {
    if (isAggregatePortfolioView) return null
    return selectedPortfolioId != null && selectedPortfolioId > 0 ? selectedPortfolioId : null
  }, [isAggregatePortfolioView, selectedPortfolioId])

  const selectedPortfolio = useMemo(
    () =>
      selectedPortfolioId != null && selectedPortfolioId > 0
        ? portfolios.find((p) => p.id === selectedPortfolioId) ?? null
        : null,
    [portfolios, selectedPortfolioId],
  )
  const hideMoney =
    isAggregatePortfolioView
      ? portfolios.some((p) => p.amountsHidden === true)
      : selectedPortfolio?.amountsHidden === true

  const displayDashboardCurrencyFormat = useMemo(
    () => (hideMoney ? maskedNumberFormatShim() : dashboardCurrencyFormat),
    [hideMoney, dashboardCurrencyFormat],
  )

  const displayDashboardDayChangeFormat = useMemo(
    () => (hideMoney ? maskedNumberFormatShim() : dashboardDayChangeFormat),
    [hideMoney, dashboardDayChangeFormat],
  )

  const valueChartCurrencyCode = (performanceSeries?.currency ?? valuationCurrency).toUpperCase()
  const valueChartPointsLabel = useMemo(() => {
    const points = performanceSeries?.points ?? []
    if (points.length === 0) {
      return null
    }
    if (valueChartRange === 'all') {
      return t('valueChart.points', { count: points.length })
    }
    const endMs = Date.parse(`${points[points.length - 1]!.day}T12:00:00Z`)
    if (!Number.isFinite(endMs)) {
      return t('valueChart.points', { count: points.length })
    }
    const startMs = Math.max(0, endMs - RANGE_TO_MS[valueChartRange] + 86_400_000)
    const visibleCount = points.filter((point) => Date.parse(`${point.day}T12:00:00Z`) >= startMs).length
    return t('valueChart.points', { count: Math.max(1, visibleCount) })
  }, [performanceSeries, t, valueChartRange])
  const valueChartRangeRailRef = useRef<HTMLDivElement | null>(null)
  const valueChartRangeDragRef = useRef({
    pointerId: -1,
    startX: 0,
    startScrollLeft: 0,
    dragging: false,
    suppressClick: false,
  })

  const handleValueChartRangePointerDown = useCallback((event: ReactPointerEvent<HTMLDivElement>) => {
    const rail = valueChartRangeRailRef.current
    if (!rail) return
    valueChartRangeDragRef.current.pointerId = event.pointerId
    valueChartRangeDragRef.current.startX = event.clientX
    valueChartRangeDragRef.current.startScrollLeft = rail.scrollLeft
    valueChartRangeDragRef.current.dragging = false
    valueChartRangeDragRef.current.suppressClick = false
  }, [])

  const handleValueChartRangePointerMove = useCallback((event: ReactPointerEvent<HTMLDivElement>) => {
    const rail = valueChartRangeRailRef.current
    if (!rail) return
    const drag = valueChartRangeDragRef.current
    if (drag.pointerId !== event.pointerId) return
    const deltaX = event.clientX - drag.startX
    if (!drag.dragging) {
      if (Math.abs(deltaX) <= 8) {
        return
      }
      drag.dragging = true
      drag.suppressClick = true
      rail.setPointerCapture?.(event.pointerId)
    }
    event.preventDefault()
    rail.scrollLeft = drag.startScrollLeft - deltaX
  }, [])

  const handleValueChartRangePointerEnd = useCallback((event: ReactPointerEvent<HTMLDivElement>) => {
    const rail = valueChartRangeRailRef.current
    const drag = valueChartRangeDragRef.current
    if (drag.pointerId !== event.pointerId) return
    if (rail?.hasPointerCapture?.(event.pointerId)) {
      rail.releasePointerCapture(event.pointerId)
    }
    drag.pointerId = -1
    drag.dragging = false
  }, [])

  const handleValueChartRangeClickCapture = useCallback((event: ReactMouseEvent<HTMLDivElement>) => {
    if (!valueChartRangeDragRef.current.suppressClick) return
    valueChartRangeDragRef.current.suppressClick = false
    event.preventDefault()
    event.stopPropagation()
  }, [])

  const formatTxMoney = useCallback(
    (row: TransactionHistoryItem) => {
      if (hideMoney) return MASKED_MONEY_LABEL
      // Alımlarda API hem kotasyon tutarını (totalAmount + quoteCurrency) hem ödeme cinsini
      // (inputAmount + inputCurrency) saklar; işlem geçmişinde "Maliyet" ödenen tutar olmalı.
      if (row.type === 'BUY') {
        const payment = normalizeToTradePaymentCurrency(row.inputCurrency)
        const inputAmt = parseApiDecimal(row.inputAmount, Number.NaN)
        if (payment != null && Number.isFinite(inputAmt) && inputAmt > 0) {
          return formatMoneyPrefixed(inputAmt, payment, i18n.language, 2)
        }
      }
      const qc = row.quoteCurrency?.trim().toUpperCase()
      const rawCur =
        qc === 'TRY' || qc === 'USD' || qc === 'EUR' ? qc : inferInstrumentQuoteCurrency(row.instrumentSymbol)
      const cur = rawCur === 'TRY' || rawCur === 'USD' || rawCur === 'EUR' ? rawCur : dashboardCurrency
      const amt = parseApiDecimal(row.totalAmount, 0)
      try {
        return new Intl.NumberFormat(i18n.language, {
          style: 'currency',
          currency: cur,
          maximumFractionDigits: 2,
        }).format(amt)
      } catch {
        return new Intl.NumberFormat(i18n.language, {
          style: 'currency',
          currency: dashboardCurrency,
          maximumFractionDigits: 2,
        }).format(amt)
      }
    },
    [hideMoney, i18n.language, dashboardCurrency],
  )

  const distribution = useMemo(() => buildPortfolioDistribution(overview), [overview])

  const allocationTableRows = useMemo(() => {
    const rows = [...distribution.fullList]
    const mul = allocationSortDir === 'asc' ? 1 : -1
    rows.sort((a, b) => {
      switch (allocationSortKey) {
        case 'symbol':
          return mul * a.symbol.localeCompare(b.symbol, undefined, { sensitivity: 'base', numeric: true })
        case 'quantity':
          return mul * (a.quantity - b.quantity)
        case 'cost':
          return mul * (a.totalCost - b.totalCost)
        case 'avgBuyPrice':
          return mul * (a.avgBuyPrice - b.avgBuyPrice)
        case 'currentPrice':
          return mul * (a.currentPrice - b.currentPrice)
        case 'weight':
          return mul * (a.sharePct - b.sharePct)
        case 'value':
          return mul * (a.value - b.value)
        case 'pnl':
          return mul * (a.pnlPercent - b.pnlPercent)
        default:
          return 0
      }
    })
    return rows
  }, [distribution.fullList, allocationSortKey, allocationSortDir])

  const setAllocationColumnSort = useCallback((key: AllocationSortKey) => {
    setAllocationSortKey((prevKey) => {
      if (prevKey === key) {
        setAllocationSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
        return prevKey
      }
      setAllocationSortDir(key === 'symbol' ? 'asc' : 'desc')
      return key
    })
  }, [])

  const instrumentDonutRows = useMemo(
    () => buildInstrumentDonutRows(distribution.fullList, (symbol) => formatDepositMaturityLabel(symbol, null, t)),
    [distribution.fullList, t],
  )

  const categoryDonutRows = useMemo(() => buildCategoryDonutRows(overview, t), [overview, t])

  const tradeFlowStats = useMemo(() => tradeFlowTotals(tradeFlow?.points ?? []), [tradeFlow])

  const tradeFlowFark = useMemo(() => {
    const { buy, sell } = tradeFlowStats
    if (!tradeFlowHydrated) return { kind: 'loading' as const }
    if (buy <= DOD_EPS && sell <= DOD_EPS) return { kind: 'empty' as const }
    if (buy > DOD_EPS && sell <= DOD_EPS) return { kind: 'empty' as const }
    if (buy > DOD_EPS) {
      const pct = ((sell - buy) / buy) * 100
      return { kind: 'pct' as const, pct }
    }
    return { kind: 'infinity' as const }
  }, [tradeFlowStats, tradeFlowHydrated])

  useEffect(() => {
    if (typeof document === 'undefined') return
    const root = document.documentElement
    const sync = () => setIsDarkTheme(root.getAttribute('data-theme') === 'dark')
    sync()
    const observer = new MutationObserver(sync)
    observer.observe(root, { attributes: true, attributeFilter: ['data-theme'] })
    return () => observer.disconnect()
  }, [])

  const refreshPortfolios = useCallback(async () => {
    const rows = await getPortfolios()
    setPortfolios(rows)
    if (rows.length === 0) {
      setSelectedPortfolioId(null)
      setShowCreatePortfolioModal(true)
      return
    }
    const urlPreferred = portfolioIdFromSearchParams(searchParams)
    setSelectedPortfolioId((prev) => resolvePortfolioSelection(prev ?? urlPreferred, rows))
  }, [searchParams])

  useEffect(() => {
    if (portfolios.length === 0) {
      return
    }
    const resolved = resolvePortfolioSelection(portfolioIdFromSearchParams(searchParams), portfolios)
    setSelectedPortfolioId((prev) => (prev === resolved ? prev : resolved))
  }, [searchParams, portfolios])

  useEffect(() => {
    if (!isAggregatePortfolioView) {
      return
    }
    setTradeTargetPortfolioId((prev) =>
      prev != null && portfolios.some((p) => p.id === prev) ? prev : portfolios[0]?.id ?? null,
    )
  }, [isAggregatePortfolioView, portfolios])

  useEffect(() => {
    void refreshPortfolios()
  }, [refreshPortfolios])

  useEffect(() => {
    if (activeSection !== 'markets') {
      setMarketLoading(false)
      return
    }
    let cancelled = false
    setMarketLoading(true)
    setMarketCatalogError(null)
    void (async () => {
      try {
        const [ovRes, catRes] = await Promise.allSettled([
          fetchMarketOverview({
            page: 0,
            size: 1000,
            category: 'all',
            sort: 'symbol,asc',
            displayCurrency: valuationCurrency,
          }),
          getInstrumentsCatalogForTradePicker(),
        ])
        if (cancelled) return
        const overview = ovRes.status === 'fulfilled' ? ovRes.value : EMPTY_MARKET_OVERVIEW
        const catalog = catRes.status === 'fulfilled' ? catRes.value : []
        const catalogFail =
          catRes.status === 'rejected' ? extractApiErrorMessage(catRes.reason) : ''
        const overviewFail =
          ovRes.status === 'rejected' ? extractApiErrorMessage(ovRes.reason) : ''

        const quoteBySymbol = new Map<string, 'TRY' | 'USD'>()
        for (const row of overview.content) {
          const sym = row.symbol?.trim().toUpperCase()
          if (!sym) continue
          const raw = (row.nativeQuote ?? '').trim().toUpperCase()
          if (raw === 'TRY' || raw === 'USD') {
            quoteBySymbol.set(sym, raw)
            if (sym.endsWith('.IS')) {
              quoteBySymbol.set(sym.slice(0, -3), raw)
            }
          }
        }

        let options: MarketOption[]
        if (catalog.length > 0) {
          options = catalog.map((inst) => {
            const fromWire =
              quoteBySymbol.get(inst.symbol) ?? quoteBySymbol.get(`${inst.symbol}.IS`) ?? null
            const nativeQuote: string =
              fromWire ?? inferNativeQuote(inst.symbol, inst.type, null, inst.exchange || null)
            return {
              instrumentId: inst.id,
              symbol: inst.symbol,
              name: inst.name,
              nativeQuote,
              assetType: normalizeMarketOptionAssetType(inst.type, inst.symbol),
            }
          })
        } else {
          options = overview.content
            .filter((row) => row.instrumentId != null)
            .map((row) => ({
              instrumentId: row.instrumentId as number,
              symbol: row.symbol,
              name: row.name,
              nativeQuote:
                (row.nativeQuote != null && String(row.nativeQuote).trim() !== ''
                  ? String(row.nativeQuote).trim().toUpperCase()
                  : null) ??
                inferNativeQuote(row.symbol, row.category, null, row.exchange ?? null),
              assetType: normalizeMarketOptionAssetType(row.category, row.symbol),
            }))
        }

        const bySymbol = new Map<string, MarketOption>(options.map((o) => [o.symbol, o]))
        for (const row of overview.content) {
          if (row.instrumentId == null) continue
          const sym = row.symbol?.trim().toUpperCase()
          if (!sym || bySymbol.has(sym)) continue
          const nativeQuote: string =
            (row.nativeQuote != null && String(row.nativeQuote).trim() !== ''
              ? String(row.nativeQuote).trim().toUpperCase()
              : null) ?? inferNativeQuote(row.symbol, row.category, null, row.exchange ?? null)
          bySymbol.set(sym, {
            instrumentId: row.instrumentId as number,
            symbol: sym,
            name: row.name,
            nativeQuote,
            assetType: normalizeMarketOptionAssetType(row.category, sym),
          })
        }
        options = Array.from(bySymbol.values())

        if (cancelled) return
        options.sort((a, b) => a.symbol.localeCompare(b.symbol, undefined, { sensitivity: 'base', numeric: true }))
        setMarketOptions(options)
        if (options.length === 0) {
          const hint = [catalogFail, overviewFail].filter(Boolean).join(' · ')
          setMarketCatalogError(
            hint ||
              t('marketsAdd.errors.instrumentListLoadFailed'),
          )
        } else {
          setMarketCatalogError(null)
        }
      } finally {
        if (!cancelled) {
          setMarketLoading(false)
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [activeSection, valuationCurrency, t])

  const sellHoldingOptions = useMemo((): SellHoldingOption[] => {
    if (!sellHoldingsOverview?.items?.length) return []
    return sellHoldingsOverview.items
      .filter((item) => item.quantity > 0)
      .map((item) => {
        const fromCatalog = marketOptions.find((m) => m.instrumentId === item.instrumentId)
        if (fromCatalog) {
          return {
            ...fromCatalog,
            holdingQty: item.quantity,
            avgBuyPrice: item.avgBuyPrice,
          }
        }
        return {
          instrumentId: item.instrumentId,
          symbol: item.symbol,
          name: item.name,
          nativeQuote: null,
          assetType: normalizeMarketOptionAssetType(item.type, item.symbol),
          holdingQty: item.quantity,
          avgBuyPrice: item.avgBuyPrice,
        }
      })
  }, [sellHoldingsOverview, marketOptions])

  const selectedInstrument = useMemo((): MarketOption | null => {
    if (selectedInstrumentId == null) return null
    if (isSellFlow) {
      return sellHoldingOptions.find((item) => item.instrumentId === selectedInstrumentId) ?? null
    }
    return marketOptions.find((item) => item.instrumentId === selectedInstrumentId) ?? null
  }, [isSellFlow, sellHoldingOptions, marketOptions, selectedInstrumentId])

  const selectedSellHolding = useMemo((): SellHoldingOption | null => {
    if (!isSellFlow || selectedInstrumentId == null) return null
    return sellHoldingOptions.find((item) => item.instrumentId === selectedInstrumentId) ?? null
  }, [isSellFlow, sellHoldingOptions, selectedInstrumentId])

  const sellOverviewItem = useMemo(() => {
    if (!isSellFlow || selectedInstrumentId == null) return null
    return sellHoldingsOverview?.items.find((item) => item.instrumentId === selectedInstrumentId) ?? null
  }, [isSellFlow, selectedInstrumentId, sellHoldingsOverview])

  const isTlDepositInstrument = useMemo(() => {
    if (selectedInstrumentId == null) return false
    const fromMarket = marketOptions.find((item) => item.instrumentId === selectedInstrumentId)
    if (fromMarket) return fromMarket.assetType === 'DEPOSIT'
    const fromHold = sellHoldingsOverview?.items.find((item) => item.instrumentId === selectedInstrumentId)
    return fromHold?.type === 'DEPOSIT'
  }, [marketOptions, selectedInstrumentId, sellHoldingsOverview])
  const tlDepositMaturityCode = useMemo(
    () => getTlDepositMaturityCodeFromInstrumentSymbol(selectedInstrument?.symbol),
    [selectedInstrument?.symbol],
  )

  /** Enstrüman birim fiyatının kotasyon para birimi; önizleme sonrası API `instrumentQuoteCurrency` öncelikli. */
  const instrumentQuoteCurrencyCode = useMemo((): TradePaymentCurrency => {
    const fromPreview = normalizeToTradePaymentCurrency(lastTradePreview?.instrumentQuoteCurrency)
    if (fromPreview) return fromPreview
    return normalizeToTradePaymentCurrency(selectedInstrument?.nativeQuote) ?? 'USD'
  }, [lastTradePreview?.instrumentQuoteCurrency, selectedInstrument])

  const instrumentDefaultPaymentCurrency = useMemo((): TradePaymentCurrency => {
    const nq = selectedInstrument?.nativeQuote?.trim().toUpperCase()
    if (nq === 'TRY') return 'TRY'
    const mapped = normalizeToTradePaymentCurrency(nq)
    if (mapped) return mapped
    return normalizeToTradePaymentCurrency(displayCurrency) ?? 'USD'
  }, [selectedInstrument, displayCurrency])

  const tradePaymentCurrencyOptions = useMemo(
    () => buildTradePaymentCurrencyOptions(instrumentDefaultPaymentCurrency),
    [instrumentDefaultPaymentCurrency],
  )

  useEffect(() => {
    setTradePaymentCurrency(instrumentDefaultPaymentCurrency)
  }, [selectedInstrumentId, instrumentDefaultPaymentCurrency])

  useEffect(() => {
    if (!tradePaymentCurrencyOptions.includes(tradePaymentCurrency)) {
      setTradePaymentCurrency(instrumentDefaultPaymentCurrency)
    }
  }, [tradePaymentCurrencyOptions, tradePaymentCurrency, instrumentDefaultPaymentCurrency])

  useEffect(() => {
    if (isTlDepositInstrument && inputMode !== 'AMOUNT') {
      setInputMode('AMOUNT')
      setLastEditedField('amount')
    }
  }, [isTlDepositInstrument, inputMode])

  useEffect(() => {
    if (!isTlDepositInstrument) {
      setDepositPastDateError(null)
      setDepositRatePreview(null)
      setDepositRateLoading(false)
      return
    }
    setUnitPrice('')
    setManualUnitPriceRequired(false)
    if (purchaseMode !== 'PAST' || acquiredAt) {
      setDepositPastDateError(null)
    }
  }, [isTlDepositInstrument, purchaseMode, acquiredAt])

  useEffect(() => {
    if (!isTlDepositInstrument || !tlDepositMaturityCode) {
      setDepositRatePreview(null)
      setDepositRateLoading(false)
      return
    }
    const asOf = purchaseMode === 'PAST' ? acquiredAt || undefined : undefined
    if (purchaseMode === 'PAST' && !asOf) {
      setDepositRatePreview(null)
      setDepositRateLoading(false)
      return
    }
    let cancelled = false
    setDepositRateLoading(true)
    void fetchTlDepositIndexLatest(tlDepositMaturityCode, asOf)
      .then((data) => {
        if (!cancelled) {
          setDepositRatePreview(data)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDepositRatePreview(null)
        }
      })
      .finally(() => {
        if (!cancelled) {
          setDepositRateLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [isTlDepositInstrument, tlDepositMaturityCode, purchaseMode, acquiredAt])

  const loadHistoryPage = useCallback(
    async (page: number, filters: TransactionHistoryFilters, portfolioId: number | null) => {
      setHistoryLoading(true)
      setHistoryError(null)
      try {
        if (historyPagedEndpointAvailable) {
          const response = await getTransactionHistoryPage(page, historySize, filters, portfolioId)
          setHistory(response.content)
          setHistoryPage(response.page)
          setHistoryTotalPages(response.totalPages)
          setHistoryTotalElements(response.totalElements)
          return
        }
        throw new Error('paged-endpoint-disabled')
      } catch (error) {
        // Fallback for temporary backend 400 issues on paged endpoint.
        const allRows = await getTransactionHistory(portfolioId)
        const filtered = allRows.filter((row) => {
          const symbolOk = !filters.symbol || row.instrumentSymbol.toLowerCase().includes(filters.symbol.toLowerCase())
          const typeOk = !filters.type || row.type === filters.type
          const modeOk = !filters.purchaseMode || row.purchaseMode === filters.purchaseMode
          const currencyOk = !filters.inputCurrency || row.inputCurrency === filters.inputCurrency
          const rowDate = new Date(row.acquiredAt ?? row.createdAt)
          const fromOk = !filters.fromDate || rowDate >= new Date(`${filters.fromDate}T00:00:00`)
          const toOk = !filters.toDate || rowDate < new Date(`${filters.toDate}T23:59:59.999`)
          return symbolOk && typeOk && modeOk && currencyOk && fromOk && toOk
        })
        const total = filtered.length
        const totalPages = total === 0 ? 0 : Math.ceil(total / historySize)
        const start = Math.max(page, 0) * historySize
        setHistory(filtered.slice(start, start + historySize))
        setHistoryPage(Math.max(page, 0))
        setHistoryTotalPages(totalPages)
        setHistoryTotalElements(total)
        const message = extractApiErrorMessage(error)
        if (message.toLowerCase().includes('no static resource') || message.toLowerCase().includes('404')) {
          setHistoryPagedEndpointAvailable(false)
        }
        setHistoryError(
          message && message !== 'paged-endpoint-disabled'
            ? `Sayfali endpoint gecici hatali (${message}). Geriye uyumlu listeleme kullanildi.`
            : null,
        )
      } finally {
        setHistoryLoading(false)
      }
    },
    [historySize, historyPagedEndpointAvailable],
  )

  const loadSalesAnalysisPage = useCallback(
    async (page: number, filters: SalesAnalysisFilters, portfolioId: number | null) => {
      setSalesAnalysisLoading(true)
      setSalesAnalysisError(null)
      try {
        const response = await getSalesAnalysisPage(page, historySize, filters, portfolioId)
        setSalesAnalysisRows(response.content)
        setSalesAnalysisPage(response.page)
        setSalesAnalysisTotalPages(response.totalPages)
        setSalesAnalysisTotalElements(response.totalElements)
      } catch (error) {
        setSalesAnalysisRows([])
        setSalesAnalysisTotalPages(0)
        setSalesAnalysisTotalElements(0)
        setSalesAnalysisError(extractApiErrorMessage(error) || t('salesAnalysis.loadError'))
      } finally {
        setSalesAnalysisLoading(false)
      }
    },
    [historySize, t],
  )

  useEffect(() => {
    if (activeSection !== 'portfolio') {
      return
    }
    if (selectedPortfolioId == null) {
      setHistory([])
      setHistoryTotalElements(0)
      setHistoryTotalPages(0)
      return
    }
    const portfolioId = selectedPortfolioId === ALL_PORTFOLIOS_ID ? null : selectedPortfolioId
    void loadHistoryPage(historyPage, appliedHistoryFilters, portfolioId)
  }, [activeSection, historyPage, appliedHistoryFilters, loadHistoryPage, selectedPortfolioId])

  useEffect(() => {
    if (activeSection !== 'salesAnalysis') {
      return
    }
    if (selectedPortfolioId == null) {
      setSalesAnalysisRows([])
      setSalesAnalysisTotalElements(0)
      setSalesAnalysisTotalPages(0)
      return
    }
    const portfolioId = selectedPortfolioId === ALL_PORTFOLIOS_ID ? null : selectedPortfolioId
    void loadSalesAnalysisPage(salesAnalysisPage, appliedSalesAnalysisFilters, portfolioId)
  }, [
    activeSection,
    salesAnalysisPage,
    appliedSalesAnalysisFilters,
    loadSalesAnalysisPage,
    selectedPortfolioId,
  ])

  useEffect(() => {
    if (activeSection !== 'dashboard' || selectedPortfolioId == null) {
      setRecentTxPreview([])
      return
    }
    const historyPid = selectedPortfolioId === ALL_PORTFOLIOS_ID ? null : selectedPortfolioId
    let cancelled = false
    setRecentTxLoading(true)
    void (async () => {
      try {
        const response = await getTransactionHistoryPage(0, RECENT_TX_PREVIEW_SIZE, EMPTY_TX_FILTERS, historyPid)
        if (!cancelled) {
          setRecentTxPreview(sortTransactionsNewestFirst(response.content).slice(0, RECENT_TX_PREVIEW_SIZE))
        }
      } catch {
        try {
          const allRows = await getTransactionHistory(historyPid)
          if (!cancelled) {
            setRecentTxPreview(sortTransactionsNewestFirst(allRows).slice(0, RECENT_TX_PREVIEW_SIZE))
          }
        } catch {
          if (!cancelled) setRecentTxPreview([])
        }
      } finally {
        if (!cancelled) setRecentTxLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [activeSection, selectedPortfolioId])

  useEffect(() => {
    setHistoryPage(0)
  }, [selectedPortfolioId, activeSection])

  useEffect(() => {
    setAllocationSortKey('value')
    setAllocationSortDir('desc')
  }, [selectedPortfolioId])

  useEffect(() => {
    if (selectedPortfolioId == null) {
      setOverview(null)
      return
    }
    if (
      activeSection !== 'dashboard' &&
      activeSection !== 'markets' &&
      activeSection !== 'allocation'
    ) {
      return
    }
    void getMyPortfolioOverview(overviewApiPortfolioId, valuationCurrency).then((data) => setOverview(data)).catch(() => setOverview(null))
  }, [activeSection, overviewApiPortfolioId, valuationCurrency, selectedPortfolioId])

  useEffect(() => {
    if (activeSection !== 'markets' || !isSellFlow || effectiveTradePortfolioId == null) {
      setSellHoldingsOverview(null)
      setSellHoldingsLoading(false)
      return
    }
    if (purchaseMode === 'PAST') {
      const normalizedAsOf = toUtcStartOfDay(acquiredAt)
      if (!normalizedAsOf) {
        setSellHoldingsOverview(null)
        setSellHoldingsLoading(false)
        return
      }
      const asOfDate = normalizedAsOf.slice(0, 10)
      let cancelled = false
      setSellHoldingsLoading(true)
      void getPortfolioHoldingsAsOf(effectiveTradePortfolioId, asOfDate, valuationCurrency)
        .then((data) => {
          if (!cancelled) setSellHoldingsOverview(data)
        })
        .catch(() => {
          if (!cancelled) setSellHoldingsOverview(null)
        })
        .finally(() => {
          if (!cancelled) setSellHoldingsLoading(false)
        })
      return () => {
        cancelled = true
      }
    }
    let cancelled = false
    setSellHoldingsLoading(true)
    void getMyPortfolioOverview(effectiveTradePortfolioId, valuationCurrency)
      .then((data) => {
        if (!cancelled) setSellHoldingsOverview(data)
      })
      .catch(() => {
        if (!cancelled) setSellHoldingsOverview(null)
      })
      .finally(() => {
        if (!cancelled) setSellHoldingsLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [activeSection, isSellFlow, effectiveTradePortfolioId, valuationCurrency, purchaseMode, acquiredAt])

  useEffect(() => {
    if (selectedPortfolioId == null) {
      setPerformanceSeries(null)
      setPerformanceSeriesHydrated(false)
      return
    }
    if (activeSection !== 'dashboard') {
      setPerformanceSeries(null)
      setPerformanceSeriesHydrated(false)
      return
    }
    let cancelled = false
    setPerformanceSeriesHydrated(false)
    void getPortfolioPerformanceSeries(overviewApiPortfolioId, valuationCurrency, 'all')
      .then((data) => {
        if (!cancelled) {
          setPerformanceSeries(data)
          setPerformanceSeriesHydrated(true)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setPerformanceSeries(null)
          setPerformanceSeriesHydrated(true)
        }
      })
    return () => {
      cancelled = true
    }
  }, [activeSection, overviewApiPortfolioId, valuationCurrency, selectedPortfolioId])

  useEffect(() => {
    if (selectedPortfolioId == null) {
      setTradeFlow(null)
      setTradeFlowHydrated(false)
      return
    }
    if (activeSection !== 'dashboard') {
      setTradeFlow(null)
      setTradeFlowHydrated(false)
      return
    }
    let cancelled = false
    setTradeFlowHydrated(false)
    setTradeFlow(null)
    void loadTradeFlowForPortfolio(overviewApiPortfolioId, valuationCurrency).then((flow) => {
      if (!cancelled) {
        setTradeFlow(flow)
        setTradeFlowHydrated(true)
      }
    })
    return () => {
      cancelled = true
    }
  }, [activeSection, overviewApiPortfolioId, valuationCurrency, selectedPortfolioId])

  useEffect(() => {
    if (selectedPortfolioId == null || activeSection !== 'dashboard') {
      setValueSnapshots([])
      return
    }
    let cancelled = false
    if (selectedPortfolioId === ALL_PORTFOLIOS_ID) {
      if (portfolios.length === 0) {
        setValueSnapshots([])
        return
      }
      void Promise.all(portfolios.map((p) => getPortfolioSnapshots(p.id)))
        .then((rows) => {
          if (!cancelled) setValueSnapshots(mergePortfolioValueSnapshots(rows))
        })
        .catch(() => {
          if (!cancelled) setValueSnapshots([])
        })
      return () => {
        cancelled = true
      }
    }
    void getPortfolioSnapshots(selectedPortfolioId)
      .then((rows) => {
        if (!cancelled) setValueSnapshots(rows)
      })
      .catch(() => {
        if (!cancelled) setValueSnapshots([])
      })
    return () => {
      cancelled = true
    }
  }, [activeSection, selectedPortfolioId, portfolios])

  useEffect(() => {
    if (activeSection !== 'markets' || selectedInstrumentId == null) {
      return
    }
    const selected = marketOptions.find((item) => item.instrumentId === selectedInstrumentId)
    if (!selected) {
      setInstrumentPerformance({
        change1D: null,
        change1M: null,
        change3M: null,
        change6M: null,
        change1Y: null,
      })
      return
    }
    void fetchMarketOverview({
      page: 0,
      size: 200,
      category: 'all',
      query: selected.symbol,
      displayCurrency: valuationCurrency,
    })
      .then((page) => {
        const row = page.content.find(
          (item) =>
            item.instrumentId === selectedInstrumentId ||
            marketOverviewSymbolMatches(item.symbol, selected.symbol),
        )
        setInstrumentPerformance({
          change1D: row?.change1D ?? null,
          change1M: row?.change1M ?? null,
          change3M: row?.change3M ?? null,
          change6M: row?.change6M ?? null,
          change1Y: row?.change1Y ?? null,
        })
      })
      .catch(() => {
        setInstrumentPerformance({
          change1D: null,
          change1M: null,
          change3M: null,
          change6M: null,
          change1Y: null,
        })
      })
  }, [activeSection, selectedInstrumentId, marketOptions, valuationCurrency])

  useEffect(() => {
    if (activeSection !== 'markets' || selectedInstrumentId == null) {
      return
    }
    const lotsNumber = Number(lots)
    const amountNumber = Number(amount)
    const normalizedAcquiredAt = purchaseMode === 'PAST' ? toUtcStartOfDay(acquiredAt) : undefined
    const tradePid = effectiveTradePortfolioId ?? undefined
    const payload =
      inputMode === 'LOTS'
        ? {
            instrumentId: selectedInstrumentId,
            portfolioId: tradePid,
            inputMode,
            lots: Number.isFinite(lotsNumber) && lotsNumber > 0 ? lotsNumber : undefined,
            inputCurrency: tradePaymentCurrency,
            purchaseMode,
            acquiredAt: normalizedAcquiredAt,
            unitPrice: purchaseMode === 'PAST' && unitPrice ? Number(unitPrice) : undefined,
          }
        : {
            instrumentId: selectedInstrumentId,
            portfolioId: tradePid,
            inputMode,
            amount: Number.isFinite(amountNumber) && amountNumber > 0 ? amountNumber : undefined,
            inputCurrency: tradePaymentCurrency,
            purchaseMode,
            acquiredAt: normalizedAcquiredAt,
            unitPrice: purchaseMode === 'PAST' && unitPrice ? Number(unitPrice) : undefined,
          }
    const missingPrimaryInput =
      (inputMode === 'LOTS' && payload.lots == null) || (inputMode === 'AMOUNT' && payload.amount == null)

    if (purchaseMode === 'PAST' && !normalizedAcquiredAt) {
      setTradePreviewError(null)
      setPastDateRollNotice(null)
      setManualUnitPriceRequired(false)
      setUnitPriceUsed(null)
      setLastTradePreview(null)
      return
    }

    if (!isTlDepositInstrument && purchaseMode === 'PAST' && normalizedAcquiredAt && missingPrimaryInput) {
      const timer = window.setTimeout(() => {
        void previewTrade({
          instrumentId: selectedInstrumentId,
          portfolioId: tradePid,
          purchaseMode,
          inputMode: 'LOTS',
          lots: 1,
          inputCurrency: tradePaymentCurrency,
          acquiredAt: normalizedAcquiredAt,
          unitPrice: unitPrice ? Number(unitPrice) : undefined,
        })
          .then((preview) => {
            setTradePreviewError(null)
            setLastTradePreview(preview)
            setUnitPriceUsed(preview.unitPriceUsed)
            if (!isTlDepositInstrument && !unitPrice) {
              setUnitPrice(String(preview.unitPriceUsed))
            }
            setManualUnitPriceRequired(preview.manualUnitPriceRequired)
            applyPastPreviewUi(preview, purchaseMode, setAcquiredAt, setPastDateRollNotice)
          })
          .catch((error) => {
            const raw = extractApiErrorMessage(error)
            setPastDateRollNotice(null)
            setManualUnitPriceRequired(false)
            setUnitPriceUsed(null)
            setLastTradePreview(null)
            setTradePreviewError(raw || t('marketsAdd.errors.previewFailed'))
          })
      }, 150)
      return () => window.clearTimeout(timer)
    }

    if (missingPrimaryInput) {
      setTradePreviewError(null)
      return
    }
    const timer = window.setTimeout(() => {
      void previewTrade(payload)
        .then((preview) => {
          setTradePreviewError(null)
          setLastTradePreview(preview)
          setUnitPriceUsed(preview.unitPriceUsed)
          if (!isTlDepositInstrument && purchaseMode === 'PAST' && !unitPrice) {
            setUnitPrice(String(preview.unitPriceUsed))
          }
          setManualUnitPriceRequired(preview.manualUnitPriceRequired)
          applyPastPreviewUi(preview, purchaseMode, setAcquiredAt, setPastDateRollNotice)
          if (inputMode === 'LOTS' || lastEditedField === 'lots') {
            setAmount(String(preview.computedInputAmount))
          } else {
            setLots(String(preview.computedLots))
          }
        })
        .catch((error) => {
          const raw = extractApiErrorMessage(error)
          setPastDateRollNotice(null)
          setManualUnitPriceRequired(false)
          setUnitPriceUsed(null)
          setLastTradePreview(null)
          setTradePreviewError(
            raw ||
              (purchaseMode === 'NOW'
                ? t('marketsAdd.errors.currentPriceUnavailable')
                : t('marketsAdd.errors.pastPreviewFailed')),
          )
        })
    }, 250)
    return () => window.clearTimeout(timer)
  }, [
    activeSection,
    selectedInstrumentId,
    effectiveTradePortfolioId,
    inputMode,
    lots,
    amount,
    tradePaymentCurrency,
    purchaseMode,
    acquiredAt,
    unitPrice,
    lastEditedField,
    isTlDepositInstrument,
    t,
  ])

  const availableAssetTypes = useMemo(
    () => {
      const source = isSellFlow ? sellHoldingOptions : marketOptions
      const seen = new Set<MarketOptionAssetType>()
      source.forEach((item) => seen.add(item.assetType))
      return MARKET_OPTION_ASSET_TYPE_ORDER.filter((type) => seen.has(type))
    },
    [isSellFlow, sellHoldingOptions, marketOptions],
  )

  const assetTypeFilterOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: t('marketsAdd.assetTypeAll') },
      ...availableAssetTypes.map((type) => ({
        value: type,
        label: t(`allocation.instrumentType.${type}`, { defaultValue: type }),
      })),
    ],
    [availableAssetTypes, t],
  )

  useEffect(() => {
    if (selectedAssetType !== 'ALL' && !availableAssetTypes.includes(selectedAssetType)) {
      setSelectedAssetType('ALL')
    }
  }, [availableAssetTypes, selectedAssetType])

  useEffect(() => {
    if (activeSection !== 'markets' || !isSellFlow) return
    setSelectedAssetType('ALL')
  }, [activeSection, isSellFlow])

  const filteredMarketOptions = useMemo(() => {
    const base = isSellFlow ? sellHoldingOptions : marketOptions
    const typeFiltered =
      selectedAssetType === 'ALL'
        ? base
        : base.filter((item) => item.assetType === selectedAssetType)
    const q = instrumentQuery.trim().toLowerCase()
    if (!q) return typeFiltered
    return typeFiltered.filter((item) => `${item.symbol} ${item.name}`.toLowerCase().includes(q))
  }, [instrumentQuery, isSellFlow, marketOptions, sellHoldingOptions, selectedAssetType])

  useEffect(() => {
    if (!isSellFlow || activeSection !== 'markets' || selectedInstrumentId == null) return
    const holding = sellHoldingOptions.find((o) => o.instrumentId === selectedInstrumentId)
    if (!holding) return
    setLots((prev) => {
      const n = Number(prev)
      if (!Number.isFinite(n) || n <= 0) return '1'
      if (n > holding.holdingQty) return String(holding.holdingQty)
      return prev
    })
  }, [isSellFlow, activeSection, selectedInstrumentId, sellHoldingOptions])

  useEffect(() => {
    if (activeSection !== 'markets') return
    if (filteredMarketOptions.length === 0) {
      setSelectedInstrumentId(null)
      return
    }
    if (selectedInstrumentId == null || !filteredMarketOptions.some((o) => o.instrumentId === selectedInstrumentId)) {
      setSelectedInstrumentId(filteredMarketOptions[0].instrumentId)
    }
  }, [activeSection, filteredMarketOptions, selectedInstrumentId])
  const parsedAmount = Number(amount)
  const parsedLots = Number(lots)
  const previewTotal = Number.isFinite(parsedAmount) && parsedAmount > 0 ? parsedAmount : 0
  const previewLots = Number.isFinite(parsedLots) && parsedLots > 0 ? parsedLots : 0
  const sellProceedsPreview =
    isSellFlow && unitPriceUsed != null && previewLots > 0 ? unitPriceUsed * previewLots : null
  const sellCostBasisPreview =
    isSellFlow && sellOverviewItem && previewLots > 0 ? sellOverviewItem.avgBuyPrice * previewLots : null
  const sellRealizedPnlPreview =
    sellProceedsPreview != null && sellCostBasisPreview != null
      ? sellProceedsPreview - sellCostBasisPreview
      : null
  const sellRealizedPnlPct =
    sellCostBasisPreview != null && sellCostBasisPreview > 0 && sellRealizedPnlPreview != null
      ? (sellRealizedPnlPreview / sellCostBasisPreview) * 100
      : null
  const sellRemainingQty =
    isSellFlow && sellOverviewItem != null
      ? Math.max(0, sellOverviewItem.quantity - previewLots)
      : null
  const selectedSellHoldingQty = selectedSellHolding?.holdingQty ?? sellOverviewItem?.quantity ?? 0
  const depositPrincipalTry = useMemo(() => {
    if (!isTlDepositInstrument || unitPriceUsed == null || previewLots <= 0) return null
    return unitPriceUsed * previewLots
  }, [isTlDepositInstrument, unitPriceUsed, previewLots])
  const depositRateDisplay = useMemo(() => {
    const rate = depositRatePreview?.annualRatePercent
    if (depositRateLoading) return t('marketsAdd.depositRateLoading')
    if (rate == null || !Number.isFinite(Number(rate))) return t('marketsAdd.depositRateUnavailable')
    return `%${Number(rate).toLocaleString(i18n.language, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`
  }, [depositRateLoading, depositRatePreview?.annualRatePercent, i18n.language, t])
  const formatDepositInstrumentLabel = useCallback(
    (symbol: string | null | undefined, fallbackName?: string | null) =>
      formatDepositMaturityLabel(symbol, fallbackName, t),
    [t],
  )
  const formatMarketOptionLabel = useCallback(
    (item: MarketOption) => {
      if (item.assetType === 'DEPOSIT') {
        return formatDepositInstrumentLabel(item.symbol, item.name)
      }
      return item.symbol && item.name ? `${item.symbol} - ${item.name}` : item.symbol || item.name || '—'
    },
    [formatDepositInstrumentLabel],
  )
  const formatSellHoldingLabel = useCallback(
    (item: SellHoldingOption) =>
      `${item.symbol} — ${formatHoldingQuantity(item.holdingQty, i18n.language)} lot`,
    [i18n.language],
  )
  const formatInstrumentSymbolLabel = useCallback(
    (symbol: string) => formatDepositMaturityLabel(symbol, null, t),
    [t],
  )
  const selectedInstrumentDisplayLabel = useMemo(
    () =>
      selectedInstrument
        ? isSellFlow && selectedSellHolding
          ? formatSellHoldingLabel(selectedSellHolding)
          : formatMarketOptionLabel(selectedInstrument)
        : '—',
    [formatMarketOptionLabel, formatSellHoldingLabel, isSellFlow, selectedInstrument, selectedSellHolding],
  )
  const projectedDistribution = useMemo(() => {
    const baseItems = overview?.items ?? []
    const baseTotal = Number(overview?.totalValue ?? 0)
    const selectedSymbol = selectedInstrument?.symbol ?? ''
    const pendingValue =
      tradePaymentCurrency === (overview?.currency ?? 'USD')
        ? previewTotal
        : unitPriceUsed != null && previewLots > 0
          ? unitPriceUsed * previewLots
          : 0
    const merged = new Map<string, number>()
    baseItems.forEach((item) => merged.set(item.symbol, Number(item.value)))
    if (selectedSymbol && pendingValue > 0) {
      merged.set(selectedSymbol, (merged.get(selectedSymbol) ?? 0) + pendingValue)
    }
    const projectedTotal = baseTotal + pendingValue
    return Array.from(merged.entries())
      .map(([symbol, value]) => ({
        symbol,
        value,
        weight: projectedTotal > 0 ? (value / projectedTotal) * 100 : 0,
      }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 5)
  }, [overview, selectedInstrument, previewTotal, previewLots, tradePaymentCurrency, unitPriceUsed])

  const openSellFromAllocation = useCallback(
    (symbol: string) => {
      if (selectedPortfolioId == null || selectedPortfolioId <= 0) return
      const item = overview?.items.find((row) => row.symbol === symbol)
      if (!item || item.quantity <= 0) return
      setTradeSide('SELL')
      setPurchaseMode('NOW')
      setSelectedInstrumentId(item.instrumentId)
      setLots(String(item.quantity))
      setInputMode('LOTS')
      setIsPreviewStep(false)
      setTradeError(null)
      setTradePreviewError(null)
      setTradeSuccess(null)
      setLastTradePreview(null)
      selectPortfolioSection('markets')
    },
    [overview, selectedPortfolioId, selectPortfolioSection],
  )

  const openTradePreview = useCallback(() => {
    if (isSellFlow && previewLots > selectedSellHoldingQty) {
      const message = t('marketsAdd.errors.exceedsHoldings', {
        max: formatHoldingQuantity(selectedSellHoldingQty, i18n.language),
      })
      setTradePreviewError(message)
      setTradeError(message)
      return
    }
    if (isTlDepositInstrument && purchaseMode === 'PAST' && !toUtcStartOfDay(acquiredAt)) {
      setDepositPastDateError(t('marketsAdd.depositPastDateRequired'))
      setTradeError(null)
      setTradePreviewError(null)
      return
    }
    setDepositPastDateError(null)
    setTradePreviewError(null)
    setTradeError(null)
    setIsPreviewStep(true)
  }, [
    isSellFlow,
    previewLots,
    selectedSellHoldingQty,
    i18n.language,
    isTlDepositInstrument,
    purchaseMode,
    acquiredAt,
    t,
  ])

  const handleConfirmDeleteTransaction = async () => {
    const target = deleteTxConfirm
    if (!target) return
    setDeleteTxSubmitting(true)
    setDeleteTxError(null)
    try {
      await deleteTransaction(target.transactionId)
      setDeleteTxConfirm(null)
      const portfolioId = selectedPortfolioId === ALL_PORTFOLIOS_ID ? null : selectedPortfolioId
      await loadHistoryPage(historyPage, appliedHistoryFilters, portfolioId)
      const updatedOverview = await getMyPortfolioOverview(overviewApiPortfolioId, valuationCurrency)
      setOverview(updatedOverview)
      setPerformanceSeries(await getPortfolioPerformanceSeries(overviewApiPortfolioId, valuationCurrency, 'all'))
      setPerformanceSeriesHydrated(true)
      setTradeFlow(await loadTradeFlowForPortfolio(overviewApiPortfolioId, valuationCurrency))
      if (activeSection === 'salesAnalysis') {
        await loadSalesAnalysisPage(salesAnalysisPage, appliedSalesAnalysisFilters, portfolioId)
      }
    } catch (error) {
      setDeleteTxError(extractApiErrorMessage(error) || t('history.deleteFailed'))
    } finally {
      setDeleteTxSubmitting(false)
    }
  }

  const submitTrade = async () => {
    if (selectedInstrumentId == null || effectiveTradePortfolioId == null || effectiveTradePortfolioId <= 0) {
      return
    }
    if (isSellFlow && previewLots > selectedSellHoldingQty) {
      const message = t('marketsAdd.errors.exceedsHoldings', {
        max: formatHoldingQuantity(selectedSellHoldingQty, i18n.language),
      })
      setTradeError(message)
      setTradePreviewError(message)
      return
    }
    setTradeError(null)
    setTradePreviewError(null)
    setTradeSuccess(null)
    const effectivePurchaseMode: PurchaseMode = purchaseMode
    const normalizedAcquiredAt = effectivePurchaseMode === 'PAST' ? toUtcStartOfDay(acquiredAt) : undefined
    const tradePid = effectiveTradePortfolioId
    const pastUnitPayload =
      purchaseMode === 'PAST' && manualUnitPriceRequired && unitPrice.trim() !== ''
        ? Number(unitPrice)
        : undefined
    const payload =
      inputMode === 'LOTS'
        ? {
            instrumentId: selectedInstrumentId,
            portfolioId: tradePid,
            inputMode,
            lots: Number(lots),
            inputCurrency: tradePaymentCurrency,
            purchaseMode: effectivePurchaseMode,
            acquiredAt: normalizedAcquiredAt,
            unitPrice: pastUnitPayload,
          }
        : {
            instrumentId: selectedInstrumentId,
            portfolioId: tradePid,
            inputMode,
            amount: Number(amount),
            inputCurrency: tradePaymentCurrency,
            purchaseMode: effectivePurchaseMode,
            acquiredAt: normalizedAcquiredAt,
            unitPrice: pastUnitPayload,
          }
    if (effectivePurchaseMode === 'PAST' && !normalizedAcquiredAt) {
      const message = isTlDepositInstrument
        ? t('marketsAdd.depositPastDateRequired')
        : t('marketsAdd.errors.pastDateRequired')
      setTradeError(message)
      if (isTlDepositInstrument) {
        setDepositPastDateError(message)
      }
      return
    }
    if (effectivePurchaseMode === 'PAST' && manualUnitPriceRequired && !unitPrice) {
      setTradeError(t('marketsAdd.errors.pastDateAndPriceRequired'))
      return
    }
    setTradeSaving(true)
    try {
      await (isSellFlow ? sellTrade : buyTrade)(payload)
      setTradeSuccess(isSellFlow ? t('marketsAdd.messages.sold') : t('marketsAdd.messages.saved'))
      setTradeSide('BUY')
      setLastTradePreview(null)
      setIsPreviewStep(false)
      await loadHistoryPage(0, appliedHistoryFilters, isAggregatePortfolioView ? null : selectedPortfolioId)
      const updatedOverview = await getMyPortfolioOverview(overviewApiPortfolioId, valuationCurrency)
      setOverview(updatedOverview)
      setPerformanceSeries(await getPortfolioPerformanceSeries(overviewApiPortfolioId, valuationCurrency, 'all'))
      setPerformanceSeriesHydrated(true)
      setTradeFlow(await loadTradeFlowForPortfolio(overviewApiPortfolioId, valuationCurrency))
    } catch (error) {
      const message = extractApiErrorMessage(error)
      setTradeError(message || t('marketsAdd.errors.saveFailed'))
    } finally {
      setTradeSaving(false)
    }
  }

  const openCreatePortfolioModal = () => {
    if (portfolios.length >= MAX_USER_PORTFOLIOS) {
      setPortfolioActionError('Maksimum 5 portfoy olusturabilirsiniz.')
      return
    }
    setPortfolioActionError(null)
    setNewPortfolioName('')
    setShowCreatePortfolioModal(true)
  }

  const handleCreatePortfolio = async () => {
    const name = newPortfolioName.trim()
    if (!name) {
      setPortfolioActionError('Portfoy adi zorunludur.')
      return
    }
    if (portfolios.length >= MAX_USER_PORTFOLIOS) {
      setPortfolioActionError('Maksimum 5 portfoy olusturabilirsiniz.')
      return
    }
    setPortfolioActionLoading(true)
    setPortfolioActionError(null)
    try {
      const created = await createPortfolio({ name })
      await refreshPortfolios()
      selectPortfolio(created.id)
      setShowCreatePortfolioModal(false)
    } catch (error) {
      setPortfolioActionError(extractApiErrorMessage(error) || 'Portfoy olusturulamadi.')
    } finally {
      setPortfolioActionLoading(false)
    }
  }

  const settingsPortfolioCreatedDisplay = useMemo(() => {
    const raw = selectedPortfolio?.createdAt
    if (!raw) return null
    try {
      const d = new Date(raw.includes('T') ? raw : `${raw}Z`)
      if (Number.isNaN(d.getTime())) return raw
      return d.toLocaleString(i18n.language, { dateStyle: 'long', timeStyle: 'short' })
    } catch {
      return raw
    }
  }, [selectedPortfolio?.createdAt, i18n.language])

  useEffect(() => {
    if (activeSection !== 'settings') {
      setPortfolioSettingsError(null)
    }
  }, [activeSection])

  useEffect(() => {
    if (deleteConfirm && selectedPortfolioId != null && selectedPortfolioId > 0 && deleteConfirm.id !== selectedPortfolioId) {
      setDeleteConfirm(null)
    }
  }, [deleteConfirm, selectedPortfolioId])

  const handleToggleAmountsHidden = async (portfolioId: number, next: boolean) => {
    setAmountsHiddenSaving(true)
    setPortfolioSettingsError(null)
    try {
      await patchPortfolioAmountsHidden(portfolioId, next)
      await refreshPortfolios()
    } catch (error) {
      setPortfolioSettingsError(extractApiErrorMessage(error) || t('settingsPage.saveAmountsHiddenError'))
    } finally {
      setAmountsHiddenSaving(false)
    }
  }

  const handleConfirmDeletePortfolio = async () => {
    const target = deleteConfirm
    if (!target) return
    setDeletePortfolioSubmitting(true)
    setPortfolioSettingsError(null)
    try {
      await deletePortfolio(target.id)
      setDeleteConfirm(null)
      setActiveSection('dashboard')
      await refreshPortfolios()
    } catch (error) {
      setPortfolioSettingsError(extractApiErrorMessage(error) || t('settingsPage.deletePortfolioError'))
    } finally {
      setDeletePortfolioSubmitting(false)
    }
  }

  return (
    <section className="my-portfolio-page">
      <div className="my-portfolio-shell">
        {isMobileNav && !mobileNavOpen ? (
          <button
            type="button"
            className="my-portfolio-sidebar-mobile-trigger"
            onClick={() => setMobileNavOpen(true)}
            aria-expanded={false}
            aria-controls="my-portfolio-sidebar"
            aria-label={t('sidebar.expand')}
          >
            <span className="my-portfolio-sidebar-toggle-icon" aria-hidden>
              ›
            </span>
          </button>
        ) : null}

        {isMobileNav && mobileNavOpen ? (
          <button
            type="button"
            className="my-portfolio-sidebar-backdrop"
            aria-label={t('sidebar.collapse')}
            onClick={() => setMobileNavOpen(false)}
          />
        ) : null}

        <aside
          id="my-portfolio-sidebar"
          className={`my-portfolio-sidebar card${
            (!isMobileNav && sidebarOpen) || (isMobileNav && mobileNavOpen) ? ' my-portfolio-sidebar-open' : ''
          }${isMobileNav && mobileNavOpen ? ' my-portfolio-sidebar-mobile-open' : ''}`}
          role={isMobileNav && mobileNavOpen ? 'dialog' : undefined}
          aria-modal={isMobileNav && mobileNavOpen ? true : undefined}
          aria-label={isMobileNav ? t('sidebar.navAria') : undefined}
        >
          <div className="my-portfolio-sidebar-top">
            <button
              type="button"
              className="my-portfolio-sidebar-toggle"
              onClick={() => {
                if (isMobileNav) {
                  setMobileNavOpen(false)
                  return
                }
                setSidebarOpen((prev) => !prev)
              }}
              aria-label={isMobileNav || sidebarOpen ? t('sidebar.collapse') : t('sidebar.expand')}
            >
              <span className="my-portfolio-sidebar-toggle-icon" aria-hidden>
                {isMobileNav || sidebarOpen ? '‹' : '›'}
              </span>
            </button>

            <label className="my-portfolio-select-wrap">
              {isMobileNav || sidebarOpen ? (
                <>
                  <span>{t('sidebar.portfolios')}</span>
                  <select
                    aria-label={t('sidebar.portfolios')}
                    value={
                      selectedPortfolioId === ALL_PORTFOLIOS_ID
                        ? String(ALL_PORTFOLIOS_ID)
                        : selectedPortfolioId != null && selectedPortfolioId > 0
                          ? String(selectedPortfolioId)
                          : portfolios.length === 0
                            ? CREATE_PORTFOLIO_SELECT_VALUE
                            : String(ALL_PORTFOLIOS_ID)
                    }
                    onChange={(event) => {
                      const value = event.target.value
                      if (value === CREATE_PORTFOLIO_SELECT_VALUE) {
                        openCreatePortfolioModal()
                        return
                      }
                      selectPortfolio(Number(value))
                    }}
                  >
                    {portfolios.length > 0 ? (
                      <option value={String(ALL_PORTFOLIOS_ID)}>{t('sidebar.overviewOption')}</option>
                    ) : null}
                    {portfolios.map((portfolio) => (
                      <option key={portfolio.id} value={portfolio.id}>
                        {portfolio.name}
                      </option>
                    ))}
                    {portfolios.length < MAX_USER_PORTFOLIOS ? (
                      <option value={CREATE_PORTFOLIO_SELECT_VALUE}>
                        {portfolios.length === 0 ? t('sidebar.addPortfolioOptionFirst') : t('sidebar.addPortfolioOptionMore')}
                      </option>
                    ) : null}
                  </select>
                </>
              ) : (
                <button type="button" className="my-portfolio-portfolio-icon" aria-label={t('sidebar.portfolios')}>
                  <svg viewBox="0 0 24 24" aria-hidden>
                    <path d="M4 7h16v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z" />
                    <path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                  </svg>
                </button>
              )}
            </label>
          </div>

          <nav className="my-portfolio-sidebar-nav" aria-label={t('sidebar.navAria')}>
            {sidebarMainKeys.map((item) => (
              <button
                key={item}
                type="button"
                className={`my-portfolio-sidebar-item${activeSection === item ? ' my-portfolio-sidebar-item-active' : ''}`}
                onClick={() => selectPortfolioSection(item)}
              >
                <span className="my-portfolio-sidebar-item-icon" aria-hidden>
                  <SidebarItemIcon item={item} />
                </span>
                {isMobileNav || sidebarOpen ? <span>{t(`sidebar.items.${item}`)}</span> : null}
              </button>
            ))}
          </nav>

          <nav className="my-portfolio-sidebar-nav my-portfolio-sidebar-nav-secondary" aria-label={t('sidebar.quickAria')}>
            {sidebarSecondaryKeys.map((item) => (
              <button
                key={item}
                type="button"
                className={`my-portfolio-sidebar-item${activeSection === item ? ' my-portfolio-sidebar-item-active' : ''}`}
                onClick={() => selectPortfolioSection(item)}
              >
                <span className="my-portfolio-sidebar-item-icon" aria-hidden>
                  <SidebarItemIcon item={item} />
                </span>
                {isMobileNav || sidebarOpen ? <span>{t(`sidebar.items.${item}`)}</span> : null}
              </button>
            ))}
          </nav>

          <button type="button" className="my-portfolio-sidebar-logout">
            <span className="my-portfolio-sidebar-item-icon" aria-hidden>
              <svg viewBox="0 0 24 24" aria-hidden>
                <path d="M10 6H7a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h3" />
                <path d="M14 16l4-4-4-4" />
                <path d="M18 12H9" />
              </svg>
            </span>
            {isMobileNav || sidebarOpen ? <span>{t('sidebar.logout')}</span> : null}
          </button>
        </aside>

        <div className="my-portfolio-content">
          {activeSection === 'markets' ? (
            <article className={`card my-portfolio-trade-card${isDarkTheme ? ' is-dark' : ' is-light'}`}>
              <div className="my-portfolio-trade-title-row">
                <div
                  className="my-portfolio-trade-toggle-group my-portfolio-trade-side-toggle"
                  role="tablist"
                  aria-label={t('sidebar.items.markets')}
                >
                  <button
                    type="button"
                    role="tab"
                    aria-selected={!isSellFlow}
                    className={!isSellFlow ? 'is-active' : ''}
                    onClick={() => {
                      setTradeSide('BUY')
                      setIsPreviewStep(false)
                    }}
                  >
                    {t('marketsAdd.tradeSide.buy')}
                  </button>
                  <button
                    type="button"
                    role="tab"
                    aria-selected={isSellFlow}
                    className={isSellFlow ? 'is-active' : ''}
                    onClick={() => {
                      setTradeSide('SELL')
                      setPurchaseMode('NOW')
                      setIsPreviewStep(false)
                    }}
                  >
                    {t('marketsAdd.tradeSide.sell')}
                  </button>
                </div>
              </div>
              {isAggregatePortfolioView && portfolios.length > 0 ? (
                <label className="my-portfolio-trade-field my-portfolio-trade-field-full">
                  <span>{t('sidebar.tradePickPortfolio')}</span>
                  <select
                    aria-label={t('sidebar.tradePickPortfolio')}
                    value={tradeTargetPortfolioId ?? ''}
                    onChange={(event) => setTradeTargetPortfolioId(Number(event.target.value))}
                  >
                    {portfolios.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </label>
              ) : null}

              <div className={`my-portfolio-trade-shell${isPreviewStep ? ' is-preview' : ''}`}>
                {isSellFlow && effectiveTradePortfolioId == null ? (
                  <p className="my-portfolio-trade-note my-portfolio-sell-empty-state" role="status">
                    {t('marketsAdd.sellPickPortfolio')}
                  </p>
                ) : isSellFlow &&
                  !sellHoldingsLoading &&
                  sellHoldingOptions.length === 0 &&
                  !(purchaseMode === 'PAST' && !toUtcStartOfDay(acquiredAt)) ? (
                  <p className="my-portfolio-trade-note my-portfolio-sell-empty-state" role="status">
                    {purchaseMode === 'PAST'
                      ? t('marketsAdd.errors.noHoldingsOnDate')
                      : t('marketsAdd.sellHoldingsEmpty')}
                  </p>
                ) : (
                <>
                <div className="my-portfolio-trade-form-panel">
                  <ol className="my-portfolio-trade-steps">
                    <li className={!isPreviewStep ? 'is-active' : ''}>
                      {isSellFlow ? t('marketsAdd.steps.detailsSell') : t('marketsAdd.steps.details')}
                    </li>
                    <li className={isPreviewStep ? 'is-active' : ''}>{t('marketsAdd.steps.preview')}</li>
                  </ol>

                  {!isPreviewStep ? (
                  <label className="my-portfolio-trade-field my-portfolio-trade-field-full">
                    <span>{t('marketsAdd.searchLabel')}</span>
                    <input
                      value={instrumentQuery}
                      onChange={(event) => setInstrumentQuery(event.target.value)}
                      placeholder={t('marketsAdd.searchPlaceholder')}
                      readOnly={isPreviewStep}
                    />
                  </label>
                  ) : null}

                  {!isPreviewStep ? (
                  <div className="my-portfolio-trade-grid">
                    <label className="my-portfolio-trade-field">
                      <span>{t('marketsAdd.assetTypeLabel')}</span>
                      <select
                        value={selectedAssetType}
                        onChange={(event) => setSelectedAssetType(event.target.value as MarketOptionAssetTypeFilter)}
                        disabled={
                          isPreviewStep ||
                          marketLoading ||
                          (isSellFlow && sellHoldingsLoading) ||
                          availableAssetTypes.length === 0
                        }
                      >
                        {assetTypeFilterOptions.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="my-portfolio-trade-field">
                      <span>{t('marketsAdd.instrumentLabel')}</span>
                      <select
                        value={selectedInstrumentId != null ? String(selectedInstrumentId) : ''}
                        onChange={(event) => {
                          const v = event.target.value
                          setSelectedInstrumentId(v === '' ? null : Number(v))
                        }}
                        disabled={
                          isPreviewStep ||
                          marketLoading ||
                          (isSellFlow ? sellHoldingsLoading : false) ||
                          filteredMarketOptions.length === 0
                        }
                      >
                        {filteredMarketOptions.map((item) => (
                          <option key={item.instrumentId} value={String(item.instrumentId)}>
                            {isSellFlow
                              ? formatSellHoldingLabel(item as SellHoldingOption)
                              : formatMarketOptionLabel(item)}
                          </option>
                        ))}
                      </select>
                      {marketCatalogError ? (
                        <small className="auth-error" role="alert">
                          {marketCatalogError}
                        </small>
                      ) : null}
                    </label>
                  </div>
                  ) : null}

                  {!isPreviewStep ? (
                  <div className="my-portfolio-trade-toggle-grid">
                    <div>
                      <div className="my-portfolio-trade-toggle-group">
                        <button
                          type="button"
                          className={purchaseMode === 'NOW' ? 'is-active' : ''}
                          onClick={() => {
                            setPurchaseMode('NOW')
                            setPastDateRollNotice(null)
                          }}
                          disabled={isPreviewStep}
                        >
                          {isSellFlow
                            ? t('marketsAdd.purchaseMode.sellNow')
                            : t('marketsAdd.purchaseMode.now')}
                        </button>
                        <button
                          type="button"
                          className={purchaseMode === 'PAST' ? 'is-active' : ''}
                          onClick={() => {
                            setPurchaseMode('PAST')
                            setPastDateRollNotice(null)
                          }}
                          disabled={isPreviewStep}
                        >
                          {isSellFlow
                            ? t('marketsAdd.purchaseMode.sellPast')
                            : t('marketsAdd.purchaseMode.past')}
                        </button>
                      </div>
                    </div>

                    {!isTlDepositInstrument && !isSellFlow ? (
                    <div>
                      <div className="my-portfolio-trade-toggle-group">
                        <button
                          type="button"
                          className={inputMode === 'LOTS' ? 'is-active' : ''}
                          onClick={() => setInputMode('LOTS')}
                          disabled={isPreviewStep}
                        >
                          {t('marketsAdd.inputMode.lots')}
                        </button>
                        <button
                          type="button"
                          className={inputMode === 'AMOUNT' ? 'is-active' : ''}
                          onClick={() => setInputMode('AMOUNT')}
                          disabled={isPreviewStep}
                        >
                          {t('marketsAdd.inputMode.amount')}
                        </button>
                      </div>
                    </div>
                    ) : null}
                  </div>
                  ) : null}

                  {!isPreviewStep ? (
                  <div className="my-portfolio-trade-grid">
                    {!isTlDepositInstrument ? (
                    <label className="my-portfolio-trade-field">
                      <span>{t('marketsAdd.lotsLabel')}</span>
                      <input
                        value={lots}
                        onChange={(event) => {
                          setLastEditedField('lots')
                          setInputMode('LOTS')
                          setLots(event.target.value)
                        }}
                        readOnly={isPreviewStep}
                        inputMode="decimal"
                      />
                      {isSellFlow && selectedSellHoldingQty > 0 ? (
                        <small className="my-portfolio-sell-holding-hint" role="status">
                          {t('marketsAdd.sellAvailableQty', {
                            qty: formatHoldingQuantity(selectedSellHoldingQty, i18n.language),
                          })}
                          {' · '}
                          <button
                            type="button"
                            className="my-portfolio-sell-all-link"
                            onClick={() => {
                              setLastEditedField('lots')
                              setInputMode('LOTS')
                              setLots(String(selectedSellHoldingQty))
                            }}
                            disabled={isPreviewStep}
                          >
                            {t('marketsAdd.sellAll')}
                          </button>
                        </small>
                      ) : null}
                    </label>
                    ) : null}

                    {!isTlDepositInstrument ? (
                      <label className="my-portfolio-trade-field">
                        <span>{isSellFlow ? t('marketsAdd.sellPriceLabel') : t('marketsAdd.buyPriceLabel')}</span>
                        <div className="my-portfolio-trade-money-input">
                          <span className="my-portfolio-trade-money-input__sym" aria-hidden>
                            {currencySymbolPrefix(instrumentQuoteCurrencyCode)}
                          </span>
                          <input
                            value={
                              purchaseMode === 'NOW'
                                ? (unitPriceUsed ?? '').toString()
                                : manualUnitPriceRequired
                                  ? unitPrice
                                  : (unitPriceUsed ?? unitPrice ?? '').toString()
                            }
                            readOnly={isPreviewStep || purchaseMode === 'NOW' || (purchaseMode === 'PAST' && !manualUnitPriceRequired)}
                            onChange={(event) => setUnitPrice(event.target.value)}
                            inputMode="decimal"
                            placeholder={manualUnitPriceRequired ? t('marketsAdd.buyPricePlaceholder') : ''}
                          />
                        </div>
                        {purchaseMode === 'PAST' && pastDateRollNotice ? (
                          <small className="my-portfolio-trade-note" role="status">
                            {t('marketsAdd.earliestPriceNotice', { date: pastDateRollNotice })}
                          </small>
                        ) : null}
                        {purchaseMode === 'PAST' && manualUnitPriceRequired ? (
                          <small className="my-portfolio-trade-note" role="alert">
                            {t('marketsAdd.manualPriceRequiredNotice')}
                          </small>
                        ) : null}
                      </label>
                    ) : (
                      <label className="my-portfolio-trade-field">
                        <span>{t('marketsAdd.depositRateLabel')}</span>
                        <input value={depositRateDisplay} readOnly />
                      </label>
                    )}

                    <label className="my-portfolio-trade-field">
                      <span>{t('marketsAdd.paymentCurrencyLabel')}</span>
                      <select
                        value={tradePaymentCurrency}
                        onChange={(event) =>
                          setTradePaymentCurrency(event.target.value as TradePaymentCurrency)
                        }
                        disabled={isPreviewStep}
                      >
                        {tradePaymentCurrencyOptions.map((c) => (
                          <option key={c} value={c}>
                            {tradePaymentCurrencyLabel(c)}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="my-portfolio-trade-field my-portfolio-trade-field-full">
                      <span>{t('marketsAdd.totalAmountLabel')}</span>
                      <div className="my-portfolio-trade-money-input">
                        <span className="my-portfolio-trade-money-input__sym" aria-hidden>
                          {currencySymbolPrefix(tradePaymentCurrency)}
                        </span>
                        <input
                          value={amount}
                          onChange={(event) => {
                            setLastEditedField('amount')
                            setInputMode('AMOUNT')
                            setAmount(event.target.value)
                          }}
                          readOnly={isPreviewStep}
                          inputMode="decimal"
                        />
                      </div>
                    </label>
                    {purchaseMode === 'PAST' ? (
                      <label className="my-portfolio-trade-field my-portfolio-trade-field-full">
                        <span>{isSellFlow ? t('marketsAdd.soldAtLabel') : t('marketsAdd.acquiredAtLabel')}</span>
                        <input
                          type="date"
                          value={acquiredAt}
                          onChange={(event) => {
                            setAcquiredAt(event.target.value)
                            setDepositPastDateError(null)
                            setUnitPrice('')
                            setManualUnitPriceRequired(false)
                            setUnitPriceUsed(null)
                            setPastDateRollNotice(null)
                          }}
                          disabled={isPreviewStep}
                        />
                        {isSellFlow && !toUtcStartOfDay(acquiredAt) ? (
                          <small className="my-portfolio-trade-note" role="status">
                            {t('marketsAdd.sellPickDate')}
                          </small>
                        ) : null}
                        {isTlDepositInstrument && depositPastDateError ? (
                          <small className="auth-error" role="alert">
                            {depositPastDateError}
                          </small>
                        ) : null}
                      </label>
                    ) : null}
                  </div>
                  ) : null}

                  {tradePreviewError ? (
                    <p className="auth-error" role="alert">
                      {tradePreviewError}
                    </p>
                  ) : null}
                  {!tradePreviewError &&
                  isSellFlow &&
                  !isPreviewStep &&
                  previewLots > 0 &&
                  previewLots > selectedSellHoldingQty ? (
                    <p className="auth-error" role="alert">
                      {t('marketsAdd.errors.exceedsHoldings', {
                        max: formatHoldingQuantity(selectedSellHoldingQty, i18n.language),
                      })}
                    </p>
                  ) : null}

                  {isPreviewStep ? (
                    <div className="my-portfolio-confirmation-box">
                      <h5>{isSellFlow ? t('marketsAdd.confirmationTitleSell') : t('marketsAdd.confirmationTitle')}</h5>
                      <p><span>{t('marketsAdd.instrumentLabel')}</span><strong>{selectedInstrumentDisplayLabel}</strong></p>
                      <p><span>{t('marketsAdd.transactionTypeLabel')}</span><strong>{purchaseMode === 'PAST' ? t('marketsAdd.purchaseMode.past') : t('marketsAdd.purchaseMode.now')}</strong></p>
                      <p><span>{t('marketsAdd.entryModeLabel')}</span><strong>{isTlDepositInstrument ? t('marketsAdd.entryModePrincipal') : inputMode === 'LOTS' ? t('marketsAdd.inputMode.lots') : t('marketsAdd.inputMode.amount')}</strong></p>
                      {isTlDepositInstrument ? (
                        <p><span>{t('marketsAdd.depositRateLabel')}</span><strong>{depositRateDisplay}</strong></p>
                      ) : (
                        <p><span>{t('marketsAdd.lotLabel')}</span><strong>{lots || '-'}</strong></p>
                      )}
                      <p><span>{t('marketsAdd.totalAmountLabel')}</span><strong>
                        {(() => {
                          const n = Number(amount)
                          return amount.trim() === '' || !Number.isFinite(n)
                            ? '—'
                            : formatMoneyPrefixed(n, tradePaymentCurrency, i18n.language, 2)
                        })()}
                      </strong></p>
                      {isTlDepositInstrument ? (
                        <p><span>{t('marketsAdd.depositPrincipalLabel')}</span><strong>
                          {depositPrincipalTry == null ? '—' : formatMoneyPrefixed(depositPrincipalTry, 'TRY', i18n.language, 2)}
                        </strong></p>
                      ) : (
                        <p><span>{t('marketsAdd.buyPriceLabel')}</span><strong>
                          {(() => {
                            const n =
                              unitPriceUsed != null && Number.isFinite(unitPriceUsed)
                                ? unitPriceUsed
                                : unitPrice.trim() !== ''
                                  ? Number(unitPrice)
                                  : NaN
                            return Number.isFinite(n)
                              ? formatMoneyPrefixed(n, instrumentQuoteCurrencyCode, i18n.language, 6)
                              : unitPrice.trim() !== ''
                                ? unitPrice
                                : '—'
                          })()}
                        </strong></p>
                      )}
                      <p>
                        <span>{t('marketsAdd.paymentCurrencyLabel')}</span>
                        <strong>{tradePaymentCurrencyLabel(tradePaymentCurrency)}</strong>
                      </p>
                      {purchaseMode === 'PAST' ? (
                        <p>
                          <span>{isSellFlow ? t('marketsAdd.soldAtLabel') : t('marketsAdd.acquiredAtLabel')}</span>
                          <strong>{acquiredAt || '-'}</strong>
                        </p>
                      ) : null}
                    </div>
                  ) : null}

                  {tradeError ? <p className="auth-error">{tradeError}</p> : null}
                  {tradeSuccess ? <p className="my-portfolio-trade-success">{tradeSuccess}</p> : null}

                  <div className="my-portfolio-trade-submit-row">
                    {isPreviewStep ? (
                      <button
                        type="button"
                        className="auth-submit auth-submit-secondary my-portfolio-trade-back"
                        onClick={() => {
                          setLastTradePreview(null)
                          setIsPreviewStep(false)
                        }}
                        disabled={tradeSaving}
                      >
                        {t('marketsAdd.back')}
                      </button>
                    ) : null}
                    <button
                      type="button"
                      className="auth-submit my-portfolio-trade-submit"
                      onClick={() => (isPreviewStep ? void submitTrade() : openTradePreview())}
                      disabled={tradeSaving}
                    >
                      {tradeSaving ? t('marketsAdd.saving') : isPreviewStep ? (isSellFlow ? t('marketsAdd.submitSell') : t('marketsAdd.submit')) : t('marketsAdd.continue')}
                    </button>
                  </div>
                </div>

                <aside className={`my-portfolio-trade-preview-panel${isPreviewStep ? ' is-expanded' : ''}`}>
                  <h4>{isSellFlow ? t('marketsAdd.sellAnalysisTitle') : t('marketsAdd.distributionTitle')}</h4>
                  {!isSellFlow ? <div className="my-portfolio-preview-ring" /> : null}
                  <div className={`my-portfolio-preview-stats${isSellFlow ? ' my-portfolio-sell-analysis-panel' : ''}`}>
                    {isSellFlow ? (
                      <>
                        <div>
                          <span>{t('marketsAdd.sellPositionLabel')}</span>
                          <strong>{`${formatHoldingQuantity(selectedSellHoldingQty, i18n.language)} lot`}</strong>
                        </div>
                        <div>
                          <span>{t('marketsAdd.sellAvgBuyLabel')}</span>
                          <strong>
                            {sellOverviewItem == null
                              ? '—'
                              : formatMoneyPrefixed(sellOverviewItem.avgBuyPrice, instrumentQuoteCurrencyCode, i18n.language, 6)}
                          </strong>
                        </div>
                        <div>
                          <span>{t('marketsAdd.sellPriceLabel')}</span>
                          <strong>
                            {unitPriceUsed == null
                              ? '—'
                              : formatMoneyPrefixed(unitPriceUsed, instrumentQuoteCurrencyCode, i18n.language, 6)}
                          </strong>
                        </div>
                        <div>
                          <span>{t('marketsAdd.totalLotsLabel')}</span>
                          <strong>{previewLots > 0 ? previewLots.toFixed(4) : '—'}</strong>
                        </div>
                        <div>
                          <span>{t('marketsAdd.sellProceedsLabel')}</span>
                          <strong>
                            {sellProceedsPreview == null
                              ? '—'
                              : formatMoneyPrefixed(sellProceedsPreview, instrumentQuoteCurrencyCode, i18n.language, 2)}
                          </strong>
                        </div>
                        <div>
                          <span>{t('marketsAdd.sellCostBasisLabel')}</span>
                          <strong>
                            {sellCostBasisPreview == null
                              ? '—'
                              : formatMoneyPrefixed(sellCostBasisPreview, instrumentQuoteCurrencyCode, i18n.language, 2)}
                          </strong>
                        </div>
                        <div>
                          <span>{t('marketsAdd.realizedPnlLabel')}</span>
                          <strong className={Number(sellRealizedPnlPreview ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                            {sellRealizedPnlPreview == null
                              ? '—'
                              : formatMoneyPrefixed(sellRealizedPnlPreview, instrumentQuoteCurrencyCode, i18n.language, 2)}
                          </strong>
                        </div>
                        <div>
                          <span>{t('marketsAdd.sellRealizedPnlPct')}</span>
                          <strong className={Number(sellRealizedPnlPct ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                            {sellRealizedPnlPct == null ? '—' : `${percentFormat.format(sellRealizedPnlPct)}%`}
                          </strong>
                        </div>
                        <div>
                          <span>{t('marketsAdd.sellRemainingQty')}</span>
                          <strong>
                            {sellRemainingQty == null
                              ? '—'
                              : `${formatHoldingQuantity(sellRemainingQty, i18n.language)} lot`}
                          </strong>
                        </div>
                        <div>
                          <span>{t('marketsAdd.instrumentCurrencyLabel')}</span>
                          <strong>{selectedInstrument?.nativeQuote ?? '—'}</strong>
                        </div>
                      </>
                    ) : (
                      <>
                        <div><span>{t('marketsAdd.totalAmountLabel')}</span><strong>{formatMoneyPrefixed(previewTotal || 0, tradePaymentCurrency, i18n.language, 2)}</strong></div>
                        <div><span>{isTlDepositInstrument ? t('marketsAdd.depositRateLabel') : t('marketsAdd.costPerLotLabel')}</span><strong>{isTlDepositInstrument ? depositRateDisplay : unitPriceUsed == null ? '—' : formatMoneyPrefixed(unitPriceUsed, instrumentQuoteCurrencyCode, i18n.language, 6)}</strong></div>
                        <div><span>{isTlDepositInstrument ? t('marketsAdd.depositPrincipalLabel') : t('marketsAdd.totalLotsLabel')}</span><strong>{isTlDepositInstrument ? (depositPrincipalTry == null ? '—' : formatMoneyPrefixed(depositPrincipalTry, 'TRY', i18n.language, 2)) : previewLots.toFixed(4)}</strong></div>
                        <div><span>{t('marketsAdd.instrumentCurrencyLabel')}</span><strong>{selectedInstrument?.nativeQuote ?? '—'}</strong></div>
                      </>
                    )}
                  </div>
                  {isPreviewStep && lastTradePreview?.acquisitionFxRates && !isSellFlow ? (
                    <AcquisitionFxPanel
                      snap={lastTradePreview.acquisitionFxRates}
                      purchaseMode={purchaseMode}
                    />
                  ) : null}
                  {!isSellFlow ? (
                  <ul className="my-portfolio-preview-distribution">
                    {projectedDistribution.map((item) => (
                      <li key={item.symbol}>
                        <span>{formatInstrumentSymbolLabel(item.symbol)}</span>
                        <strong>{item.weight.toFixed(2)}%</strong>
                      </li>
                    ))}
                  </ul>
                  ) : null}
                  {isPreviewStep && !isSellFlow ? (
                    <div className="my-portfolio-preview-performance">
                      <h5>{t('marketsAdd.instrumentChangesTitle')}</h5>
                      <div>
                        <span>{t('marketsAdd.periods.today')}</span>
                        <strong className={Number(instrumentPerformance.change1D ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                          {instrumentPerformance.change1D == null ? '—' : `${percentFormat.format(instrumentPerformance.change1D)}%`}
                        </strong>
                      </div>
                      <div>
                        <span>{t('marketsAdd.periods.oneMonth')}</span>
                        <strong className={Number(instrumentPerformance.change1M ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                          {instrumentPerformance.change1M == null ? '—' : `${percentFormat.format(instrumentPerformance.change1M)}%`}
                        </strong>
                      </div>
                      <div>
                        <span>{t('marketsAdd.periods.threeMonths')}</span>
                        <strong className={Number(instrumentPerformance.change3M ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                          {instrumentPerformance.change3M == null ? '—' : `${percentFormat.format(instrumentPerformance.change3M)}%`}
                        </strong>
                      </div>
                      <div>
                        <span>{t('marketsAdd.periods.sixMonths')}</span>
                        <strong className={Number(instrumentPerformance.change6M ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                          {instrumentPerformance.change6M == null ? '—' : `${percentFormat.format(instrumentPerformance.change6M)}%`}
                        </strong>
                      </div>
                      <div>
                        <span>{t('marketsAdd.periods.oneYear')}</span>
                        <strong className={Number(instrumentPerformance.change1Y ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                          {instrumentPerformance.change1Y == null ? '—' : `${percentFormat.format(instrumentPerformance.change1Y)}%`}
                        </strong>
                      </div>
                    </div>
                  ) : null}
                  <p className="my-portfolio-preview-note">{t('marketsAdd.previewNote')}</p>
                </aside>
                </>
                )}
              </div>

            </article>
          ) : null}

          {activeSection === 'portfolio' ? (
            <article className={`card my-portfolio-trade-card${isDarkTheme ? ' is-dark' : ' is-light'}`}>
              <div className="my-portfolio-history-head">
                <div>
                  <h4>Islem Gecmisi</h4>
                </div>
                <p className="my-portfolio-history-count">{historyTotalElements} kayit</p>
              </div>
              <div className="my-portfolio-history-filters">
                <input
                  value={historyFilters.symbol ?? ''}
                  onChange={(event) => setHistoryFilters((prev) => ({ ...prev, symbol: event.target.value }))}
                  placeholder="Sembol ara (AAPL, THYAO...)"
                />
                <select
                  value={historyFilters.type ?? ''}
                  onChange={(event) =>
                    setHistoryFilters((prev) => ({ ...prev, type: event.target.value as TransactionHistoryFilters['type'] }))
                  }
                >
                  <option value="">Islem Tipi (Tum)</option>
                  <option value="BUY">BUY</option>
                  <option value="SELL">SELL</option>
                </select>
                <select
                  value={historyFilters.purchaseMode ?? ''}
                  onChange={(event) =>
                    setHistoryFilters((prev) => ({
                      ...prev,
                      purchaseMode: event.target.value as TransactionHistoryFilters['purchaseMode'],
                    }))
                  }
                >
                  <option value="">Alim Tipi (Tum)</option>
                  <option value="NOW">Piyasadan</option>
                  <option value="PAST">Gecmis Alim</option>
                </select>
                <select
                  value={historyFilters.inputCurrency ?? ''}
                  onChange={(event) =>
                    setHistoryFilters((prev) => ({
                      ...prev,
                      inputCurrency: event.target.value as TransactionHistoryFilters['inputCurrency'],
                    }))
                  }
                >
                  <option value="">Odeme PB (Tum)</option>
                  <option value="TRY">TRY</option>
                  <option value="USD">USD</option>
                  <option value="EUR">EUR</option>
                </select>
                <input
                  type="date"
                  value={historyFilters.fromDate ?? ''}
                  onChange={(event) => setHistoryFilters((prev) => ({ ...prev, fromDate: event.target.value }))}
                />
                <input
                  type="date"
                  value={historyFilters.toDate ?? ''}
                  onChange={(event) => setHistoryFilters((prev) => ({ ...prev, toDate: event.target.value }))}
                />
                <button
                  type="button"
                  className="auth-submit"
                  onClick={() => {
                    setHistoryPage(0)
                    setAppliedHistoryFilters(historyFilters)
                  }}
                >
                  Filtrele
                </button>
              </div>
              {historyLoading ? (
                <div className="markets-skeleton-row" />
              ) : (
                <div className="my-portfolio-history-wrap">
                  <table>
                    <thead>
                      <tr>
                        {isAggregatePortfolioView ? <th>{t('sidebar.historyPortfolioColumn')}</th> : null}
                        <th>Enstruman</th>
                        <th>Islem</th>
                        <th>Alim Tipi</th>
                        <th>Lot</th>
                        <th>Maliyet</th>
                        <th>Liste PB</th>
                        <th>Alim kuru</th>
                        <th>Tarih</th>
                        <th>{t('history.deleteColumn')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.map((row) => (
                        <tr key={row.transactionId}>
                          {isAggregatePortfolioView ? (
                            <td>{row.portfolioName?.trim() || (row.portfolioId != null ? `#${row.portfolioId}` : '—')}</td>
                          ) : null}
                          <td>{formatInstrumentSymbolLabel(row.instrumentSymbol)}</td>
                          <td>{row.type}</td>
                          <td>
                            <span className={`my-portfolio-history-badge ${row.purchaseMode === 'PAST' ? 'is-past' : 'is-now'}`}>
                              {row.purchaseMode === 'PAST' ? 'Gecmis alim' : 'Piyasadan ekleme'}
                            </span>
                          </td>
                          <td>{row.quantity}</td>
                          <td>{formatTxMoney(row)}</td>
                          <td>
                            {row.quoteCurrency?.trim()
                              ? row.quoteCurrency.trim().toUpperCase()
                              : inferInstrumentQuoteCurrency(row.instrumentSymbol)}
                          </td>
                          <td className="my-portfolio-history-fx">{formatTxFxLegLabel(row, i18n.language)}</td>
                          <td>{new Date(row.acquiredAt ?? row.createdAt).toLocaleString(i18n.language)}</td>
                          <td>
                            <button
                              type="button"
                              className="my-portfolio-history-delete-btn"
                              onClick={() => {
                                setDeleteTxError(null)
                                setDeleteTxConfirm(row)
                              }}
                              aria-label={t('history.deleteAria')}
                            >
                              {t('history.delete')}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              {historyError ? <p className="my-portfolio-trade-note">{historyError}</p> : null}
              <div className="my-portfolio-history-pagination">
                <button type="button" className="auth-submit auth-submit-secondary" disabled={historyPage <= 0} onClick={() => setHistoryPage((p) => p - 1)}>
                  Onceki
                </button>
                <span>
                  Sayfa {historyTotalPages === 0 ? 0 : historyPage + 1} / {Math.max(historyTotalPages, 1)}
                </span>
                <button
                  type="button"
                  className="auth-submit auth-submit-secondary"
                  disabled={historyPage + 1 >= historyTotalPages}
                  onClick={() => setHistoryPage((p) => p + 1)}
                >
                  Sonraki
                </button>
              </div>
            </article>
          ) : null}

          {activeSection === 'allocation' ? (
            <article className={`card my-portfolio-trade-card my-portfolio-allocation-detail${isDarkTheme ? ' is-dark' : ' is-light'}`}>
              <header className="my-portfolio-allocation-head">
                <button
                  type="button"
                  className="auth-submit auth-submit-secondary my-portfolio-allocation-back"
                  onClick={() => setActiveSection('dashboard')}
                >
                  {t('allocation.back')}
                </button>
                <h2 className="my-portfolio-allocation-detail-title">{t('allocation.detailTitle')}</h2>
              </header>
              {portfolios.length === 0 ? (
                <div className="my-portfolio-allocation-detail-content">
                  <div className="my-portfolio-allocation-chart-row">
                    <div className="my-portfolio-allocation-dual">
                      <section className="my-portfolio-allocation-panel">
                        <header className="my-portfolio-allocation-panel-head">
                          <h3 className="my-portfolio-allocation-panel-title">{t('allocation.byInstrumentTitle')}</h3>
                        </header>
                        <AllocationDonut
                          rows={[]}
                          sharePctDisplay={sharePctDisplay}
                          currencyFormat={displayDashboardCurrencyFormat}
                          ariaLabel={`${t('allocation.byInstrumentTitle')} — ${t('distributionTitle')}`}
                        />
                      </section>
                      <section className="my-portfolio-allocation-panel">
                        <header className="my-portfolio-allocation-panel-head">
                          <h3 className="my-portfolio-allocation-panel-title">{t('allocation.byCategoryTitle')}</h3>
                        </header>
                        <AllocationDonut
                          rows={[]}
                          sharePctDisplay={sharePctDisplay}
                          currencyFormat={displayDashboardCurrencyFormat}
                          ariaLabel={`${t('allocation.byCategoryTitle')} — ${t('distributionTitle')}`}
                          donutVariant="category"
                        />
                      </section>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="my-portfolio-allocation-detail-content">
                  <div className="my-portfolio-allocation-chart-row">
                    <div className="my-portfolio-allocation-dual">
                      <section className="my-portfolio-allocation-panel">
                        <header className="my-portfolio-allocation-panel-head">
                          <h3 className="my-portfolio-allocation-panel-title">{t('allocation.byInstrumentTitle')}</h3>
                        </header>
                        <AllocationDonut
                          rows={instrumentDonutRows}
                          sharePctDisplay={sharePctDisplay}
                          currencyFormat={displayDashboardCurrencyFormat}
                          ariaLabel={`${t('allocation.byInstrumentTitle')} — ${t('distributionTitle')}`}
                        />
                      </section>
                      <section className="my-portfolio-allocation-panel">
                        <header className="my-portfolio-allocation-panel-head">
                          <h3 className="my-portfolio-allocation-panel-title">{t('allocation.byCategoryTitle')}</h3>
                        </header>
                        <AllocationDonut
                          rows={categoryDonutRows}
                          sharePctDisplay={sharePctDisplay}
                          currencyFormat={displayDashboardCurrencyFormat}
                          ariaLabel={`${t('allocation.byCategoryTitle')} — ${t('distributionTitle')}`}
                          donutVariant="category"
                        />
                      </section>
                    </div>
                  </div>
                  {distribution.fullList.length === 0 ? (
                    <p className="my-portfolio-trade-note">{t('allocation.empty')}</p>
                  ) : (
                  <div className="my-portfolio-allocation-table-wrap">
                    <table className="my-portfolio-allocation-table">
                      <colgroup>
                        <col className="my-portfolio-alloc-col-sym" />
                        <col className="my-portfolio-alloc-col-qty" />
                        <col className="my-portfolio-alloc-col-cost" />
                        <col className="my-portfolio-alloc-col-avg" />
                        <col className="my-portfolio-alloc-col-cur" />
                        <col className="my-portfolio-alloc-col-w" />
                        <col className="my-portfolio-alloc-col-val" />
                        <col className="my-portfolio-alloc-col-pnl" />
                      </colgroup>
                      <thead>
                        <tr>
                          <th
                            scope="col"
                            aria-sort={
                              allocationSortKey === 'symbol'
                                ? allocationSortDir === 'asc'
                                  ? 'ascending'
                                  : 'descending'
                                : 'none'
                            }
                          >
                            <button
                              type="button"
                              className={`my-portfolio-alloc-sort-btn${allocationSortKey === 'symbol' ? ' is-active' : ''}`}
                              onClick={() => setAllocationColumnSort('symbol')}
                              title={`${t('allocation.symbol')} — ${
                                allocationSortKey === 'symbol'
                                  ? allocationSortDir === 'asc'
                                    ? t('allocation.sortAsc')
                                    : t('allocation.sortDesc')
                                  : t('allocation.sortActivate')
                              }`}
                            >
                              <span>{t('allocation.symbol')}</span>
                              {allocationSortKey === 'symbol' ? (
                                <span className="my-portfolio-alloc-sort-ico" aria-hidden>
                                  {allocationSortDir === 'asc' ? '▲' : '▼'}
                                </span>
                              ) : null}
                            </button>
                          </th>
                          {(
                            [
                              ['quantity', t('allocation.quantity')],
                              ['cost', t('allocation.cost')],
                              ['avgBuyPrice', t('allocation.avgBuyPrice')],
                              ['currentPrice', t('allocation.currentPrice')],
                              ['weight', t('allocation.weight')],
                              ['value', t('allocation.value')],
                              ['pnl', t('allocation.pnlPctShort')],
                            ] as const
                          ).map(([colKey, label]) => (
                            <th
                              key={colKey}
                              scope="col"
                              className="my-portfolio-allocation-num"
                              aria-sort={
                                allocationSortKey === colKey
                                  ? allocationSortDir === 'asc'
                                    ? 'ascending'
                                    : 'descending'
                                  : 'none'
                              }
                            >
                              <button
                                type="button"
                                className={`my-portfolio-alloc-sort-btn is-num${
                                  allocationSortKey === colKey ? ' is-active' : ''
                                }`}
                                onClick={() => setAllocationColumnSort(colKey)}
                                title={`${label} — ${
                                  allocationSortKey === colKey
                                    ? allocationSortDir === 'asc'
                                      ? t('allocation.sortAsc')
                                      : t('allocation.sortDesc')
                                    : t('allocation.sortActivate')
                                }`}
                              >
                                <span>{label}</span>
                                {allocationSortKey === colKey ? (
                                  <span className="my-portfolio-alloc-sort-ico" aria-hidden>
                                    {allocationSortDir === 'asc' ? '▲' : '▼'}
                                  </span>
                                ) : null}
                              </button>
                            </th>
                          ))}
                          {!isAggregatePortfolioView && selectedPortfolioId != null && selectedPortfolioId > 0 ? (
                            <th scope="col">{t('allocation.action')}</th>
                          ) : null}
                        </tr>
                      </thead>
                      <tbody>
                        {allocationTableRows.map((row) => (
                          <tr key={row.symbol}>
                            <td>
                              <span className="my-portfolio-allocation-symbol">
                                <span className={`my-portfolio-dot ${row.colorClass}`} />
                                {formatInstrumentSymbolLabel(row.symbol)}
                              </span>
                            </td>
                            <td className="my-portfolio-allocation-num my-portfolio-allocation-muted">
                              {formatHoldingQuantity(row.quantity, i18n.language)}
                            </td>
                            <td className="my-portfolio-allocation-num">
                              {displayDashboardCurrencyFormat.format(row.totalCost)}
                            </td>
                            <td className="my-portfolio-allocation-num my-portfolio-allocation-muted">
                              {row.avgBuyPrice > 0
                                ? displayDashboardCurrencyFormat.format(row.avgBuyPrice)
                                : '—'}
                            </td>
                            <td className="my-portfolio-allocation-num my-portfolio-allocation-muted">
                              {row.currentPrice > 0
                                ? displayDashboardCurrencyFormat.format(row.currentPrice)
                                : '—'}
                            </td>
                            <td className="my-portfolio-allocation-num">{sharePctDisplay.format(row.sharePct)}%</td>
                            <td className="my-portfolio-allocation-num">{displayDashboardCurrencyFormat.format(row.value)}</td>
                            <td
                              className={`my-portfolio-allocation-num${row.pnlPercent > 1e-9 ? ' my-portfolio-up' : row.pnlPercent < -1e-9 ? ' my-portfolio-down' : ''}`}
                            >
                              {dashboardPctFormat.format(row.pnlPercent)}%
                            </td>
                            {!isAggregatePortfolioView && selectedPortfolioId != null && selectedPortfolioId > 0 ? (
                              <td>
                                {row.quantity > 0 ? (
                                  <button
                                    type="button"
                                    className="my-portfolio-alloc-sell-btn"
                                    onClick={() => openSellFromAllocation(row.symbol)}
                                  >
                                    {t('allocation.sell')}
                                  </button>
                                ) : null}
                              </td>
                            ) : null}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  )}
                </div>
              )}
            </article>
          ) : null}

          {activeSection === 'salesAnalysis' ? (
            <article className={`card my-portfolio-trade-card${isDarkTheme ? ' is-dark' : ' is-light'}`}>
              <div className="my-portfolio-history-head">
                <div>
                  <h4>{t('salesAnalysis.title')}</h4>
                </div>
                <p className="my-portfolio-history-count">{salesAnalysisTotalElements} {t('salesAnalysis.records')}</p>
              </div>
              <div className="my-portfolio-history-filters">
                <input
                  value={salesAnalysisFilters.symbol ?? ''}
                  onChange={(event) => setSalesAnalysisFilters((prev) => ({ ...prev, symbol: event.target.value }))}
                  placeholder={t('salesAnalysis.symbolPlaceholder')}
                />
                <input
                  type="date"
                  value={salesAnalysisFilters.fromDate ?? ''}
                  onChange={(event) => setSalesAnalysisFilters((prev) => ({ ...prev, fromDate: event.target.value }))}
                />
                <input
                  type="date"
                  value={salesAnalysisFilters.toDate ?? ''}
                  onChange={(event) => setSalesAnalysisFilters((prev) => ({ ...prev, toDate: event.target.value }))}
                />
                <button
                  type="button"
                  className="auth-submit"
                  onClick={() => {
                    setSalesAnalysisPage(0)
                    setAppliedSalesAnalysisFilters(salesAnalysisFilters)
                  }}
                >
                  {t('salesAnalysis.filter')}
                </button>
              </div>
              {salesAnalysisLoading ? (
                <div className="markets-skeleton-row" />
              ) : (
                <div className="my-portfolio-history-wrap my-portfolio-sales-analysis-wrap">
                  <table>
                    <thead>
                      <tr>
                        {isAggregatePortfolioView ? <th>{t('sidebar.historyPortfolioColumn')}</th> : null}
                        <th>{t('salesAnalysis.instrument')}</th>
                        <th>{t('salesAnalysis.quantity')}</th>
                        <th>{t('salesAnalysis.avgCostAtSell')}</th>
                        <th>{t('salesAnalysis.sellUnitPrice')}</th>
                        <th>{t('salesAnalysis.sellProceeds')}</th>
                        <th>{t('salesAnalysis.realizedPnl')}</th>
                        <th>{t('salesAnalysis.hypotheticalValueNow')}</th>
                        <th>{t('salesAnalysis.opportunityDelta')}</th>
                        <th>{t('salesAnalysis.soldAt')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {salesAnalysisRows.map((row) => {
                        const qcRaw = row.quoteCurrency?.trim().toUpperCase() || dashboardCurrency
                        const rowCurrency: TradePaymentCurrency =
                          qcRaw === 'TRY' || qcRaw === 'USD' || qcRaw === 'EUR'
                            ? qcRaw
                            : dashboardCurrency === 'TRY' ||
                                dashboardCurrency === 'USD' ||
                                dashboardCurrency === 'EUR'
                              ? dashboardCurrency
                              : 'USD'
                        const fmt = (n: number | null | undefined, digits = 2) =>
                          n == null || !Number.isFinite(n)
                            ? '—'
                            : formatMoneyPrefixed(n, rowCurrency, i18n.language, digits)
                        return (
                          <tr key={row.transactionId}>
                            {isAggregatePortfolioView ? (
                              <td>{row.portfolioName?.trim() || (row.portfolioId != null ? `#${row.portfolioId}` : '—')}</td>
                            ) : null}
                            <td>{formatInstrumentSymbolLabel(row.instrumentSymbol)}</td>
                            <td>{formatHoldingQuantity(row.quantity, i18n.language)}</td>
                            <td>{fmt(row.avgCostAtSell, 6)}</td>
                            <td>{fmt(row.sellUnitPrice, 6)}</td>
                            <td>{fmt(row.sellProceeds)}</td>
                            <td className={row.realizedPnl >= 0 ? 'my-portfolio-up' : 'my-portfolio-down'}>
                              {fmt(row.realizedPnl)} ({dashboardPctFormat.format(row.realizedPnlPct)}%)
                            </td>
                            <td>{fmt(row.hypotheticalValueNow)}</td>
                            <td className={(row.opportunityDelta ?? 0) >= 0 ? 'my-portfolio-up' : 'my-portfolio-down'}>
                              {fmt(row.opportunityDelta)}
                            </td>
                            <td>{new Date(row.soldAt).toLocaleString(i18n.language)}</td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              )}
              {salesAnalysisError ? <p className="my-portfolio-trade-note">{salesAnalysisError}</p> : null}
              <div className="my-portfolio-history-pagination">
                <button
                  type="button"
                  className="auth-submit auth-submit-secondary"
                  disabled={salesAnalysisPage <= 0}
                  onClick={() => setSalesAnalysisPage((p) => p - 1)}
                >
                  {t('salesAnalysis.prev')}
                </button>
                <span>
                  {t('salesAnalysis.pageOf', {
                    current: salesAnalysisTotalPages === 0 ? 0 : salesAnalysisPage + 1,
                    total: Math.max(salesAnalysisTotalPages, 1),
                  })}
                </span>
                <button
                  type="button"
                  className="auth-submit auth-submit-secondary"
                  disabled={salesAnalysisPage + 1 >= salesAnalysisTotalPages}
                  onClick={() => setSalesAnalysisPage((p) => p + 1)}
                >
                  {t('salesAnalysis.next')}
                </button>
              </div>
            </article>
          ) : null}

          {activeSection === 'news' ? <MyNewsPanel embedded /> : null}

          {activeSection === 'analysis' ? <MyAnalysisPanel embedded /> : null}

          {activeSection === 'targets' ? (
            <PortfolioGoalsPanel
              portfolioId={overviewApiPortfolioId}
              displayCurrency={displayCurrency}
              moneyFormat={displayDashboardCurrencyFormat}
              percentFormat={dashboardPctFormat}
              hideMoney={hideMoney}
              isDarkTheme={isDarkTheme}
            />
          ) : null}

          {activeSection === 'watchlist' ? (
            <Suspense fallback={<div className="markets-skeleton-row" />}>
              <MyPortfolioWatchlistSection currencyFormat={currencyFormat} percentFormat={percentFormat} />
            </Suspense>
          ) : null}

          {activeSection === 'settings' ? (
            <article className={`card my-portfolio-trade-card${isDarkTheme ? ' is-dark' : ' is-light'}`}>
              <h2 className="my-portfolio-trade-title">{t('settingsPage.title')}</h2>
              <p className="my-portfolio-trade-subtitle">{t('settingsPage.scopeHint')}</p>
              {portfolioSettingsError ? <p className="auth-error">{portfolioSettingsError}</p> : null}
              {selectedPortfolio == null ? (
                <p className="my-portfolio-trade-subtitle">
                  {isAggregatePortfolioView ? t('sidebar.settingsOverviewHint') : t('settingsPage.pickPortfolio')}
                </p>
              ) : (
                <>
                  <div
                    className="my-portfolio-settings-active-summary"
                    role="region"
                    aria-label={t('settingsPage.activeKicker')}
                  >
                    <p className="my-portfolio-settings-active-summary-kicker">{t('settingsPage.activeKicker')}</p>
                    <h3 className="my-portfolio-settings-active-summary-name" title={t('settingsPage.portfolioNameLabel')}>
                      {selectedPortfolio.name}
                    </h3>
                    <dl className="my-portfolio-settings-dl">
                      <div>
                        <dt>{t('settingsPage.createdAtLabel')}</dt>
                        <dd>
                          {settingsPortfolioCreatedDisplay ?? t('settingsPage.createdUnknown')}
                        </dd>
                      </div>
                      <div>
                        <dt>{t('settingsPage.baseCurrencyLabel')}</dt>
                        <dd>
                          {selectedPortfolio.baseCurrency?.trim().toUpperCase() === 'MIXED'
                            ? t('settingsPage.baseCurrencyNeutral')
                            : selectedPortfolio.baseCurrency}
                        </dd>
                      </div>
                      <div>
                        <dt>{t('settingsPage.quotaLabel')}</dt>
                        <dd>
                          {t('settingsPage.quotaSummary', {
                            used: portfolios.length,
                            max: MAX_USER_PORTFOLIOS,
                          })}
                        </dd>
                      </div>
                    </dl>
                  </div>

                  <h4 className="my-portfolio-settings-section-title">{t('settingsPage.privacySection')}</h4>
                  <label className="my-portfolio-settings-toggle" htmlFor="pf-hide-amounts">
                    <input
                      id="pf-hide-amounts"
                      type="checkbox"
                      checked={hideMoney}
                      disabled={amountsHiddenSaving || deletePortfolioSubmitting}
                      aria-describedby="pf-hide-amounts-help"
                      onChange={(event) => void handleToggleAmountsHidden(selectedPortfolio.id, event.target.checked)}
                    />
                    <span>{t('settingsPage.hideAmountsLabel')}</span>
                  </label>
                  <p id="pf-hide-amounts-help" className="my-portfolio-trade-subtitle my-portfolio-settings-help">
                    {t('settingsPage.hideAmountsHelp')}
                  </p>

                  <h4 className="my-portfolio-settings-section-title">{t('settingsPage.dangerSection')}</h4>
                  <p className="my-portfolio-trade-subtitle my-portfolio-settings-help">{t('settingsPage.deleteCtaHint')}</p>
                  <div className="my-portfolio-settings-actions">
                    <button
                      type="button"
                      className="auth-submit"
                      disabled={deletePortfolioSubmitting || amountsHiddenSaving}
                      onClick={() => {
                        setPortfolioSettingsError(null)
                        setDeleteConfirm({ id: selectedPortfolio.id, name: selectedPortfolio.name })
                      }}
                    >
                      {t('settingsPage.deleteCta')}
                    </button>
                  </div>
                </>
              )}
            </article>
          ) : null}

          {activeSection === 'dashboard' ? (
            <>
              <div className="my-portfolio-grid my-portfolio-grid--dashboard">
            <article className="card my-portfolio-card my-portfolio-card--dashboard-value">
              <div className="my-portfolio-card-head">
                <h3>{t('valueTitle')}</h3>
              </div>
              <div className="my-portfolio-dashboard-value-layout">
                <div className="my-portfolio-dashboard-value-copy">
                  <div
                    className="my-portfolio-dashboard-value-badges"
                    role="tablist"
                    aria-label={t('valueChart.metricAria')}
                  >
                    <button
                      type="button"
                      role="tab"
                      aria-selected={valueChartMetric === 'value'}
                      className={`my-portfolio-dashboard-value-badge${valueChartMetric === 'value' ? ' is-active' : ''}`}
                      onClick={() => setValueChartMetric('value')}
                    >
                      <span className="my-portfolio-dashboard-value-badge-icon">
                        <PortfolioDashboardGlyph kind="currency" />
                      </span>
                      {valueChartCurrencyCode}
                    </button>
                    <button
                      type="button"
                      role="tab"
                      aria-selected={valueChartMetric === 'performance'}
                      className={`my-portfolio-dashboard-value-badge${valueChartMetric === 'performance' ? ' is-active' : ''}`}
                      onClick={() => setValueChartMetric('performance')}
                    >
                      <span className="my-portfolio-dashboard-value-badge-icon">
                        <PortfolioDashboardGlyph kind="performance" />
                      </span>
                      {t('valueChart.metricPerformance')}
                    </button>
                  </div>
                  <p className="my-portfolio-main-value">
                    {selectedPortfolioId == null
                      ? '—'
                      : overview
                        ? displayDashboardCurrencyFormat.format(Number(overview.totalValue ?? 0))
                        : '…'}
                  </p>
                  <p
                    className={`my-portfolio-sub-value${
                      (() => {
                        if (!overview) return ''
                        const dod = parseApiDecimal(overview.dayOverDayChange, 0)
                        if (dod > DOD_EPS) return ' my-portfolio-dod-pos'
                        if (dod < -DOD_EPS) return ' my-portfolio-dod-neg'
                        return ' my-portfolio-dod-neutral'
                      })()
                    }`}
                  >
                    {selectedPortfolioId == null ? null : !overview ? (
                      '…'
                    ) : (
                      (() => {
                        const totalVal = parseApiDecimal(overview.totalValue, 0)
                        const dod = parseApiDecimal(overview.dayOverDayChange, 0)
                        const priorClose = totalVal - dod
                        const showPct = Math.abs(priorClose) >= DOD_EPS
                        const pct = showPct ? (dod / priorClose) * 100 : null
                        return (
                          <>
                            <span className="my-portfolio-dod-amount">{displayDashboardDayChangeFormat.format(dod)}</span>
                            {pct != null && showPct ? (
                              <span className="my-portfolio-dod-pct">
                                {' '}
                                ({hideMoney ? '•••' : dashboardPctFormat.format(pct)}%)
                              </span>
                            ) : null}
                            <span className="my-portfolio-dod-suffix">
                              {' '}
                              {t('sinceYesterday')}
                            </span>
                          </>
                        )
                      })()
                    )}
                  </p>
                  {valueChartPointsLabel ? (
                    <p className="my-portfolio-dashboard-value-series-note">
                      <span className="my-portfolio-dashboard-value-badge-icon">
                        <PortfolioDashboardGlyph kind="history" />
                      </span>
                      {valueChartPointsLabel}
                    </p>
                  ) : null}
                </div>
                {selectedPortfolioId == null ? null : (
                  <div className="my-portfolio-dashboard-value-chart">
                    <div className="my-portfolio-dashboard-value-chart-shell">
                      <div className="my-portfolio-dashboard-value-chart-panel">
                        <div className="my-portfolio-dashboard-value-chart-head">
                          <div>
                            <p className="my-portfolio-dashboard-value-chart-label">
                              {valueChartMetric === 'performance'
                                ? t('valueChart.performanceTitle')
                                : t('valueChart.marketValueTitle')}
                            </p>
                            <p className="my-portfolio-dashboard-value-chart-meta">
                              <span>{valueChartRangeLabel(valueChartRange)}</span>
                              {valueChartPointsLabel ? (
                                <>
                                  <span className="my-portfolio-dashboard-value-chart-meta-sep">·</span>
                                  <span>{valueChartPointsLabel}</span>
                                </>
                              ) : null}
                            </p>
                          </div>
                          <div className="my-portfolio-dashboard-value-range-scroller-wrap">
                            <div
                              ref={valueChartRangeRailRef}
                              className="my-portfolio-dashboard-value-range-scroller"
                              role="tablist"
                              aria-label={t('valueChart.rangeAria')}
                              onPointerDown={handleValueChartRangePointerDown}
                              onPointerMove={handleValueChartRangePointerMove}
                              onPointerUp={handleValueChartRangePointerEnd}
                              onPointerCancel={handleValueChartRangePointerEnd}
                              onClickCapture={handleValueChartRangeClickCapture}
                            >
                              {VALUE_CHART_RANGES.map((range) => (
                                <button
                                  key={range}
                                  type="button"
                                  className={`my-portfolio-chip my-portfolio-chip--range${valueChartRange === range ? ' is-active' : ''}`}
                                  onClick={() => setValueChartRange(range)}
                                >
                                  {valueChartRangeLabel(range)}
                                </button>
                              ))}
                            </div>
                          </div>
                        </div>
                        {performanceSeriesHydrated ? (
                          <PortfolioValueHistoryChart
                            key={`${valueChartMetric}-${valueChartRange}-${performanceSeries?.points[0]?.day ?? 'empty'}-${performanceSeries != null && performanceSeries.points.length > 0 ? performanceSeries.points[performanceSeries.points.length - 1]!.day : 'empty'}-${performanceSeries?.points.length ?? 0}`}
                            series={performanceSeries}
                            metric={valueChartMetric}
                            range={valueChartRange}
                            height={236}
                            isDark={isDarkTheme}
                            locale={i18n.language}
                            maskAmounts={hideMoney}
                            moneyFormatter={displayDashboardCurrencyFormat}
                            percentFormatter={dashboardPctFormat}
                            emptyLabel={t('valueChart.empty')}
                          />
                        ) : (
                          <div className="my-portfolio-value-chart-empty">{t('valueChart.loading')}</div>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </article>

            <article
              className="card my-portfolio-card my-portfolio-card--dashboard-trade"
              aria-labelledby="portfolio-change-heading"
            >
              <div className="my-portfolio-card-head">
                <h3 id="portfolio-change-heading">{t('changeTitle')}</h3>
              </div>
              <div className="my-portfolio-trade-flow-net">
                {selectedPortfolioId == null
                  ? '—'
                  : !tradeFlowHydrated
                    ? t('tradeFlow.loading')
                    : hideMoney
                      ? '•••'
                      : displayDashboardCurrencyFormat.format(tradeFlowStats.net)}
              </div>
              <div className="my-portfolio-trade-flow-totals">
                <div>
                  <span className="trade-flow-stat-label">{t('tradeFlow.buyLabel')}</span>
                  <strong>
                    {selectedPortfolioId == null
                      ? '—'
                      : !tradeFlowHydrated
                        ? t('tradeFlow.loading')
                        : displayDashboardCurrencyFormat.format(tradeFlowStats.buy)}
                  </strong>
                </div>
                <div>
                  <span className="trade-flow-stat-label">{t('tradeFlow.diffLabel')}</span>
                  <strong className="trade-flow-fark-val">
                    {selectedPortfolioId == null
                      ? '—'
                      : tradeFlowFark.kind === 'loading'
                        ? t('tradeFlow.loading')
                        : tradeFlowFark.kind === 'empty'
                          ? '—'
                          : tradeFlowFark.kind === 'infinity'
                            ? `+${t('tradeFlow.pctInfinity')}%`
                            : Math.abs(tradeFlowFark.pct) < 1e-6
                              ? '0%'
                              : `${percentFormat.format(tradeFlowFark.pct)}%`}
                  </strong>
                </div>
                <div>
                  <span className="trade-flow-stat-label">{t('tradeFlow.sellLabel')}</span>
                  <strong>
                    {selectedPortfolioId == null
                      ? '—'
                      : !tradeFlowHydrated
                        ? t('tradeFlow.loading')
                        : displayDashboardCurrencyFormat.format(tradeFlowStats.sell)}
                  </strong>
                </div>
              </div>
              {selectedPortfolioId == null ? null : (
                <PortfolioHistorySparkline
                  variant="tradeFlow"
                  tradeFlowPoints={tradeFlow?.points ?? []}
                  isDark={isDarkTheme}
                  locale={i18n.language}
                  maskAmounts={hideMoney}
                  formatValue={(v) => displayDashboardCurrencyFormat.format(v)}
                  emptyLabel={t('tradeFlow.empty')}
                />
              )}
            </article>

            <article className="card my-portfolio-card my-portfolio-card--dashboard-profit">
              <div className="my-portfolio-card-head">
                <h3>{t('profitTitle')}</h3>
              </div>
              {selectedPortfolioId == null ? (
                <p className="my-portfolio-pnl-donut-empty">—</p>
              ) : !overview ? (
                <p className="my-portfolio-pnl-donut-empty">…</p>
              ) : (
                <PnlSplitDonut
                  items={overview.items}
                  totalPnl={parseApiDecimal(overview.totalPnl, 0)}
                  totalPnlPercent={parseApiDecimal(overview.totalPnlPercent, 0)}
                  currencyFormat={displayDashboardCurrencyFormat}
                  pctFormat={dashboardPctFormat}
                  sharePctDisplay={sharePctDisplay}
                  hideAmounts={hideMoney}
                />
              )}
            </article>

            <article
              className="card my-portfolio-card my-portfolio-card-dist-preview my-portfolio-card--dashboard-allocation"
              aria-labelledby="portfolio-distribution-heading"
            >
              <div className="my-portfolio-card-head">
                <h3 id="portfolio-distribution-heading">{t('distributionTitle')}</h3>
                <button
                  type="button"
                  disabled={selectedPortfolioId == null}
                  onClick={() => setActiveSection('allocation')}
                >
                  {t('actions.viewAll')}
                </button>
              </div>
              <div className="my-portfolio-dashboard-preview-body">
                {selectedPortfolioId == null || distribution.bar.length === 0 ? (
                  <div className="my-portfolio-dist-preview my-portfolio-dist-preview--empty">
                    <AllocationDonut
                      rows={[]}
                      currencyFormat={displayDashboardCurrencyFormat}
                      sharePctDisplay={sharePctDisplay}
                      ariaLabel={`${t('allocation.byInstrumentTitle')} — ${t('distributionTitle')}`}
                    />
                    <p className="my-portfolio-dashboard-preview-empty">{t('allocation.empty')}</p>
                  </div>
                ) : (
                  <div className="my-portfolio-dist-preview my-portfolio-dist-dashboard">
                    <AllocationDonut
                      rows={instrumentDonutRows}
                      currencyFormat={displayDashboardCurrencyFormat}
                      sharePctDisplay={sharePctDisplay}
                      ariaLabel={`${t('allocation.byInstrumentTitle')} — ${t('distributionTitle')}`}
                    />
                    <div className="my-portfolio-dist-list" role="table" aria-label={t('distributionTitle')}>
                      <div className="my-portfolio-dist-list-head" role="row">
                        <span role="columnheader">{t('allocation.symbol')}</span>
                        <span role="columnheader">{t('allocation.weight')}</span>
                        <span role="columnheader">{t('allocation.value')}</span>
                      </div>
                      <ul className="my-portfolio-asset-list my-portfolio-asset-list--dist-preview" role="rowgroup">
                        {distribution.topList.map((row) => (
                          <li key={row.symbol} role="row">
                            <span className="my-portfolio-dist-list-symbol" role="cell">
                              <span className={`my-portfolio-dot ${row.colorClass}`} aria-hidden />
                              <strong>{formatInstrumentSymbolLabel(row.symbol)}</strong>
                            </span>
                            <span className="my-portfolio-dist-list-weight" role="cell">
                              {sharePctDisplay.format(row.sharePct)}%
                            </span>
                            <span className="my-portfolio-dist-list-value" role="cell">
                              {displayDashboardCurrencyFormat.format(row.value)}
                            </span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                )}
              </div>
            </article>

            <article
              className="card my-portfolio-card my-portfolio-card-tx-preview my-portfolio-card--dashboard-tx"
              aria-labelledby="portfolio-recent-tx-heading"
            >
              <div className="my-portfolio-card-head">
                <h3 id="portfolio-recent-tx-heading">{t('transactionHistoryTitle')}</h3>
                <button
                  type="button"
                  disabled={selectedPortfolioId == null}
                  onClick={() => setActiveSection('portfolio')}
                >
                  {t('actions.viewAll')}
                </button>
              </div>
              <div className="my-portfolio-dashboard-preview-body">
                {recentTxLoading ? (
                  <div className="markets-skeleton-row" aria-hidden />
                ) : (
                  <div className="my-portfolio-tx-preview">
                    <div className="my-portfolio-dashboard-mini-table-wrap">
                      <table className="my-portfolio-dashboard-mini-table my-portfolio-dashboard-mini-table--tx">
                        <thead>
                          <tr>
                            <th scope="col">{t('allocation.symbol')}</th>
                            <th scope="col" className="is-num">
                              {t('allocation.quantity')}
                            </th>
                            <th scope="col" className="is-num">
                              {t('allocation.value')}
                            </th>
                            <th scope="col" className="is-tx-type">
                              {t('transactionHistorySide')}
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {recentTxPreview.length === 0 ? (
                            <tr>
                              <td colSpan={4} className="my-portfolio-dashboard-preview-slot-empty">
                                {t('transactionHistoryEmpty')}
                              </td>
                            </tr>
                          ) : (
                            recentTxPreview.map((row) => {
                            const isBuy = String(row.type).toUpperCase() === 'BUY'
                            const qty = Number(row.quantity)
                            const qtyStr = Number.isFinite(qty)
                              ? Math.abs(qty - Math.round(qty)) < 1e-9
                                ? String(Math.round(qty))
                                : new Intl.NumberFormat(i18n.language, { maximumFractionDigits: 6 }).format(qty)
                              : '—'
                            return (
                              <tr key={row.transactionId}>
                                <td>
                                  <span className="my-portfolio-dashboard-mini-symbol">{formatInstrumentSymbolLabel(row.instrumentSymbol)}</span>
                                </td>
                                <td className="is-num is-muted">× {qtyStr}</td>
                                <td className="is-num">
                                  <span className="my-portfolio-tx-preview-amt">{formatTxMoney(row)}</span>
                                </td>
                                <td className="is-tx-type">
                                  <span className={`my-portfolio-recent-tx-badge${isBuy ? ' is-buy' : ' is-sell'}`}>
                                    <svg className="my-portfolio-recent-tx-badge-ico" viewBox="0 0 12 12" aria-hidden>
                                      {isBuy ? (
                                        <path fill="currentColor" d="M6 2.2 10.2 8.8H1.8z" />
                                      ) : (
                                        <path fill="currentColor" d="M6 9.8 1.8 3.2h8.4z" />
                                      )}
                                    </svg>
                                    {isBuy ? t('txTypeBuy') : t('txTypeSell')}
                                  </span>
                                </td>
                              </tr>
                            )
                          })
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
            </article>

            <TargetsInsightCard
              portfolioId={overviewApiPortfolioId}
              displayCurrency={valuationCurrency}
              moneyFormat={displayDashboardCurrencyFormat}
              percentFormat={dashboardPctFormat}
              hideMoney={hideMoney}
            />
            <FavoriteNewsInsightCard />
              </div>
            </>
          ) : null}
        </div>
      </div>
      {deleteConfirm ? (
        <div className="my-portfolio-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="pf-del-title">
          <div className="my-portfolio-modal card">
            <h3 id="pf-del-title">{t('settingsPage.deleteModalTitle')}</h3>
            <p>{t('settingsPage.deleteModalBody', { name: deleteConfirm.name })}</p>
            <div className="my-portfolio-modal-actions">
              <button
                type="button"
                className="auth-submit auth-submit-secondary"
                disabled={deletePortfolioSubmitting}
                onClick={() => setDeleteConfirm(null)}
              >
                {t('settingsPage.cancel')}
              </button>
              <button
                type="button"
                className="auth-submit"
                disabled={deletePortfolioSubmitting}
                onClick={() => void handleConfirmDeletePortfolio()}
              >
                {deletePortfolioSubmitting ? t('settingsPage.deleting') : t('settingsPage.confirmDelete')}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {deleteTxConfirm ? (
        <div className="my-portfolio-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="tx-del-title">
          <div className="my-portfolio-modal card">
            <h3 id="tx-del-title">{t('history.deleteModalTitle')}</h3>
            <p>{t('history.deleteModalBody')}</p>
            {deleteTxError ? <p className="auth-error">{deleteTxError}</p> : null}
            <div className="my-portfolio-modal-actions">
              <button
                type="button"
                className="auth-submit auth-submit-secondary"
                disabled={deleteTxSubmitting}
                onClick={() => {
                  setDeleteTxConfirm(null)
                  setDeleteTxError(null)
                }}
              >
                {t('history.deleteCancel')}
              </button>
              <button
                type="button"
                className="auth-submit"
                disabled={deleteTxSubmitting}
                onClick={() => void handleConfirmDeleteTransaction()}
              >
                {deleteTxSubmitting ? t('history.deleting') : t('history.deleteConfirm')}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {showCreatePortfolioModal ? (
        <div className="my-portfolio-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="pf-create-title">
          <div className="my-portfolio-modal card">
            <h4 id="pf-create-title">
              {portfolios.length === 0 ? t('createPortfolioModal.titleFirst') : t('createPortfolioModal.titleMore')}
            </h4>
            <p>
              {portfolios.length === 0 ? t('createPortfolioModal.leadFirst') : t('createPortfolioModal.leadMore')}
            </p>
            <label className="my-portfolio-modal-field">
              <span>{t('createPortfolioModal.nameLabel')}</span>
              <input
                value={newPortfolioName}
                onChange={(event) => setNewPortfolioName(event.target.value)}
                placeholder={t('createPortfolioModal.namePlaceholder')}
                maxLength={120}
              />
            </label>
            {portfolioActionError ? <p className="auth-error">{portfolioActionError}</p> : null}
            <div className="my-portfolio-modal-actions">
              {portfolios.length > 0 ? (
                <button
                  type="button"
                  className="auth-submit auth-submit-secondary"
                  onClick={() => setShowCreatePortfolioModal(false)}
                  disabled={portfolioActionLoading}
                >
                  {t('createPortfolioModal.cancel')}
                </button>
              ) : null}
              <button type="button" className="auth-submit" onClick={() => void handleCreatePortfolio()} disabled={portfolioActionLoading}>
                {portfolioActionLoading ? t('createPortfolioModal.creating') : t('createPortfolioModal.create')}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
