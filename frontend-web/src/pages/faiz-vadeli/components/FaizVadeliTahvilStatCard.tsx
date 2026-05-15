import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { StatCardCopy } from '../faizVadeliDashboardCopy'
import { fetchTahvilSummary } from '../api/tahvilMarketApi'
import { FaizVadeliStatCard } from './FaizVadeliStatCard'
import { TAHVIL_SYMBOLS, type TahvilSymbol } from '../lib/tahvilSymbol'

function formatPercent(value: number, locale: string): string {
  const nf = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return `%${nf.format(value)}`
}

function formatSignedPct(value: number, locale: string): string {
  return (
    new Intl.NumberFormat(locale, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
      signDisplay: 'exceptZero',
    }).format(value) + '%'
  )
}

export function FaizVadeliTahvilStatCard({
  template,
  symbol,
  onSymbolChange,
  onShowHistory,
}: {
  template: StatCardCopy
  symbol: TahvilSymbol
  onSymbolChange: (s: TahvilSymbol) => void
  onShowHistory: () => void
}) {
  const { t, i18n } = useTranslation('common')
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'
  const [loading, setLoading] = useState(true)
  const [stat, setStat] = useState<StatCardCopy>(() => ({
    ...template,
    value: '',
    sub1: '',
    sub2: undefined,
    badge: undefined,
    delta: undefined,
    deltaTone: undefined,
  }))

  const tenorSelect = useMemo(
    () => (
      <div className="fi-faiz-tl-dep-maturity-wrap" data-fi-faiz-skip-card-activate>
        <select
          id="fi-faiz-tahvil-symbol"
          className="fi-faiz-tl-dep-maturity-select"
          value={symbol}
          aria-label={t('faizVadeliPage.tahvil.tenorSelectAria')}
          onChange={(e) => {
            const v = e.target.value
            if (TAHVIL_SYMBOLS.includes(v as TahvilSymbol)) {
              onSymbolChange(v as TahvilSymbol)
            }
          }}
          onClick={(e) => e.stopPropagation()}
        >
          {TAHVIL_SYMBOLS.map((code) => (
            <option key={code} value={code}>
              {t(`faizVadeliPage.tahvil.symbolLabel.${code}`)}
            </option>
          ))}
        </select>
      </div>
    ),
    [symbol, onSymbolChange, t],
  )

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setStat({
      ...template,
      value: '',
      sub1: '',
      sub2: undefined,
      badge: undefined,
      delta: undefined,
      deltaTone: undefined,
    })

    fetchTahvilSummary(symbol)
      .then((row) => {
        if (cancelled) {
          return
        }
        const raw = row?.price
        const num = raw == null ? NaN : typeof raw === 'number' ? raw : Number(raw)
        if (row == null || raw == null || Number.isNaN(num)) {
          setStat({
            ...template,
            value: t('faizVadeliPage.tahvil.liveNoData'),
            sub1: t('faizVadeliPage.tahvil.liveNoDataHint'),
            sub2: undefined,
            badge: undefined,
            delta: undefined,
            deltaTone: undefined,
          })
          return
        }
        const ch = row.change1D ?? 0
        const deltaStr = formatSignedPct(ch, locale)
        const deltaTone: 'positive' | 'negative' | 'neutral' =
          ch > 0 ? 'positive' : ch < 0 ? 'negative' : 'neutral'
        const pctFmt = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(ch)
        setStat({
          ...template,
          value: formatPercent(num, locale),
          sub1: t('faizVadeliPage.tahvil.change1dSub', { pct: pctFmt }),
          sub2: undefined,
          badge: undefined,
          delta: ch === 0 ? undefined : deltaStr,
          deltaTone: ch === 0 ? undefined : deltaTone,
        })
      })
      .catch(() => {
        if (!cancelled) {
          setStat({
            ...template,
            value: t('faizVadeliPage.tahvil.liveLoadError'),
            sub1: '',
            sub2: undefined,
            badge: undefined,
            delta: undefined,
            deltaTone: undefined,
          })
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
  }, [template.title, template.icon, locale, t, symbol])

  return (
    <FaizVadeliStatCard
      stat={stat}
      headEndSlot={tenorSelect}
      valueSkeleton={loading}
      valueSkeletonAria={t('faizVadeliPage.tahvil.liveSkeletonAria')}
      interactive={!loading}
      onActivate={onShowHistory}
      interactiveAriaLabel={t('faizVadeliPage.tahvil.chartOpenHint')}
      interactiveTitle={t('faizVadeliPage.tahvil.chartOpenHint')}
    />
  )
}
