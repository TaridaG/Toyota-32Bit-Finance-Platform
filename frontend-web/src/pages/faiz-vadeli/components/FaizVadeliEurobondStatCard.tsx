import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { StatCardCopy } from '../faizVadeliDashboardCopy'
import { fetchTrEurobondInstruments, toNum, type EurobondInstrumentWire } from '../api/eurobondMarketApi'
import { FaizVadeliStatCard } from './FaizVadeliStatCard'
import { TR_USD_EUROBOND_DEFAULT_ISIN } from '../lib/trUsdEurobondIsins'

const EM = '—'

export function FaizVadeliEurobondStatCard({
  template,
  selectedIsin,
  instruments,
  onInstrumentsLoaded,
  onIsinChange,
  onShowHistory,
}: {
  template: StatCardCopy
  selectedIsin: string
  instruments: EurobondInstrumentWire[]
  onInstrumentsLoaded: (rows: EurobondInstrumentWire[]) => void
  onIsinChange: (isin: string) => void
  onShowHistory: () => void
}) {
  const { t, i18n } = useTranslation('common')
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'
  const [loading, setLoading] = useState(true)
  const [stat, setStat] = useState<StatCardCopy>(() => ({
    ...template,
    title: t('faizVadeliPage.eurobond.cardTitle'),
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
    fetchTrEurobondInstruments()
      .then((rows) => {
        if (cancelled) return
        onInstrumentsLoaded(rows)
        if (rows.length > 0 && !rows.some((r) => (r.isin ?? '').toUpperCase() === selectedIsin.toUpperCase())) {
          const first = (rows[0].isin ?? TR_USD_EUROBOND_DEFAULT_ISIN).toUpperCase()
          onIsinChange(first)
        }
      })
      .catch(() => {
        if (!cancelled) onInstrumentsLoaded([])
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [onInstrumentsLoaded])

  const row = useMemo(
    () => instruments.find((x) => (x.isin ?? '').toUpperCase() === selectedIsin.toUpperCase()),
    [instruments, selectedIsin],
  )

  useEffect(() => {
    const fmtPrice = (v: number | null) =>
      v == null
        ? EM
        : new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)
    const fmtPct = (v: number | null) =>
      v == null
        ? EM
        : `${new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)}%`

    if (loading) {
      return
    }
    if (!row) {
      setStat({
        ...template,
        title: t('faizVadeliPage.eurobond.cardTitle'),
        value: t('faizVadeliPage.eurobond.cardNoData'),
        sub1: t('faizVadeliPage.eurobond.cardSubtitle'),
        sub2: undefined,
        badge: undefined,
        delta: undefined,
        deltaTone: undefined,
      })
      return
    }
    const px = toNum(row.cleanPrice)
    const ytm = toNum(row.yieldToMaturityPercent)
    const mat = row.maturityDate ?? EM
    const src = row.sourceProvider ?? EM
    const d1 = toNum(row.dailyChangePercent)
    const valueMain = px != null ? fmtPrice(px) : ytm != null ? fmtPct(ytm) : EM
    const sub1 = `${t('faizVadeliPage.eurobond.cardYtm')}: ${fmtPct(ytm)} · ${t('faizVadeliPage.eurobond.cardMat')}: ${mat}`
    const sub2 = `${t('faizVadeliPage.eurobond.cardSrc')}: ${src}`
    const deltaTone: 'positive' | 'negative' | 'neutral' | undefined =
      d1 == null ? undefined : d1 > 0 ? 'positive' : d1 < 0 ? 'negative' : 'neutral'
    setStat({
      ...template,
      title: t('faizVadeliPage.eurobond.cardTitle'),
      value: valueMain,
      sub1,
      sub2,
      badge: undefined,
      delta: d1 == null || d1 === 0 ? undefined : fmtPct(d1),
      deltaTone,
    })
  }, [template, loading, row, locale, t])

  const select = useMemo(
    () => (
      <div className="fi-faiz-tl-dep-maturity-wrap" data-fi-faiz-skip-card-activate>
        <select
          id="fi-faiz-eurobond-isin"
          className="fi-faiz-tl-dep-maturity-select"
          value={selectedIsin}
          aria-label={t('faizVadeliPage.eurobond.isinSelectAria')}
          onChange={(e) => onIsinChange(e.target.value.toUpperCase())}
          onClick={(e) => e.stopPropagation()}
        >
          {instruments.map((r) => {
            const isin = (r.isin ?? '').toUpperCase()
            return (
              <option key={isin} value={isin}>
                {r.displayName ?? isin}
              </option>
            )
          })}
        </select>
      </div>
    ),
    [instruments, selectedIsin, onIsinChange, t],
  )

  return (
    <FaizVadeliStatCard
      stat={stat}
      headEndSlot={instruments.length > 0 ? select : undefined}
      valueSkeleton={loading}
      valueSkeletonAria={t('faizVadeliPage.eurobond.liveSkeletonAria')}
      interactive={!loading && instruments.length > 0}
      onActivate={onShowHistory}
      interactiveAriaLabel={t('faizVadeliPage.eurobond.chartOpenHint')}
      interactiveTitle={t('faizVadeliPage.eurobond.chartOpenHint')}
    />
  )
}
