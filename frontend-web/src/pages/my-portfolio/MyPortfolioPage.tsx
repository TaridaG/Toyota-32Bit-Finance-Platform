import { Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { fetchMarketOverview } from '../../features/markets/api/marketService'
import { buyTrade, createPortfolio, getInstrumentPriceCoverage, getMyPortfolioOverview, getPortfolios, getTransactionHistory, getTransactionHistoryPage, previewTrade } from '../../features/portfolio/api/portfolioApi'
import type { Portfolio, PortfolioOverview, PurchaseMode, TradeInputMode, TransactionHistoryFilters, TransactionHistoryItem } from '../../shared/types/portfolio'

const MyPortfolioWatchlistSection = lazy(async () => {
  const mod = await import('./components/MyPortfolioWatchlistSection')
  return { default: mod.MyPortfolioWatchlistSection }
})

type PortfolioAsset = {
  symbol: string
  sharePercent: number
  value: number
  changePercent: number
  colorClass: string
}

const topGainers = [
  { symbol: 'AAPL', price: 120, change: 12.04 },
  { symbol: 'AIRBNB', price: 120, change: 12.04 },
  { symbol: 'NVDA', price: 120, change: 12.04 },
  { symbol: 'AMZN', price: 120, change: 12.04 },
  { symbol: 'SPTP', price: 120, change: 12.04 },
]

const assets: PortfolioAsset[] = [
  { symbol: 'AAPL', sharePercent: 40, value: 7518, changePercent: 12.04, colorClass: 'my-portfolio-dot-blue' },
  { symbol: 'AIRBNB', sharePercent: 29, value: 5102, changePercent: 1.8, colorClass: 'my-portfolio-dot-purple' },
  { symbol: 'NVDA', sharePercent: 17, value: 3916, changePercent: -2.3, colorClass: 'my-portfolio-dot-pink' },
  { symbol: 'AMZN', sharePercent: 14, value: 2518, changePercent: 7.01, colorClass: 'my-portfolio-dot-green' },
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

const sidebarMainKeys = ['dashboard', 'markets', 'portfolio'] as const
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

function MiniLineChart() {
  const points = '0,96 20,70 40,78 60,54 80,46 100,58 120,44 140,36 160,52 180,50 200,34 220,28 240,22 260,26 280,48 300,44'
  return (
    <svg viewBox="0 0 300 110" className="my-portfolio-line-chart" aria-hidden>
      <defs>
        <linearGradient id="portfolioArea" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="rgba(99, 102, 241, 0.28)" />
          <stop offset="100%" stopColor="rgba(99, 102, 241, 0.02)" />
        </linearGradient>
      </defs>
      <path d="M0,110 L0,96 L20,70 L40,78 L60,54 L80,46 L100,58 L120,44 L140,36 L160,52 L180,50 L200,34 L220,28 L240,22 L260,26 L280,48 L300,44 L300,110 Z" />
      <polyline points={points} />
      <circle cx="200" cy="34" r="4" />
    </svg>
  )
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
  const [portfolioActionLoading, setPortfolioActionLoading] = useState(false)
  const [portfolioActionError, setPortfolioActionError] = useState<string | null>(null)
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
  const [selectedInstrumentId, setSelectedInstrumentId] = useState<number | null>(null)
  const [instrumentQuery, setInstrumentQuery] = useState('')
  const [purchaseMode, setPurchaseMode] = useState<PurchaseMode>('NOW')
  const [inputMode, setInputMode] = useState<TradeInputMode>('LOTS')
  const [inputCurrency, setInputCurrency] = useState<'TRY' | 'USD' | 'EUR'>('USD')
  const [lots, setLots] = useState('1')
  const [amount, setAmount] = useState('')
  const [acquiredAt, setAcquiredAt] = useState('')
  const [unitPrice, setUnitPrice] = useState('')
  const [unitPriceUsed, setUnitPriceUsed] = useState<number | null>(null)
  const [fxRateUsed, setFxRateUsed] = useState<number | null>(null)
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

  const currencyFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 2,
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
      displayCurrency: 'USD',
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
        if (!selectedInstrumentId && options.length > 0) {
          setSelectedInstrumentId(options[0].instrumentId)
        }
      })
      .finally(() => setMarketLoading(false))
  }, [activeSection, selectedInstrumentId])

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
    setHistoryPage(0)
  }, [selectedPortfolioId])

  useEffect(() => {
    if (activeSection !== 'markets') return
    void getMyPortfolioOverview(selectedPortfolioId).then((data) => setOverview(data)).catch(() => setOverview(null))
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
      displayCurrency: 'USD',
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
  }, [activeSection, selectedInstrumentId, marketOptions])

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
            inputCurrency,
            purchaseMode,
            acquiredAt: normalizedAcquiredAt,
            unitPrice: purchaseMode === 'PAST' && unitPrice ? Number(unitPrice) : undefined,
          }
        : {
            instrumentId: selectedInstrumentId,
            portfolioId: selectedPortfolioId ?? undefined,
            inputMode,
            amount: Number.isFinite(amountNumber) && amountNumber > 0 ? amountNumber : undefined,
            inputCurrency,
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
          inputCurrency,
          acquiredAt: normalizedAcquiredAt,
          unitPrice: unitPrice ? Number(unitPrice) : undefined,
        })
          .then((preview) => {
            setUnitPriceUsed(preview.unitPriceUsed)
            setFxRateUsed(preview.fxRateUsed)
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
          setFxRateUsed(preview.fxRateUsed)
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
  }, [activeSection, selectedInstrumentId, selectedPortfolioId, inputMode, lots, amount, inputCurrency, purchaseMode, acquiredAt, unitPrice, lastEditedField])

  const selectedInstrument = useMemo(
    () => marketOptions.find((item) => item.instrumentId === selectedInstrumentId) ?? null,
    [marketOptions, selectedInstrumentId],
  )
  const filteredMarketOptions = useMemo(() => {
    const q = instrumentQuery.trim().toLowerCase()
    if (!q) return marketOptions
    return marketOptions.filter((item) => `${item.symbol} ${item.name}`.toLowerCase().includes(q))
  }, [instrumentQuery, marketOptions])
  const parsedAmount = Number(amount)
  const parsedLots = Number(lots)
  const previewTotal = Number.isFinite(parsedAmount) && parsedAmount > 0 ? parsedAmount : 0
  const previewLots = Number.isFinite(parsedLots) && parsedLots > 0 ? parsedLots : 0
  const projectedDistribution = useMemo(() => {
    const baseItems = overview?.items ?? []
    const baseTotal = Number(overview?.totalValue ?? 0)
    const selectedSymbol = selectedInstrument?.symbol ?? ''
    const pendingValue =
      inputCurrency === (overview?.currency ?? 'USD')
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
  }, [overview, selectedInstrument, previewTotal, previewLots, inputCurrency, unitPriceUsed])

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
            inputCurrency,
            purchaseMode,
            acquiredAt: normalizedAcquiredAt,
            unitPrice: purchaseMode === 'PAST' ? Number(unitPrice) : undefined,
          }
        : {
            instrumentId: selectedInstrumentId,
            portfolioId: selectedPortfolioId ?? undefined,
            inputMode,
            amount: Number(amount),
            inputCurrency,
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
      const updatedOverview = await getMyPortfolioOverview(selectedPortfolioId)
      setOverview(updatedOverview)
    } catch (error) {
      const message = extractApiErrorMessage(error)
      setTradeError(message || 'Islem kaydedilemedi.')
    } finally {
      setTradeSaving(false)
    }
  }

  const openCreatePortfolioModal = () => {
    if (portfolios.length >= 5) {
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
    if (portfolios.length >= 5) {
      setPortfolioActionError('Maksimum 5 portfoy olusturabilirsiniz.')
      return
    }
    setPortfolioActionLoading(true)
    setPortfolioActionError(null)
    try {
      const created = await createPortfolio({ name })
      await refreshPortfolios()
      setSelectedPortfolioId(created.id)
      setShowCreatePortfolioModal(false)
    } catch (error) {
      setPortfolioActionError(extractApiErrorMessage(error) || 'Portfoy olusturulamadi.')
    } finally {
      setPortfolioActionLoading(false)
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
                    value={selectedPortfolioId ?? ''}
                    onChange={(event) => setSelectedPortfolioId(Number(event.target.value))}
                    disabled={portfolios.length === 0}
                  >
                    {portfolios.map((portfolio) => (
                      <option key={portfolio.id} value={portfolio.id}>
                        {portfolio.name}
                      </option>
                    ))}
                  </select>
                  <button type="button" className="auth-submit auth-submit-secondary" onClick={openCreatePortfolioModal}>
                    Yeni Portfoy Ekle
                  </button>
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
                      <select value={inputCurrency} onChange={(event) => setInputCurrency(event.target.value as 'TRY' | 'USD' | 'EUR')} disabled={isPreviewStep}>
                        <option value="TRY">TRY</option>
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                      </select>
                    </label>

                    <label className="my-portfolio-trade-field">
                      <span>Kur</span>
                      <input value={fxRateUsed == null ? '' : fxRateUsed.toFixed(6)} readOnly />
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
                      <p><span>Para Birimi</span><strong>{inputCurrency}</strong></p>
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
                    <div><span>Toplam Tutar</span><strong>{currencyFormat.format(previewTotal || 0)}</strong></div>
                    <div><span>Maliyet / Lot</span><strong>{unitPriceUsed == null ? '—' : currencyFormat.format(unitPriceUsed)}</strong></div>
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
                  <option value="">PB (Tum)</option>
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
                        <th>PB</th>
                        <th>Kur</th>
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
                          <td>{row.inputAmount ?? row.totalAmount}</td>
                          <td>{row.inputCurrency ?? 'USD'}</td>
                          <td>{row.fxRateUsed ?? 1}</td>
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

          {activeSection === 'watchlist' ? (
            <Suspense fallback={<div className="markets-skeleton-row" />}>
              <MyPortfolioWatchlistSection currencyFormat={currencyFormat} percentFormat={percentFormat} />
            </Suspense>
          ) : null}

          {activeSection !== 'watchlist' && activeSection !== 'markets' && activeSection !== 'portfolio' ? (
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
            <article className="card my-portfolio-card my-portfolio-card-wide">
              <div className="my-portfolio-card-head">
                <h3>{t('valueTitle')}</h3>
                <button type="button">{t('actions.yearly')}</button>
              </div>
              <p className="my-portfolio-main-value">{currencyFormat.format(134815)}</p>
              <p className="my-portfolio-sub-value">
                + {currencyFormat.format(19698)} {t('fromLastYear')}
              </p>
              <MiniLineChart />
            </article>

            <article className="card my-portfolio-card">
              <div className="my-portfolio-card-head">
                <h3>{t('profitTitle')}</h3>
                <button type="button">{t('actions.yearly')}</button>
              </div>
              <div className="my-portfolio-donut-wrap">
                <div className="my-portfolio-donut">
                  <div>
                    <strong>{currencyFormat.format(8436)}</strong>
                    <span>-{currencyFormat.format(268.2)}</span>
                  </div>
                </div>
              </div>
              <ul className="my-portfolio-legend">
                <li>{t('legend.stocks')}</li>
                <li>{t('legend.funds')}</li>
                <li>{t('legend.bonds')}</li>
                <li>{t('legend.reits')}</li>
              </ul>
            </article>

            <article className="card my-portfolio-card">
              <div className="my-portfolio-card-head">
                <h3>{t('distributionTitle')}</h3>
                <button type="button">{t('actions.viewAll')}</button>
              </div>
              <div className="my-portfolio-distribution-bar">
                {assets.map((asset) => (
                  <span key={asset.symbol} className={asset.colorClass} style={{ width: `${asset.sharePercent}%` }} />
                ))}
              </div>
              <ul className="my-portfolio-asset-list">
                {assets.map((asset) => (
                  <li key={asset.symbol}>
                    <div>
                      <span className={`my-portfolio-dot ${asset.colorClass}`} />
                      <strong>{asset.symbol}</strong>
                      <small>{asset.sharePercent}%</small>
                    </div>
                    <span>{currencyFormat.format(asset.value)}</span>
                  </li>
                ))}
              </ul>
            </article>

            <article className="card my-portfolio-card">
              <div className="my-portfolio-card-head">
                <h3>{t('assetsTitle')}</h3>
                <button type="button">{t('actions.viewAll')}</button>
              </div>
              <ul className="my-portfolio-my-assets">
                {assets.map((asset) => (
                  <li key={asset.symbol}>
                    <strong>{asset.symbol}</strong>
                    <span>{currencyFormat.format(asset.value)}</span>
                    <small className={asset.changePercent >= 0 ? 'my-portfolio-up' : 'my-portfolio-down'}>
                      {percentFormat.format(asset.changePercent)}
                    </small>
                  </li>
                ))}
              </ul>
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
      {showCreatePortfolioModal ? (
        <div className="my-portfolio-modal-overlay" role="dialog" aria-modal="true">
          <div className="my-portfolio-modal card">
            <h4>Ilk Portfoyunuzu Olusturun</h4>
            <p>Portfoy islemleri icin bir portfoy adi girmeniz gerekiyor (maksimum 5 adet).</p>
            <input
              value={newPortfolioName}
              onChange={(event) => setNewPortfolioName(event.target.value)}
              placeholder="Ornek: Core Portfolio"
              maxLength={120}
            />
            {portfolioActionError ? <p className="auth-error">{portfolioActionError}</p> : null}
            <div className="my-portfolio-modal-actions">
              {portfolios.length > 0 ? (
                <button
                  type="button"
                  className="auth-submit auth-submit-secondary"
                  onClick={() => setShowCreatePortfolioModal(false)}
                  disabled={portfolioActionLoading}
                >
                  Vazgec
                </button>
              ) : null}
              <button type="button" className="auth-submit" onClick={() => void handleCreatePortfolio()} disabled={portfolioActionLoading}>
                {portfolioActionLoading ? 'Olusturuluyor...' : 'Portfoy Olustur'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
