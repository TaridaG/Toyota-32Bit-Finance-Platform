import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { StatCardCopy } from '../faizVadeliDashboardCopy'
import { fetchMarketOverviewPage } from '../../../features/markets/api/marketService'
import type { MarketOverviewItem } from '../../../shared/types/market'
import { FaizVadeliStatCard } from './FaizVadeliStatCard'

const EM = '—'
const METAL_FUTURES_SYMBOLS = ['GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F'] as const
export type MetalFuturesSymbol = (typeof METAL_FUTURES_SYMBOLS)[number]

function isMetalFuturesSymbol(value: string): value is MetalFuturesSymbol {
  return (METAL_FUTURES_SYMBOLS as readonly string[]).includes(value)
}

function formatSignedPct(value: number, locale: string): string {
  const abs = Math.abs(value)
  const body = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(abs)
  if (value > 0) return `+${body}`
  if (value < 0) return `−${body}`
  return body
}

export function FaizVadeliMetalFuturesStatCard({
  template,
  symbol,
  active,
  onSymbolChange,
  onShowHistory,
}: {
  template: StatCardCopy
  symbol: MetalFuturesSymbol
  active?: boolean
  onSymbolChange: (next: MetalFuturesSymbol) => void
  onShowHistory: () => void
}) {
  const { t, i18n } = useTranslation('common')
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'
  const [loading, setLoading] = useState(true)
  const [rows, setRows] = useState<MarketOverviewItem[]>([])
  const [stat, setStat] = useState<StatCardCopy>(() => ({
    ...template,
    title: t('faizVadeliPage.metalFutures.cardTitle'),
    value: '',
    sub1: '',
    sub2: undefined,
    badge: undefined,
    delta: undefined,
    deltaTone: undefined,
  }))

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    fetchMarketOverviewPage({
      category: 'globalFutures',
      page: 0,
      size: METAL_FUTURES_SYMBOLS.length,
      sort: 'symbol,asc',
      displayCurrency: 'TRY',
    })
      .then((page) => {
        if (!cancelled) {
          setRows(page.content)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRows([])
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [locale])

  const row = useMemo(
    () => rows.find((r) => r.symbol.trim().toUpperCase() === symbol.toUpperCase()),
    [rows, symbol],
  )

  useEffect(() => {
    const usdFmt = new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })
    const pctFmt = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

    if (loading) {
      return
    }
    if (!row || !Number.isFinite(row.price) || row.price <= 0) {
      setStat({
        ...template,
        title: t('faizVadeliPage.metalFutures.cardTitle'),
        value: t('faizVadeliPage.metalFutures.liveNoData'),
        sub1: t('faizVadeliPage.metalFutures.liveNoDataHint'),
        sub2: undefined,
        badge: undefined,
        delta: undefined,
        deltaTone: undefined,
      })
      return
    }

    const spread = row.spotSpreadPct
    const spreadStr =
      spread != null && Number.isFinite(spread) ? `${pctFmt.format(spread)}%` : EM
    const spotLabel = row.linkedSpotSymbol ?? EM
    const ch = row.change1D ?? row.change24h
    const deltaStr = ch != null && Number.isFinite(ch) ? formatSignedPct(ch, locale) : undefined
    const deltaTone: 'positive' | 'negative' | 'neutral' | undefined =
      ch == null || !Number.isFinite(ch) ? undefined : ch > 0 ? 'positive' : ch < 0 ? 'negative' : 'neutral'

    setStat({
      ...template,
      title: t('faizVadeliPage.metalFutures.cardTitle'),
      value: usdFmt.format(row.price),
      sub1: t('faizVadeliPage.metalFutures.spotBasisSub', { spot: spotLabel, pct: spreadStr }),
      sub2: row.exchangeName ? t('faizVadeliPage.metalFutures.exchangeSub', { exchange: row.exchangeName }) : undefined,
      badge: undefined,
      delta: deltaStr,
      deltaTone,
    })
  }, [loading, row, template, locale, t])

  const symbolSelect = (
    <div className="fi-faiz-tl-dep-maturity-wrap" data-fi-faiz-skip-card-activate>
      <select
        className="fi-faiz-tl-dep-maturity-select fi-faiz-metal-futures-select"
        value={symbol}
        aria-label={t('faizVadeliPage.metalFutures.symbolSelectAria')}
        onChange={(e) => {
          const next = e.target.value
          if (isMetalFuturesSymbol(next)) {
            onSymbolChange(next)
            onShowHistory()
          }
        }}
      >
        {METAL_FUTURES_SYMBOLS.map((sym) => (
          <option key={sym} value={sym}>
            {sym}
          </option>
        ))}
      </select>
    </div>
  )

  return (
    <FaizVadeliStatCard
      stat={stat}
      headEndSlot={symbolSelect}
      valueSkeleton={loading}
      valueSkeletonAria={t('faizVadeliPage.metalFutures.liveSkeletonAria')}
      active={active}
      interactive={!loading}
      onActivate={onShowHistory}
      interactiveAriaLabel={t('faizVadeliPage.metalFutures.chartOpenHint')}
      interactiveTitle={t('faizVadeliPage.metalFutures.chartOpenHint')}
    />
  )
}
