/**
 * Single integration point for market table sorting (URL `sort=field,direction` ↔ API ordering).
 * Add a new metric: extend MARKET_SORT_FIELDS + comparator branch / metricForSortField + UI onClick.
 */

export const MARKET_SORT_FIELDS = [
  'symbol',
  'price',
  'displayAmount',
  'volume24h',
  'openInterest',
  'spotSpreadPct',
  'change1D',
  'change1M',
  'change3M',
  'change6M',
  'change1Y',
  'trendScore',
] as const
export type MarketSortField = (typeof MARKET_SORT_FIELDS)[number]

const LEGACY_SORT_FIELD: Record<string, MarketSortField> = {
  change24h: 'change1D',
}

/** Sort keys that do not need 1D–1Y summary/history prefetch for the whole filtered list. */
const FIELDS_WITHOUT_PERIOD_PREFETCH = new Set<MarketSortField>([
  'price',
  'symbol',
  'displayAmount',
  'volume24h',
  'openInterest',
  'spotSpreadPct',
])

function isMarketSortField(value: string): value is MarketSortField {
  return (MARKET_SORT_FIELDS as readonly string[]).includes(value)
}

export function sortRequiresPeriodPrefetch(field: MarketSortField): boolean {
  return !FIELDS_WITHOUT_PERIOD_PREFETCH.has(field)
}

/** After sort, period data must still be loaded for the current page only (price / symbol / displayAmount sorts). */
export function sortRequiresPageSummaryFetch(field: MarketSortField): boolean {
  return FIELDS_WITHOUT_PERIOD_PREFETCH.has(field)
}

export function parseMarketSortQuery(sort: string | undefined): { field: MarketSortField; direction: 'asc' | 'desc' } {
  const [rawField = 'change1D', rawDir = 'desc'] = (sort ?? '').split(',')
  const mapped = LEGACY_SORT_FIELD[rawField] ?? rawField
  const field = isMarketSortField(mapped) ? mapped : 'change1D'
  const direction = rawDir === 'asc' ? 'asc' : 'desc'
  return { field, direction }
}

/** Populated server-side when sorting by the header-currency column. */
export const SORT_DISPLAY_AMOUNT_KEY = '__sortDisplayAmount' as const

/** TEFAS canonical rows use {@code FUND_TI2}-style symbols; US-listed ETFs in the same tab are plain tickers (QQQ, …). */
function canonicalFundSymbolRank(symbol: string): number {
  return symbol.trim().toUpperCase().startsWith('FUND_') ? 0 : 1
}

export type MarketOverviewSortOptions = {
  /**
   * Under the Markets "Fon" tab, keep TEFAS ({@code FUND_*}) ahead of US ETF tickers so NAV-flat funds
   * are not pushed past page 1 when sorting by daily % move (US names often have larger 1D deltas).
   */
  groupCanonicalFundsFirst?: boolean
}

export type MarketSortableRow = {
  symbol: string
  price: number
  change24h?: number
  change1D?: number
  change1M?: number
  change3M?: number
  change6M?: number
  change1Y?: number
  trendScore?: number
  volume24h?: number | null
  openInterest?: number | null
  spotSpreadPct?: number | null
  [SORT_DISPLAY_AMOUNT_KEY]?: number | null
}

export function metricForSortField(row: MarketSortableRow, field: MarketSortField): number {
  switch (field) {
    case 'symbol':
    case 'displayAmount':
      return 0
    case 'price':
      return row.price ?? 0
    case 'change1D':
      return row.change1D ?? row.change24h ?? 0
    case 'change1M':
      return row.change1M ?? 0
    case 'change3M':
      return row.change3M ?? 0
    case 'change6M':
      return row.change6M ?? 0
    case 'change1Y':
      return row.change1Y ?? 0
    case 'trendScore':
      return row.trendScore ?? 0
    case 'volume24h':
      return row.volume24h ?? 0
    case 'openInterest':
      return row.openInterest ?? 0
    case 'spotSpreadPct':
      return row.spotSpreadPct ?? 0
    default:
      return 0
  }
}

function compareNullableNumber(
  a: number | null | undefined,
  b: number | null | undefined,
  direction: 'asc' | 'desc',
): number {
  const aOk = a != null && Number.isFinite(a)
  const bOk = b != null && Number.isFinite(b)
  if (!aOk && !bOk) return 0
  if (!aOk) return 1
  if (!bOk) return -1
  const diff = (a as number) - (b as number)
  if (diff === 0) return 0
  if (direction === 'asc') {
    return diff < 0 ? -1 : 1
  }
  return diff < 0 ? 1 : -1
}

export function sortMarketOverviewRows<T extends MarketSortableRow>(
  rows: T[],
  sort: string | undefined,
  options?: MarketOverviewSortOptions,
): T[] {
  const { field, direction } = parseMarketSortQuery(sort)
  const next = [...rows]
  next.sort((a, b) => {
    if (options?.groupCanonicalFundsFirst) {
      const ra = canonicalFundSymbolRank(a.symbol)
      const rb = canonicalFundSymbolRank(b.symbol)
      if (ra !== rb) {
        return ra - rb
      }
    }
    if (field === 'symbol') {
      const diff = a.symbol.localeCompare(b.symbol)
      if (diff !== 0) {
        return direction === 'asc' ? diff : -diff
      }
      return 0
    }
    if (field === 'displayAmount') {
      const av = a[SORT_DISPLAY_AMOUNT_KEY]
      const bv = b[SORT_DISPLAY_AMOUNT_KEY]
      const c = compareNullableNumber(av, bv, direction)
      if (c !== 0) {
        return c
      }
      return a.symbol.localeCompare(b.symbol)
    }
    const va = metricForSortField(a, field)
    const vb = metricForSortField(b, field)
    const diff = va - vb
    if (diff !== 0) {
      return direction === 'asc' ? diff : -diff
    }
    return a.symbol.localeCompare(b.symbol)
  })
  return next
}

/** Map URL segment to the same field id used in sort query (canonical). */
export function marketSortFieldFromUrl(raw: string | undefined): MarketSortField {
  const mapped = raw ? LEGACY_SORT_FIELD[raw] ?? raw : 'change1D'
  return isMarketSortField(mapped) ? mapped : 'change1D'
}
