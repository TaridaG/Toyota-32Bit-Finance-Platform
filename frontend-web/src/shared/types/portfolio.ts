export type Portfolio = {
  id: number
  name: string
  /**
   * `MIXED` = unified portfolio account (not locked to one quote currency).
   * Older rows may still be `TRY` or `USD`.
   */
  baseCurrency: string
  /** ISO local date-time from API when present */
  createdAt?: string | null
  /** When true, dashboard/allocation amounts for this portfolio are masked in the UI. */
  amountsHidden?: boolean
}

export type PositionSummary = {
  instrumentId: number
  symbol: string
  quantity: number
  avgCost: number
  currentPrice: number
  marketValue: number
  pnl: number
  pnlPercentage: number
}

export type PortfolioSummary = {
  totalCost: number
  totalMarketValue: number
  totalPnL: number
  totalPnLPercentage: number
  positions: PositionSummary[]
}

export type PortfolioAllocation = {
  symbol: string
  marketValue: number
  percentage: number
}

export type CreatePortfolioPayload = {
  name: string
  /** Omit for a unified account (`MIXED` on the server). `TRY` / `USD` remain supported. */
  baseCurrency?: string
}

export type ApiResponse<T> = {
  success: boolean
  data: T
}

export type TradeInputMode = 'LOTS' | 'AMOUNT'
export type PurchaseMode = 'NOW' | 'PAST'

/** Backend {@code CurrencyConversionService} TRY-hub seti ile uyumlu odeme para birimleri. */
export type TradePaymentCurrency = 'TRY' | 'USD' | 'EUR' | 'GBP' | 'JPY' | 'AED'

export type TradePreviewPayload = {
  portfolioId?: number
  instrumentId: number
  inputMode: TradeInputMode
  lots?: number
  amount?: number
  /** Ödeme para birimi (portfoy kotasyonlari + gecmis islemlerden onerilen liste). */
  inputCurrency: TradePaymentCurrency
  purchaseMode: PurchaseMode
  acquiredAt?: string
  unitPrice?: number
}

export type AcquisitionFxRatesSnapshot = {
  fxAsOfIso: string
  usdTry?: number | null
  eurTry?: number | null
  gbpTry?: number | null
  jpyTry?: number | null
  aedTry?: number | null
  eurUsd?: number | null
  gbpUsd?: number | null
  jpyUsd?: number | null
}

export type TradePreview = {
  instrumentId: number
  instrumentSymbol: string
  instrumentQuoteCurrency: string
  computedLots: number
  computedInputAmount: number
  inputCurrency: string
  unitPriceUsed: number
  fxRateUsed: number
  manualUnitPriceRequired: boolean
  unitPriceSource: string
  /** ISO-8601; set for PAST to the acquisition instant used (may differ after earliest-data rollback). */
  effectiveAcquiredAt?: string | null
  pastDateRolledToEarliestData?: boolean
  /** TRY hub (+ USD legs) at conversion time: MDS snapshot for PAST, live for NOW. */
  acquisitionFxRates?: AcquisitionFxRatesSnapshot | null
}

export type TradeExecution = {
  transactionId: number
  instrumentId: number
  instrumentSymbol: string
  type: string
  purchaseMode: PurchaseMode
  quantity: number
  price: number
  totalAmount: number
  inputCurrency: string | null
  inputAmount: number | null
  fxRateUsed: number | null
  acquiredAt: string | null
  createdAt: string
}

export type TransactionHistoryItem = {
  transactionId: number
  portfolioId: number | null
  portfolioName: string | null
  instrumentSymbol: string
  type: string
  purchaseMode: string | null
  sourceLabel: string | null
  quantity: number
  price: number
  totalAmount: number
  /** Listing-currency notional (unit price × qty); same as DB `totalAmount`. */
  quoteCurrency?: string | null
  /** Payment / wallet currency amount at execution (DB `input_amount`); use for "Maliyet" when set. */
  inputCurrency: string | null
  inputAmount: number | null
  fxRateUsed: number | null
  acquiredAt: string | null
  createdAt: string
}

export type TransactionHistoryPage = {
  content: TransactionHistoryItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type TransactionHistoryFilters = {
  symbol?: string
  type?: 'BUY' | 'SELL' | ''
  purchaseMode?: PurchaseMode | ''
  inputCurrency?: 'TRY' | 'USD' | 'EUR' | ''
  fromDate?: string
  toDate?: string
}

export type PortfolioOverviewItem = {
  instrumentId: number
  symbol: string
  name: string
  type: string
  /** Exchange enum from API (e.g. BIST, BINANCE); optional when unknown. */
  exchange?: string | null
  quantity: number
  avgBuyPrice: number
  currentPrice: number
  value: number
  /** Prior-day mark (UTC cut-off) in overview currency; null when unavailable. */
  priorDayValue?: number | null
  pnl: number
  pnlPercent: number
}

export type PortfolioValueSnapshot = {
  id: number
  userId: string
  totalCost: number
  totalValue: number
  unrealizedPnl: number
  createdAt: string
  externalPortfolioId?: number | null
}

/** Signed cash flow per trade in overview currency (X-Currency): buy positive, sell negative. */
export type PortfolioTradeFlowPoint = {
  transactionId: number
  createdAt: string
  signedAmount: number
}

export type PortfolioTradeFlow = {
  currency: string
  points: PortfolioTradeFlowPoint[]
}

export type PortfolioPerformancePoint = {
  day: string
  marketValue: number
  netFlow: number
  dailyReturnPct?: number | null
  twrPct: number
}

export type PortfolioPerformanceSeries = {
  currency: string
  inceptionDay?: string | null
  points: PortfolioPerformancePoint[]
}

export type PortfolioOverview = {
  currency: string
  totalValue: number
  totalCost: number
  totalPnl: number
  totalPnlPercent: number
  /** Same positions marked with last price before start of today (UTC) vs current. */
  dayOverDayChange?: number | null
  items: PortfolioOverviewItem[]
}

export type InstrumentPriceCoverage = {
  instrumentId: number
  symbol: string
  firstAvailableAt: string | null
  lastAvailableAt: string | null
}

