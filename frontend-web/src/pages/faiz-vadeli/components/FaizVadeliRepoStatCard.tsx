import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { StatCardCopy } from '../faizVadeliDashboardCopy'
import { fetchRepoRateLatest } from '../api/repoRateApi'
import { FaizVadeliStatCard } from './FaizVadeliStatCard'

function formatPercent(value: number, locale: string): string {
  const nf = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return `%${nf.format(value)}`
}

function formatObservationDate(iso: string | null | undefined, locale: string): string {
  if (!iso) {
    return ''
  }
  const d = new Date(`${iso}T12:00:00`)
  if (Number.isNaN(d.getTime())) {
    return iso
  }
  return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'long', year: 'numeric' }).format(d)
}

export function FaizVadeliRepoStatCard({
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

    fetchRepoRateLatest()
      .then((d) => {
        if (cancelled) {
          return
        }
        const raw = d.value
        const num = raw == null ? NaN : typeof raw === 'number' ? raw : Number(raw)
        if (raw == null || Number.isNaN(num)) {
          setStat({
            ...template,
            value: t('faizVadeliPage.repo.liveNoData'),
            sub1: t('faizVadeliPage.repo.liveNoDataHint'),
            delta: undefined,
            deltaTone: undefined,
          })
          return
        }
        const bp = d.change1dBasisPoints
        let sub1 = `${t('faizVadeliPage.repo.liveObservation')}: ${formatObservationDate(d.observationDate, locale)}`
        let delta: string | undefined
        let deltaTone: StatCardCopy['deltaTone']
        if (bp != null && Number.isFinite(bp)) {
          const sign = bp > 0 ? '+' : ''
          sub1 = t('faizVadeliPage.repo.change1dSub', { bp: `${sign}${bp}` })
          const pct = bp / 100
          const nf = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2, signDisplay: 'exceptZero' })
          delta = nf.format(pct)
          deltaTone = bp > 0 ? 'positive' : bp < 0 ? 'negative' : 'neutral'
        }
        setStat({
          ...template,
          value: formatPercent(num, locale),
          sub1,
          delta,
          deltaTone,
        })
      })
      .catch(() => {
        if (!cancelled) {
          setStat({
            ...template,
            value: t('faizVadeliPage.repo.liveLoadError'),
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
      helpTerm="Repo"
      helpPageKey="FAIZ_VADELI"
      helpElementId="FAIZ_VADELI:repo"
      valueSkeleton={loading}
      valueSkeletonAria={t('faizVadeliPage.repo.liveSkeletonAria')}
      interactive={!loading}
      active={active}
      onActivate={onShowHistory}
      interactiveAriaLabel={t('faizVadeliPage.repo.chartOpenHint')}
      interactiveTitle={t('faizVadeliPage.repo.chartOpenHint')}
    />
  )
}
