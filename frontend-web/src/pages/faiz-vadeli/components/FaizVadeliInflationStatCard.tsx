import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { StatCardCopy } from '../faizVadeliDashboardCopy'
import { fetchCpiLatest, type CpiMetricCode } from '../api/cpiApi'
import { FaizVadeliStatCard } from './FaizVadeliStatCard'

function formatPercent(value: number, locale: string): string {
  const nf = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return `%${nf.format(value)}`
}

function formatDelta(value: number, locale: string): string {
  const nf = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2, signDisplay: 'exceptZero' })
  return nf.format(value)
}

function formatMonthLabel(iso: string | null | undefined, locale: string): string {
  if (!iso) {
    return ''
  }
  const d = new Date(`${iso}T12:00:00`)
  if (Number.isNaN(d.getTime())) {
    return iso
  }
  return new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' }).format(d)
}

export function FaizVadeliInflationStatCard({
  template,
  active,
  onShowHistory,
}: {
  template: StatCardCopy
  active?: boolean
  onShowHistory: () => void
}) {
  const { t, i18n } = useTranslation('common')
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'
  const [loading, setLoading] = useState(true)
  const [stat, setStat] = useState<StatCardCopy>(() => ({
    ...template,
    value: '',
    sub1: '',
    delta: undefined,
    deltaTone: undefined,
  }))

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setStat({
      ...template,
      value: '',
      sub1: '',
      delta: undefined,
      deltaTone: undefined,
    })

    const metric: CpiMetricCode = 'YEARLY_PCT'
    fetchCpiLatest(metric)
      .then((d) => {
        if (cancelled) {
          return
        }
        const raw = d.value
        const num = raw == null ? NaN : typeof raw === 'number' ? raw : Number(raw)
        if (raw == null || Number.isNaN(num)) {
          setStat({
            ...template,
            value: t('faizVadeliPage.inflation.liveNoData'),
            sub1: t('faizVadeliPage.inflation.liveNoDataHint'),
            delta: undefined,
            deltaTone: undefined,
          })
          return
        }
        const deltaRaw = d.deltaVsPriorMonth
        const deltaNum =
          deltaRaw == null ? NaN : typeof deltaRaw === 'number' ? deltaRaw : Number(deltaRaw)
        const deltaStr = !Number.isNaN(deltaNum) ? formatDelta(deltaNum, locale) : undefined
        const deltaTone =
          deltaStr == null ? undefined : deltaNum > 0 ? 'negative' : deltaNum < 0 ? 'positive' : 'neutral'

        setStat({
          ...template,
          value: formatPercent(num, locale),
          sub1: formatMonthLabel(d.observationMonth, locale),
          delta: deltaStr,
          deltaTone,
        })
      })
      .catch(() => {
        if (!cancelled) {
          setStat({
            ...template,
            value: t('faizVadeliPage.inflation.liveLoadError'),
            sub1: '',
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
  }, [template.title, template.icon, locale, t])

  return (
    <FaizVadeliStatCard
      stat={stat}
      helpTerm="Enflasyon"
      helpPageKey="FAIZ_VADELI"
      helpElementId="FAIZ_VADELI:inflation"
      valueSkeleton={loading}
      valueSkeletonAria={t('faizVadeliPage.inflation.liveSkeletonAria')}
      interactive={!loading}
      active={active}
      onActivate={onShowHistory}
      interactiveAriaLabel={t('faizVadeliPage.inflation.chartOpenHint')}
      interactiveTitle={t('faizVadeliPage.inflation.chartOpenHint')}
    />
  )
}
