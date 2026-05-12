import { Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { fetchMarketOverview } from '../../features/markets/api/marketService'
import {
  buyTrade,
  createPortfolio,
  deletePortfolio,
  getInstrumentPriceCoverage,
  getMyPortfolioOverview,
  getPortfolioSnapshots,
  getPortfolios,
  getTransactionHistory,
  getTransactionHistoryPage,
  patchPortfolioAmountsHidden,
  previewTrade,
} from '../../features/portfolio/api/portfolioApi'
import type {
  Portfolio,
  PortfolioOverview,
  PortfolioOverviewItem,
  PortfolioTradeFlow,
  PortfolioValueSnapshot,
  PurchaseMode,
  TradeInputMode,
  TransactionHistoryFilters,
  TransactionHistoryItem,
} from '../../shared/types/portfolio'
import { AllocationDonut, type AllocationCategoryGroup, type AllocationDonutRow } from './components/AllocationDonut'
import { PnlSplitDonut } from './components/PnlSplitDonut'
import { PortfolioValueHistoryChart } from './components/PortfolioValueHistoryChart'
import { TradeFlowHistoryChart, tradeFlowPeriodTotals } from './components/TradeFlowHistoryChart'
import { loadTradeFlowForPortfolio } from '../../features/portfolio/lib/loadTradeFlowForPortfolio'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'

const MyPortfolioWatchlistSection = lazy(async () => {
  const mod = await import('./components/MyPortfolioWatchlistSection')
  return { default: mod.MyPortfolioWatchlistSection }
})

const MAX_USER_PORTFOLIOS = 5
const CREATE_PORTFOLIO_SELECT_VALUE = '__create_portfolio__'
const MASKED_MONEY_LABEL = '••••'

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

/** Listing quote bucket for filtering instruments to a portfolio base (TRY vs USD). */
function marketOptionListingQuote(opt: MarketOption): 'TRY' | 'USD' {
  const n = (opt.nativeQuote ?? '').trim().toUpperCase()
  if (n === 'TRY') return 'TRY'
  if (n === 'USD' || n === 'EUR') return 'USD'
  return inferInstrumentQuoteCurrency(opt.symbol) === 'TRY' ? 'TRY' : 'USD'
}

const RECENT_TX_PREVIEW_SIZE = 4

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

const DISTRIBUTION_TOP_N = 4
const DISTRIBUTION_BAR_COLORS = [
  'my-portfolio-dot-blue',
  'my-portfolio-dot-purple',
  'my-portfolio-dot-pink',
  'my-portfolio-dot-green',
] as const

type DistributionBarSeg = { key: string; widthPct: number; colorClass: string }
type DistributionRow = {
  symbol: string
  value: number
  sharePct: number
  colorClass: string
  quantity: number
  pnlPercent: number
}

function formatHoldingQuantity(q: number, locale: string): string {
  if (!Number.isFinite(q)) return '—'
  if (Math.abs(q - Math.round(q)) < 1e-9) return String(Math.round(q))
  return new Intl.NumberFormat(locale, { maximumFractionDigits: 6 }).format(q)
}

type AllocationSortKey = 'symbol' | 'quantity' | 'weight' | 'value' | 'pnl'
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
    .map((it) => ({
      symbol: it.symbol,
      value: Math.max(0, parseApiDecimal(it.value, 0)),
      quantity: parseApiDecimal(it.quantity, 0),
      pnlPercent: parseApiDecimal(it.pnlPercent, 0),
    }))
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
    pnlPercent: r.pnlPercent,
    colorClass: DISTRIBUTION_BAR_COLORS[i % DISTRIBUTION_BAR_COLORS.length],
  }))

  const fullList: DistributionRow[] = rows.map((r, i) => ({
    symbol: r.symbol,
    value: r.value,
    sharePct: r.sharePct,
    quantity: r.quantity,
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

function buildInstrumentDonutRows(fullList: DistributionRow[]): AllocationDonutRow[] {
  return fullList.map((r) => ({
    rowKey: r.symbol,
    symbol: r.symbol,
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

const topGainers = [
  { symbol: 'AAPL', price: 120, change: 12.04 },
  { symbol: 'AIRBNB', price: 120, change: 12.04 },
  { symbol: 'NVDA', price: 120, change: 12.04 },
  { symbol: 'AMZN', price: 120, change: 12.04 },
  { symbol: 'SPTP', price: 120, change: 12.04 },
]

const marketInsights = [
  {
    titleKey: 'insights.items.tesla.title',
    detailKey: 'insights.items.tesla.detail',
    thumb: '🚗',
  },
  {
    titleKey: 'insights.items.apple.title',
    detailKey: 'insights.items.apple.detail',
    thumb: '📱',
  },
  {
    titleKey: 'insights.items.nvidia.title',
    detailKey: 'insights.items.nvidia.detail',
    thumb: '🧠',
  },
  {
    titleKey: 'insights.items.banking.title',
    detailKey: 'insights.items.banking.detail',
    thumb: '🏦',
  },
]

const sidebarMainKeys = ['dashboard', 'markets', 'portfolio', 'allocation'] as const
const sidebarSecondaryKeys = ['news', 'analysis', 'targets', 'watchlist', 'settings'] as const
type MarketOption = {
  instrumentId: number
  symbol: string
  name: string
  nativeQuote: string | null
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

export function MyPortfolioPage() {
  const { t, i18n } = useTranslation('portfolio')
  const [isDarkTheme, setIsDarkTheme] = useState(() =>
    typeof document !== 'undefined' && document.documentElement.getAttribute('data-theme') === 'dark',
  )
  const [portfolios, setPortfolios] = useState<Portfolio[]>([])
  const [selectedPortfolioId, setSelectedPortfolioId] = useState<number | null>(null)
  const [showCreatePortfolioModal, setShowCreatePortfolioModal] = useState(false)
  const [newPortfolioName, setNewPortfolioName] = useState('')
  const [newPortfolioBaseCurrency, setNewPortfolioBaseCurrency] = useState<'TRY' | 'USD'>('TRY')
  const [portfolioActionLoading, setPortfolioActionLoading] = useState(false)
  const [portfolioActionError, setPortfolioActionError] = useState<string | null>(null)
  const [portfolioSettingsError, setPortfolioSettingsError] = useState<string | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<{ id: number; name: string } | null>(null)
  const [deletePortfolioSubmitting, setDeletePortfolioSubmitting] = useState(false)
  const [amountsHiddenSaving, setAmountsHiddenSaving] = useState(false)
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [activeSection, setActiveSection] = useState<string>('dashboard')
  const [marketOptions, setMarketOptions] = useState<MarketOption[]>([])
  const [marketLoading, setMarketLoading] = useState(false)
  const [tradeSaving, setTradeSaving] = useState(false)
  const [tradeError, setTradeError] = useState<string | null>(null)
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
  const [tradeFlow, setTradeFlow] = useState<PortfolioTradeFlow | null>(null)
  const [tradeFlowHydrated, setTradeFlowHydrated] = useState(false)
  const [recentTxPreview, setRecentTxPreview] = useState<TransactionHistoryItem[]>([])
  const [recentTxLoading, setRecentTxLoading] = useState(false)
  const [allocationSortKey, setAllocationSortKey] = useState<AllocationSortKey>('value')
  const [allocationSortDir, setAllocationSortDir] = useState<AllocationSortDir>('desc')
  const [valueSnapshots, setValueSnapshots] = useState<PortfolioValueSnapshot[]>([])
  const [selectedInstrumentId, setSelectedInstrumentId] = useState<number | null>(null)
  const [instrumentQuery, setInstrumentQuery] = useState('')
  const [purchaseMode, setPurchaseMode] = useState<PurchaseMode>('NOW')
  const [inputMode, setInputMode] = useState<TradeInputMode>('LOTS')
  const [lots, setLots] = useState('1')
  const [amount, setAmount] = useState('')
  const [acquiredAt, setAcquiredAt] = useState('')
  const [unitPrice, setUnitPrice] = useState('')
  const [unitPriceUsed, setUnitPriceUsed] = useState<number | null>(null)
  const [manualUnitPriceRequired, setManualUnitPriceRequired] = useState(false)
  const [firstAvailableDate, setFirstAvailableDate] = useState<string | null>(null)
  const [lastEditedField, setLastEditedField] = useState<'lots' | 'amount'>('lots')
  const [isPreviewStep, setIsPreviewStep] = useState(false)
  const [instrumentPerformance, setInstrumentPerformance] = useState<InstrumentPerformance>({
    change1D: null,
    change1M: null,
    change3M: null,
    change6M: null,
    change1Y: null,
  })
  useDocumentTitle(t('titleDoc'))
  const { currency: displayCurrency } = useAppPreferences()

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

  const selectedPortfolio = useMemo(
    () =>
      selectedPortfolioId != null ? portfolios.find((p) => p.id === selectedPortfolioId) ?? null : null,
    [portfolios, selectedPortfolioId],
  )
  const hideMoney = selectedPortfolio?.amountsHidden === true

  /** Valuation and instrument catalog for the selected portfolio (fallback: app preference). */
  const valuationCurrency = useMemo((): 'TRY' | 'USD' => {
    const bc = selectedPortfolio?.baseCurrency?.trim().toUpperCase()
    if (bc === 'TRY') return 'TRY'
    if (bc === 'USD') return 'USD'
    return displayCurrency === 'TRY' ? 'TRY' : 'USD'
  }, [selectedPortfolio, displayCurrency])

  const displayDashboardCurrencyFormat = useMemo(
    () => (hideMoney ? maskedNumberFormatShim() : dashboardCurrencyFormat),
    [hideMoney, dashboardCurrencyFormat],
  )

  const displayDashboardDayChangeFormat = useMemo(
    () => (hideMoney ? maskedNumberFormatShim() : dashboardDayChangeFormat),
    [hideMoney, dashboardDayChangeFormat],
  )

  const formatTxMoney = useCallback(
    (row: TransactionHistoryItem) => {
      if (hideMoney) return MASKED_MONEY_LABEL
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

  const instrumentDonutRows = useMemo(() => buildInstrumentDonutRows(distribution.fullList), [distribution.fullList])

  const categoryDonutRows = useMemo(() => buildCategoryDonutRows(overview, t), [overview, t])

  /** Üstteki alım/satım özetleri: sabit son 1 ay penceresi (aralık butonları kaldırıldı). */
  const tradeFlowTotals = useMemo(() => tradeFlowPeriodTotals(tradeFlow?.points ?? [], '1m'), [tradeFlow])

  /** Satım − alım oranı (alım bazlı). Satım fazlaysa pozitif (+, yeşil); alım fazlaysa negatif (kırmızı). */
  const tradeFlowFark = useMemo(() => {
    const { buy, sell } = tradeFlowTotals
    if (!tradeFlowHydrated) return { kind: 'loading' as const }
    if (buy <= DOD_EPS && sell <= DOD_EPS) return { kind: 'empty' as const }
    const delta = sell - buy
    if (buy > DOD_EPS) {
      const pct = ((sell - buy) / buy) * 100
      return {
        kind: 'pct' as const,
        pct,
        tone: delta > DOD_EPS ? ('pos' as const) : delta < -DOD_EPS ? ('neg' as const) : ('neutral' as const),
      }
    }
    return { kind: 'infinity' as const, tone: 'pos' as const }
  }, [tradeFlowTotals, tradeFlowHydrated])

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
    setSelectedPortfolioId((prev) => (prev && rows.some((p) => p.id === prev) ? prev : rows[0].id))
  }, [])

  useEffect(() => {
    void refreshPortfolios()
  }, [refreshPortfolios])

  useEffect(() => {
    if (activeSection !== 'markets') {
      return
    }
    setMarketLoading(true)
    void fetchMarketOverview({
      page: 0,
      size: 200,
      category: 'all',
      sort: 'symbol,asc',
      displayCurrency: valuationCurrency,
    })
      .then((res) => {
        const options: MarketOption[] = res.content
          .filter((row) => row.instrumentId != null)
          .map((row) => ({
            instrumentId: row.instrumentId as number,
            symbol: row.symbol,
            name: row.name,
            nativeQuote: row.nativeQuote ?? null,
          }))
        setMarketOptions(options)
      })
      .finally(() => setMarketLoading(false))
  }, [activeSection, valuationCurrency])

  const selectedInstrument = useMemo(
    () => marketOptions.find((item) => item.instrumentId === selectedInstrumentId) ?? null,
    [marketOptions, selectedInstrumentId],
  )

  /** BIST / TRY-kotasyonlu FX (nativeQuote TRY) → ödeme TRY; ABD hisse, kripto, USD-FX → USD (backend fiyat ekseni). */
  const tradePaymentCurrency = useMemo((): 'TRY' | 'USD' => {
    return selectedInstrument?.nativeQuote === 'TRY' ? 'TRY' : 'USD'
  }, [selectedInstrument])

  /** Alım önizlemesi: birim fiyat ve tutar enstrüman kotasyonunda (BIST → TRY). */
  const tradeQuoteCurrencyFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency: tradePaymentCurrency,
        maximumFractionDigits: 2,
      }),
    [i18n.language, tradePaymentCurrency],
  )

  const loadHistoryPage = useCallback(
    async (page: number, filters: TransactionHistoryFilters) => {
      setHistoryLoading(true)
      setHistoryError(null)
      try {
        if (historyPagedEndpointAvailable) {
          const response = await getTransactionHistoryPage(page, historySize, filters, selectedPortfolioId)
          setHistory(response.content)
          setHistoryPage(response.page)
          setHistoryTotalPages(response.totalPages)
          setHistoryTotalElements(response.totalElements)
          return
        }
        throw new Error('paged-endpoint-disabled')
      } catch (error) {
        // Fallback for temporary backend 400 issues on paged endpoint.
        const allRows = await getTransactionHistory(selectedPortfolioId)
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
    [historySize, historyPagedEndpointAvailable, selectedPortfolioId],
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
    void loadHistoryPage(historyPage, appliedHistoryFilters)
  }, [activeSection, historyPage, appliedHistoryFilters, loadHistoryPage, selectedPortfolioId])

  useEffect(() => {
    if (activeSection !== 'dashboard' || selectedPortfolioId == null) {
      setRecentTxPreview([])
      return
    }
    let cancelled = false
    setRecentTxLoading(true)
    void (async () => {
      try {
        const response = await getTransactionHistoryPage(0, RECENT_TX_PREVIEW_SIZE, EMPTY_TX_FILTERS, selectedPortfolioId)
        if (!cancelled) {
          setRecentTxPreview(sortTransactionsNewestFirst(response.content).slice(0, RECENT_TX_PREVIEW_SIZE))
        }
      } catch {
        try {
          const allRows = await getTransactionHistory(selectedPortfolioId)
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
  }, [selectedPortfolioId])

  useEffect(() => {
    setAllocationSortKey('value')
    setAllocationSortDir('desc')
  }, [selectedPortfolioId])

  useEffect(() => {
    if (selectedPortfolioId == null) {
      setOverview(null)
      return
    }
    if (activeSection !== 'dashboard' && activeSection !== 'markets' && activeSection !== 'allocation') {
      return
    }
    void getMyPortfolioOverview(selectedPortfolioId, valuationCurrency).then((data) => setOverview(data)).catch(() => setOverview(null))
  }, [activeSection, selectedPortfolioId, valuationCurrency])

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
    void loadTradeFlowForPortfolio(selectedPortfolioId, valuationCurrency).then((flow) => {
      if (!cancelled) {
        setTradeFlow(flow)
        setTradeFlowHydrated(true)
      }
    })
    return () => {
      cancelled = true
    }
  }, [activeSection, selectedPortfolioId, valuationCurrency])

  useEffect(() => {
    if (selectedPortfolioId == null || activeSection !== 'dashboard') {
      setValueSnapshots([])
      return
    }
    let cancelled = false
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
  }, [activeSection, selectedPortfolioId])

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
        const row = page.content.find((item) => item.symbol === selected.symbol)
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
    if (activeSection !== 'markets' || selectedInstrumentId == null) return
    void getInstrumentPriceCoverage(selectedInstrumentId)
      .then((coverage) => {
        if (coverage.firstAvailableAt) {
          setFirstAvailableDate(coverage.firstAvailableAt.slice(0, 10))
        } else {
          setFirstAvailableDate(null)
        }
      })
      .catch(() => setFirstAvailableDate(null))
  }, [activeSection, selectedInstrumentId])

  useEffect(() => {
    if (activeSection !== 'markets' || selectedInstrumentId == null) {
      return
    }
    const lotsNumber = Number(lots)
    const amountNumber = Number(amount)
    const normalizedAcquiredAt = purchaseMode === 'PAST' ? toUtcStartOfDay(acquiredAt) : undefined
    const payload =
      inputMode === 'LOTS'
        ? {
            instrumentId: selectedInstrumentId,
            portfolioId: selectedPortfolioId ?? undefined,
            inputMode,
            lots: Number.isFinite(lotsNumber) && lotsNumber > 0 ? lotsNumber : undefined,
            inputCurrency: tradePaymentCurrency,
            purchaseMode,
            acquiredAt: normalizedAcquiredAt,
            unitPrice: purchaseMode === 'PAST' && unitPrice ? Number(unitPrice) : undefined,
          }
        : {
            instrumentId: selectedInstrumentId,
            portfolioId: selectedPortfolioId ?? undefined,
            inputMode,
            amount: Number.isFinite(amountNumber) && amountNumber > 0 ? amountNumber : undefined,
            inputCurrency: tradePaymentCurrency,
            purchaseMode,
            acquiredAt: normalizedAcquiredAt,
            unitPrice: purchaseMode === 'PAST' && unitPrice ? Number(unitPrice) : undefined,
          }
    const missingPrimaryInput =
      (inputMode === 'LOTS' && payload.lots == null) || (inputMode === 'AMOUNT' && payload.amount == null)

    if (purchaseMode === 'PAST' && normalizedAcquiredAt && missingPrimaryInput) {
      const timer = window.setTimeout(() => {
        void previewTrade({
          instrumentId: selectedInstrumentId,
          portfolioId: selectedPortfolioId ?? undefined,
          purchaseMode,
          inputMode: 'LOTS',
          lots: 1,
          inputCurrency: tradePaymentCurrency,
          acquiredAt: normalizedAcquiredAt,
          unitPrice: unitPrice ? Number(unitPrice) : undefined,
        })
          .then((preview) => {
            setUnitPriceUsed(preview.unitPriceUsed)
            if (!unitPrice) {
              setUnitPrice(String(preview.unitPriceUsed))
            }
            setManualUnitPriceRequired(preview.manualUnitPriceRequired)
          })
          .catch((error) => {
            const message = extractApiErrorMessage(error).toLowerCase()
            if (
              message.includes('historical price unavailable') ||
              message.includes('unitprice is required') ||
              message.includes('selected date')
            ) {
              setManualUnitPriceRequired(true)
              setUnitPriceUsed(null)
            }
          })
      }, 150)
      return () => window.clearTimeout(timer)
    }

    if (missingPrimaryInput) {
      return
    }
    const timer = window.setTimeout(() => {
      void previewTrade(payload)
        .then((preview) => {
          setUnitPriceUsed(preview.unitPriceUsed)
          if (purchaseMode === 'PAST' && !unitPrice) {
            setUnitPrice(String(preview.unitPriceUsed))
          }
          setManualUnitPriceRequired(preview.manualUnitPriceRequired)
          if (inputMode === 'LOTS' || lastEditedField === 'lots') {
            setAmount(String(preview.computedInputAmount))
          } else {
            setLots(String(preview.computedLots))
          }
        })
        .catch((error) => {
          const message = extractApiErrorMessage(error).toLowerCase()
          if (
            purchaseMode === 'PAST' &&
            (message.includes('historical price unavailable') ||
              message.includes('unitprice is required') ||
              message.includes('selected date') ||
              message.includes('date'))
          ) {
            setManualUnitPriceRequired(true)
            setUnitPriceUsed(null)
          }
        })
    }, 250)
    return () => window.clearTimeout(timer)
  }, [
    activeSection,
    selectedInstrumentId,
    selectedPortfolioId,
    inputMode,
    lots,
    amount,
    tradePaymentCurrency,
    purchaseMode,
    acquiredAt,
    unitPrice,
    lastEditedField,
  ])

  const filteredMarketOptions = useMemo(() => {
    const quoteTarget = valuationCurrency
    const byQuote = marketOptions.filter((item) => marketOptionListingQuote(item) === quoteTarget)
    const q = instrumentQuery.trim().toLowerCase()
    if (!q) return byQuote
    return byQuote.filter((item) => `${item.symbol} ${item.name}`.toLowerCase().includes(q))
  }, [instrumentQuery, marketOptions, valuationCurrency])

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

  const submitTrade = async () => {
    if (selectedInstrumentId == null || selectedPortfolioId == null) {
      return
    }
    setTradeError(null)
    setTradeSuccess(null)
    const normalizedAcquiredAt = purchaseMode === 'PAST' ? toUtcStartOfDay(acquiredAt) : undefined
    const payload =
      inputMode === 'LOTS'
        ? {
            instrumentId: selectedInstrumentId,
            portfolioId: selectedPortfolioId ?? undefined,
            inputMode,
            lots: Number(lots),
            inputCurrency: tradePaymentCurrency,
            purchaseMode,
            acquiredAt: normalizedAcquiredAt,
            unitPrice: purchaseMode === 'PAST' ? Number(unitPrice) : undefined,
          }
        : {
            instrumentId: selectedInstrumentId,
            portfolioId: selectedPortfolioId ?? undefined,
            inputMode,
            amount: Number(amount),
            inputCurrency: tradePaymentCurrency,
            purchaseMode,
            acquiredAt: normalizedAcquiredAt,
            unitPrice: purchaseMode === 'PAST' ? Number(unitPrice) : undefined,
          }
    if (purchaseMode === 'PAST' && !normalizedAcquiredAt) {
      setTradeError('Onceden alimda tarih zorunludur.')
      return
    }
    if (purchaseMode === 'PAST' && manualUnitPriceRequired && !unitPrice) {
      setTradeError('Onceden alimda tarih ve birim fiyat zorunludur.')
      return
    }
    setTradeSaving(true)
    try {
      await buyTrade(payload)
      setTradeSuccess('Islem basariyla kaydedildi.')
      setIsPreviewStep(false)
      await loadHistoryPage(0, appliedHistoryFilters)
      const updatedOverview = await getMyPortfolioOverview(selectedPortfolioId, valuationCurrency)
      setOverview(updatedOverview)
      setTradeFlow(await loadTradeFlowForPortfolio(selectedPortfolioId, valuationCurrency))
    } catch (error) {
      const message = extractApiErrorMessage(error)
      setTradeError(message || 'Islem kaydedilemedi.')
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
    setNewPortfolioBaseCurrency('TRY')
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
      const created = await createPortfolio({ name, baseCurrency: newPortfolioBaseCurrency })
      await refreshPortfolios()
      setSelectedPortfolioId(created.id)
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
    if (deleteConfirm && selectedPortfolioId != null && deleteConfirm.id !== selectedPortfolioId) {
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
        <aside className={`my-portfolio-sidebar card${sidebarOpen ? ' my-portfolio-sidebar-open' : ''}`}>
          <div className="my-portfolio-sidebar-top">
            <button
              type="button"
              className="my-portfolio-sidebar-toggle"
              onClick={() => setSidebarOpen((prev) => !prev)}
              aria-label={sidebarOpen ? t('sidebar.collapse') : t('sidebar.expand')}
            >
              <span className="my-portfolio-sidebar-toggle-icon" aria-hidden>
                {sidebarOpen ? '‹' : '›'}
              </span>
            </button>

            <label className="my-portfolio-select-wrap">
              {sidebarOpen ? (
                <>
                  <span>{t('sidebar.portfolios')}</span>
                  <select
                    aria-label={t('sidebar.portfolios')}
                    value={
                      selectedPortfolioId != null
                        ? String(selectedPortfolioId)
                        : portfolios.length === 0
                          ? CREATE_PORTFOLIO_SELECT_VALUE
                          : ''
                    }
                    onChange={(event) => {
                      const value = event.target.value
                      if (value === CREATE_PORTFOLIO_SELECT_VALUE) {
                        openCreatePortfolioModal()
                        return
                      }
                      setSelectedPortfolioId(Number(value))
                    }}
                  >
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
                onClick={() => setActiveSection(item)}
              >
                <span className="my-portfolio-sidebar-item-icon" aria-hidden>
                  <SidebarItemIcon item={item} />
                </span>
                {sidebarOpen ? <span>{t(`sidebar.items.${item}`)}</span> : null}
              </button>
            ))}
          </nav>

          <nav className="my-portfolio-sidebar-nav my-portfolio-sidebar-nav-secondary" aria-label={t('sidebar.quickAria')}>
            {sidebarSecondaryKeys.map((item) => (
              <button
                key={item}
                type="button"
                className={`my-portfolio-sidebar-item${activeSection === item ? ' my-portfolio-sidebar-item-active' : ''}`}
                onClick={() => setActiveSection(item)}
              >
                <span className="my-portfolio-sidebar-item-icon" aria-hidden>
                  <SidebarItemIcon item={item} />
                </span>
                {sidebarOpen ? <span>{t(`sidebar.items.${item}`)}</span> : null}
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
            {sidebarOpen ? <span>{t('sidebar.logout')}</span> : null}
          </button>
        </aside>

        <div className="my-portfolio-content">
          {activeSection === 'markets' ? (
            <article className={`card my-portfolio-trade-card${isDarkTheme ? ' is-dark' : ' is-light'}`}>
              <h2 className="my-portfolio-trade-title">Portfoye Yeni Enstruman Ekle</h2>
              <p className="my-portfolio-trade-subtitle">Enstruman sec, lot veya tutar gir, aninda maliyet/lot hesapla.</p>
              {selectedPortfolioId != null && selectedPortfolio ? (
                <p className="my-portfolio-trade-listing-hint" role="note">
                  {valuationCurrency === 'TRY' ? t('marketsAdd.listingHintTry') : t('marketsAdd.listingHintUsd')}
                </p>
              ) : null}

              <div className={`my-portfolio-trade-shell${isPreviewStep ? ' is-preview' : ''}`}>
                <div className="my-portfolio-trade-form-panel">
                  <ol className="my-portfolio-trade-steps">
                    <li className={!isPreviewStep ? 'is-active' : ''}>Alim Detaylari</li>
                    <li className={isPreviewStep ? 'is-active' : ''}>Onizleme</li>
                  </ol>

                  {!isPreviewStep ? (
                  <label className="my-portfolio-trade-field my-portfolio-trade-field-full">
                    <span>Enstruman Ara</span>
                    <input
                      value={instrumentQuery}
                      onChange={(event) => setInstrumentQuery(event.target.value)}
                      placeholder="Sirket, sembol veya ISIN ara..."
                      readOnly={isPreviewStep}
                    />
                  </label>
                  ) : null}

                  {!isPreviewStep ? (
                  <label className="my-portfolio-trade-field my-portfolio-trade-field-full">
                    <span>Enstruman</span>
                    <select
                      value={selectedInstrumentId ?? ''}
                      onChange={(event) => setSelectedInstrumentId(Number(event.target.value))}
                      disabled={isPreviewStep || marketLoading || filteredMarketOptions.length === 0}
                    >
                      {filteredMarketOptions.map((item) => (
                        <option key={item.instrumentId} value={item.instrumentId}>
                          {item.symbol} - {item.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  ) : null}

                  {!isPreviewStep ? (
                  <div className="my-portfolio-trade-toggle-grid">
                    <div>
                      <div className="my-portfolio-trade-toggle-group">
                        <button
                          type="button"
                          className={purchaseMode === 'NOW' ? 'is-active' : ''}
                          onClick={() => setPurchaseMode('NOW')}
                          disabled={isPreviewStep}
                        >
                          Piyasadan ekle
                        </button>
                        <button
                          type="button"
                          className={purchaseMode === 'PAST' ? 'is-active' : ''}
                          onClick={() => setPurchaseMode('PAST')}
                          disabled={isPreviewStep}
                        >
                          Gecmis alim ekle
                        </button>
                      </div>
                    </div>

                    <div>
                      <div className="my-portfolio-trade-toggle-group">
                        <button
                          type="button"
                          className={inputMode === 'LOTS' ? 'is-active' : ''}
                          onClick={() => setInputMode('LOTS')}
                          disabled={isPreviewStep}
                        >
                          Lot ile gir
                        </button>
                        <button
                          type="button"
                          className={inputMode === 'AMOUNT' ? 'is-active' : ''}
                          onClick={() => setInputMode('AMOUNT')}
                          disabled={isPreviewStep}
                        >
                          Tutar ile gir
                        </button>
                      </div>
                    </div>
                  </div>
                  ) : null}

                  {!isPreviewStep ? (
                  <div className="my-portfolio-trade-grid">
                    <label className="my-portfolio-trade-field">
                      <span>Lot Miktari</span>
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
                    </label>

                    <label className="my-portfolio-trade-field">
                      <span>Alim Fiyati</span>
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
                        placeholder={manualUnitPriceRequired ? 'Aldigin fiyati gir' : ''}
                      />
                      {purchaseMode === 'PAST' && manualUnitPriceRequired ? (
                        <small className="my-portfolio-trade-note">
                          O gunun verisi yok. Alim fiyatini manuel girin; toplulukta portfoyunde gozukmez notu ile islenir.
                          {firstAvailableDate ? ` Ilk mevcut tarih: ${firstAvailableDate}.` : ''}
                        </small>
                      ) : null}
                    </label>

                    <label className="my-portfolio-trade-field">
                      <span>Para Birimi</span>
                      <input
                        readOnly
                        value={tradePaymentCurrency === 'TRY' ? 'TRY (TL)' : 'USD'}
                        title="BIST ve TRY kotasyonlu varliklarda TL; digerlerinde USD — sistem fiyat eksenine uyar."
                      />
                    </label>

                    <label className="my-portfolio-trade-field my-portfolio-trade-field-full">
                      <span>Toplam Tutar</span>
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
                    </label>
                    {purchaseMode === 'PAST' ? (
                      <label className="my-portfolio-trade-field my-portfolio-trade-field-full">
                        <span>Alim Tarihi</span>
                        <input
                          type="date"
                          value={acquiredAt}
                          onChange={(event) => {
                            setAcquiredAt(event.target.value)
                            setUnitPrice('')
                            setManualUnitPriceRequired(false)
                            setUnitPriceUsed(null)
                          }}
                          disabled={isPreviewStep}
                        />
                      </label>
                    ) : null}
                  </div>
                  ) : null}

                  {isPreviewStep ? (
                    <div className="my-portfolio-confirmation-box">
                      <h5>Alim Bilgileri</h5>
                      <p><span>Enstruman</span><strong>{selectedInstrument?.symbol ?? '-'}</strong></p>
                      <p><span>Islem Tipi</span><strong>{purchaseMode === 'PAST' ? 'Gecmis alim ekle' : 'Piyasadan ekle'}</strong></p>
                      <p><span>Giris Sekli</span><strong>{inputMode === 'LOTS' ? 'Lot ile gir' : 'Tutar ile gir'}</strong></p>
                      <p><span>Lot</span><strong>{lots || '-'}</strong></p>
                      <p><span>Toplam Tutar</span><strong>{amount || '-'}</strong></p>
                      <p><span>Alim Fiyati</span><strong>{unitPriceUsed ?? unitPrice ?? '-'}</strong></p>
                      <p>
                        <span>Para Birimi</span>
                        <strong>{tradePaymentCurrency === 'TRY' ? 'TRY (TL)' : 'USD'}</strong>
                      </p>
                      {purchaseMode === 'PAST' ? <p><span>Alim Tarihi</span><strong>{acquiredAt || '-'}</strong></p> : null}
                    </div>
                  ) : null}

                  {tradeError ? <p className="auth-error">{tradeError}</p> : null}
                  {tradeSuccess ? <p className="my-portfolio-trade-success">{tradeSuccess}</p> : null}

                  <div className="my-portfolio-trade-submit-row">
                    {isPreviewStep ? (
                      <button
                        type="button"
                        className="auth-submit auth-submit-secondary my-portfolio-trade-back"
                        onClick={() => setIsPreviewStep(false)}
                        disabled={tradeSaving}
                      >
                        Geri
                      </button>
                    ) : null}
                    <button
                      type="button"
                      className="auth-submit my-portfolio-trade-submit"
                      onClick={() => (isPreviewStep ? void submitTrade() : setIsPreviewStep(true))}
                      disabled={tradeSaving}
                    >
                      {tradeSaving ? 'Kaydediliyor...' : isPreviewStep ? 'Onayla ve Ekle' : 'Devam Et'}
                    </button>
                    {purchaseMode === 'PAST' ? (
                      <span className="my-portfolio-trade-warning-inline">
                        Toplulukta gecmis alimlariniz portfoyunuzde gozukmez.
                      </span>
                    ) : null}
                  </div>
                </div>

                <aside className={`my-portfolio-trade-preview-panel${isPreviewStep ? ' is-expanded' : ''}`}>
                  <h4>Portfoy Dagilimi</h4>
                  <div className="my-portfolio-preview-ring" />
                  <div className="my-portfolio-preview-stats">
                    <div><span>Toplam Tutar</span><strong>{tradeQuoteCurrencyFormat.format(previewTotal || 0)}</strong></div>
                    <div><span>Maliyet / Lot</span><strong>{unitPriceUsed == null ? '—' : tradeQuoteCurrencyFormat.format(unitPriceUsed)}</strong></div>
                    <div><span>Toplam Lot</span><strong>{previewLots.toFixed(4)}</strong></div>
                    <div><span>Enstruman PB</span><strong>{selectedInstrument?.nativeQuote ?? '—'}</strong></div>
                  </div>
                  <ul className="my-portfolio-preview-distribution">
                    {projectedDistribution.map((item) => (
                      <li key={item.symbol}>
                        <span>{item.symbol}</span>
                        <strong>{item.weight.toFixed(2)}%</strong>
                      </li>
                    ))}
                  </ul>
                  {isPreviewStep ? (
                    <div className="my-portfolio-preview-performance">
                      <h5>Enstruman Degisimleri</h5>
                      <div>
                        <span>Bugun</span>
                        <strong className={Number(instrumentPerformance.change1D ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                          {instrumentPerformance.change1D == null ? '—' : `${percentFormat.format(instrumentPerformance.change1D)}%`}
                        </strong>
                      </div>
                      <div>
                        <span>1 Ay</span>
                        <strong className={Number(instrumentPerformance.change1M ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                          {instrumentPerformance.change1M == null ? '—' : `${percentFormat.format(instrumentPerformance.change1M)}%`}
                        </strong>
                      </div>
                      <div>
                        <span>3 Ay</span>
                        <strong className={Number(instrumentPerformance.change3M ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                          {instrumentPerformance.change3M == null ? '—' : `${percentFormat.format(instrumentPerformance.change3M)}%`}
                        </strong>
                      </div>
                      <div>
                        <span>6 Ay</span>
                        <strong className={Number(instrumentPerformance.change6M ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                          {instrumentPerformance.change6M == null ? '—' : `${percentFormat.format(instrumentPerformance.change6M)}%`}
                        </strong>
                      </div>
                      <div>
                        <span>1 Yil</span>
                        <strong className={Number(instrumentPerformance.change1Y ?? 0) >= 0 ? 'is-up' : 'is-down'}>
                          {instrumentPerformance.change1Y == null ? '—' : `${percentFormat.format(instrumentPerformance.change1Y)}%`}
                        </strong>
                      </div>
                    </div>
                  ) : null}
                  <p className="my-portfolio-preview-note">Islem anlik fiyat/kur uzerinden hesaplanir.</p>
                </aside>
              </div>

            </article>
          ) : null}

          {activeSection === 'portfolio' ? (
            <article className={`card my-portfolio-trade-card${isDarkTheme ? ' is-dark' : ' is-light'}`}>
              <div className="my-portfolio-history-head">
                <h4>Islem Gecmisi</h4>
                <p>{historyTotalElements} kayit</p>
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
                        <th>Enstruman</th>
                        <th>Islem</th>
                        <th>Alim Tipi</th>
                        <th>Lot</th>
                        <th>Maliyet</th>
                        <th>Liste PB</th>
                        <th>Tarih</th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.map((row) => (
                        <tr key={row.transactionId}>
                          <td>{row.instrumentSymbol}</td>
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
                          <td>{new Date(row.acquiredAt ?? row.createdAt).toLocaleString(i18n.language)}</td>
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
              {selectedPortfolioId == null ? (
                <p className="my-portfolio-trade-note">{t('allocation.empty')}</p>
              ) : distribution.fullList.length === 0 ? (
                <p className="my-portfolio-trade-note">{t('allocation.empty')}</p>
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
                  <div className="my-portfolio-allocation-table-wrap">
                    <table className="my-portfolio-allocation-table">
                      <colgroup>
                        <col className="my-portfolio-alloc-col-sym" />
                        <col className="my-portfolio-alloc-col-qty" />
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
                        </tr>
                      </thead>
                      <tbody>
                        {allocationTableRows.map((row) => (
                          <tr key={row.symbol}>
                            <td>
                              <span className="my-portfolio-allocation-symbol">
                                <span className={`my-portfolio-dot ${row.colorClass}`} />
                                {row.symbol}
                              </span>
                            </td>
                            <td className="my-portfolio-allocation-num my-portfolio-allocation-muted">
                              {formatHoldingQuantity(row.quantity, i18n.language)}
                            </td>
                            <td className="my-portfolio-allocation-num">{sharePctDisplay.format(row.sharePct)}%</td>
                            <td className="my-portfolio-allocation-num">{displayDashboardCurrencyFormat.format(row.value)}</td>
                            <td
                              className={`my-portfolio-allocation-num${row.pnlPercent > 1e-9 ? ' my-portfolio-up' : row.pnlPercent < -1e-9 ? ' my-portfolio-down' : ''}`}
                            >
                              {dashboardPctFormat.format(row.pnlPercent)}%
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </article>
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
              {selectedPortfolioId == null || !selectedPortfolio ? (
                <p className="my-portfolio-trade-subtitle">{t('settingsPage.pickPortfolio')}</p>
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
                        <dd>{selectedPortfolio.baseCurrency}</dd>
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

          {activeSection !== 'watchlist' &&
          activeSection !== 'markets' &&
          activeSection !== 'portfolio' &&
          activeSection !== 'allocation' &&
          activeSection !== 'settings' ? (
            <>
              <div className="my-portfolio-gainers">
                <span>{t('topGainers')}</span>
                <ul>
                  {topGainers.map((item) => (
                    <li key={item.symbol}>
                      <strong>{item.symbol}</strong>
                      <span>{currencyFormat.format(item.price)}</span>
                      <small>{percentFormat.format(item.change)}</small>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="my-portfolio-grid">
            <article className="card my-portfolio-card">
              <div className="my-portfolio-card-head">
                <h3>{t('valueTitle')}</h3>
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
              {selectedPortfolioId == null ? null : (
                <>
                  <PortfolioValueHistoryChart
                    snapshots={valueSnapshots}
                    liveTotalValue={overview ? parseApiDecimal(overview.totalValue, 0) : null}
                    height={200}
                    isDark={isDarkTheme}
                    emptyLabel={t('valueChart.empty')}
                    locale={i18n.language}
                    maskAmounts={hideMoney}
                  />
                </>
              )}
            </article>

            <article className="card my-portfolio-card" aria-labelledby="portfolio-change-heading">
              <div className="my-portfolio-card-head">
                <h3 id="portfolio-change-heading">{t('changeTitle')}</h3>
              </div>
              <div
                className={`my-portfolio-trade-flow-net${
                  selectedPortfolioId != null &&
                  tradeFlowHydrated &&
                  tradeFlowTotals.net > DOD_EPS
                    ? ' my-portfolio-trade-flow-net--buy-heavy'
                    : selectedPortfolioId != null &&
                        tradeFlowHydrated &&
                        tradeFlowTotals.net < -DOD_EPS
                      ? ' my-portfolio-trade-flow-net--sell-heavy'
                      : selectedPortfolioId != null && tradeFlowHydrated
                        ? ' my-portfolio-trade-flow-net--balanced'
                        : ''
                }`}
              >
                {selectedPortfolioId == null
                  ? '—'
                  : !tradeFlowHydrated
                    ? t('tradeFlow.loading')
                    : hideMoney
                      ? '•••'
                      : displayDashboardCurrencyFormat.format(tradeFlowTotals.net)}
              </div>
              <div className="my-portfolio-trade-flow-totals">
                <div>
                  <span className="trade-flow-stat-label">{t('tradeFlow.buyLabel')}</span>
                  <strong>
                    {selectedPortfolioId == null
                      ? '—'
                      : !tradeFlowHydrated
                        ? t('tradeFlow.loading')
                        : displayDashboardCurrencyFormat.format(tradeFlowTotals.buy)}
                  </strong>
                </div>
                <div>
                  <span className="trade-flow-stat-label">{t('tradeFlow.diffLabel')}</span>
                  <strong
                    className={`trade-flow-fark-val${
                      tradeFlowFark.kind === 'pct' || tradeFlowFark.kind === 'infinity'
                        ? tradeFlowFark.tone === 'pos'
                          ? ' my-portfolio-dod-pos'
                          : tradeFlowFark.tone === 'neg'
                            ? ' my-portfolio-dod-neg'
                            : ' my-portfolio-dod-neutral'
                        : ' my-portfolio-dod-neutral'
                    }`}
                  >
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
                        : displayDashboardCurrencyFormat.format(tradeFlowTotals.sell)}
                  </strong>
                </div>
              </div>
              <p className="my-portfolio-sub-value my-portfolio-trade-flow-net-caption">{t('tradeFlow.netLabel')}</p>
              {selectedPortfolioId == null ? null : !tradeFlowHydrated ? (
                <div className="my-portfolio-value-chart-empty" style={{ minHeight: 200 }}>
                  {t('tradeFlow.loading')}
                </div>
              ) : (
                <>
                  <TradeFlowHistoryChart
                    points={tradeFlow?.points ?? []}
                    height={200}
                    isDark={isDarkTheme}
                    emptyLabel={t('tradeFlow.empty')}
                    locale={i18n.language}
                    maskAmounts={hideMoney}
                  />
                </>
              )}
            </article>

            <article className="card my-portfolio-card">
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

            <article className="card my-portfolio-card" aria-labelledby="portfolio-distribution-heading">
              <div className="my-portfolio-card-head">
                <h3 id="portfolio-distribution-heading">{t('distributionTitle')}</h3>
                <button
                  type="button"
                  disabled={selectedPortfolioId == null || distribution.fullList.length === 0}
                  onClick={() => setActiveSection('allocation')}
                >
                  {t('actions.viewAll')}
                </button>
              </div>
              {selectedPortfolioId == null || distribution.bar.length === 0 ? (
                <p className="my-portfolio-distribution-empty">{t('allocation.empty')}</p>
              ) : (
                <>
                  <div
                    className="my-portfolio-distribution-bar"
                    role="img"
                    aria-label={t('distributionTitle')}
                  >
                    {distribution.bar.map((seg) => (
                      <span
                        key={seg.key}
                        className={seg.colorClass}
                        style={{
                          width: `${seg.widthPct}%`,
                          minWidth: seg.widthPct > 0 ? '4px' : 0,
                        }}
                        title={seg.key === '__other__' ? t('allocation.other') : seg.key}
                      />
                    ))}
                  </div>
                  <ul className="my-portfolio-asset-list">
                    {distribution.topList.map((row) => (
                      <li key={row.symbol}>
                        <div>
                          <span className={`my-portfolio-dot ${row.colorClass}`} />
                          <strong>{row.symbol}</strong>
                          <small>{sharePctDisplay.format(row.sharePct)}%</small>
                        </div>
                        <span>{displayDashboardCurrencyFormat.format(row.value)}</span>
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </article>

            <article
              className="card my-portfolio-card my-portfolio-card-tx-preview"
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
              {recentTxLoading ? (
                <div className="markets-skeleton-row" aria-hidden />
              ) : (
                <ul className="my-portfolio-recent-tx">
                  {recentTxPreview.length === 0 ? (
                    <li className="my-portfolio-recent-tx-empty">{t('transactionHistoryEmpty')}</li>
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
                        <li key={row.transactionId} className="my-portfolio-recent-tx-row">
                          <div className="my-portfolio-recent-tx-left">
                            <span className="my-portfolio-recent-tx-symbol">{row.instrumentSymbol}</span>
                            <span className="my-portfolio-recent-tx-qty">
                              <span className="my-portfolio-recent-tx-qty-label">×</span>
                              {qtyStr}
                            </span>
                          </div>
                          <div className="my-portfolio-recent-tx-right">
                            <span className="my-portfolio-recent-tx-amt">{formatTxMoney(row)}</span>
                            <span
                              className={`my-portfolio-recent-tx-badge${isBuy ? ' is-buy' : ' is-sell'}`}
                            >
                              <svg className="my-portfolio-recent-tx-badge-ico" viewBox="0 0 12 12" aria-hidden>
                                {isBuy ? (
                                  <path fill="currentColor" d="M6 2.2 10.2 8.8H1.8z" />
                                ) : (
                                  <path fill="currentColor" d="M6 9.8 1.8 3.2h8.4z" />
                                )}
                              </svg>
                              {isBuy ? t('txTypeBuy') : t('txTypeSell')}
                            </span>
                          </div>
                        </li>
                      )
                    })
                  )}
                </ul>
              )}
            </article>

            <article className="card my-portfolio-card">
              <div className="my-portfolio-card-head">
                <h3>{t('insightTitle')}</h3>
                <button type="button">{t('actions.viewAll')}</button>
              </div>
              <ul className="my-portfolio-insight-list">
                {marketInsights.map((item) => (
                  <li key={item.titleKey}>
                    <span>{item.thumb}</span>
                    <div>
                      <strong>{t(item.titleKey)}</strong>
                      <small>{t(item.detailKey)}</small>
                    </div>
                  </li>
                ))}
              </ul>
            </article>
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
            <div className="my-portfolio-modal-field">
              <span className="my-portfolio-modal-field-label">{t('createPortfolioModal.currencyLabel')}</span>
              <div className="my-portfolio-currency-grid" role="group" aria-label={t('createPortfolioModal.currencyLabel')}>
                <button
                  type="button"
                  className={`my-portfolio-currency-chip${newPortfolioBaseCurrency === 'TRY' ? ' is-active' : ''}`}
                  onClick={() => setNewPortfolioBaseCurrency('TRY')}
                  disabled={portfolioActionLoading}
                >
                  {t('createPortfolioModal.tryChip')}
                </button>
                <button
                  type="button"
                  className={`my-portfolio-currency-chip${newPortfolioBaseCurrency === 'USD' ? ' is-active' : ''}`}
                  onClick={() => setNewPortfolioBaseCurrency('USD')}
                  disabled={portfolioActionLoading}
                >
                  {t('createPortfolioModal.usdChip')}
                </button>
              </div>
            </div>
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
