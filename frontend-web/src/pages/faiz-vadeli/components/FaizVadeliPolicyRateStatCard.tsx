import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { StatCardCopy } from '../faizVadeliDashboardCopy'
import { fetchPolicyRateLatest } from '../api/policyRateApi'
import { FaizVadeliStatCard } from './FaizVadeliStatCard'

function formatPercent(value: number, locale: string): string {
  const nf = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return `%${nf.format(value)}`
}

function formatDecisionDate(iso: string | null | undefined, locale: string): string {
  if (!iso) {
    return ''
  }
  const d = new Date(`${iso}T12:00:00`)
  if (Number.isNaN(d.getTime())) {
    return iso
  }
  return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'long', year: 'numeric' }).format(d)
}

export function FaizVadeliPolicyRateStatCard({
  template,
  onShowHistory,
}: {
  template: StatCardCopy
  onShowHistory: () => void
}) {
  const { t, i18n } = useTranslation('common')
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'
  const [loading, setLoading] = useState(true)
  const [stat, setStat] = useState<StatCardCopy>(() => ({
    ...template,
    value: '',
    sub1: '',
    badge: undefined,
    delta: undefined,
  }))

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setStat({
      ...template,
      value: '',
      sub1: '',
      badge: undefined,
      delta: undefined,
    })

    fetchPolicyRateLatest()
      .then((d) => {
        if (cancelled) {
          return
        }
        const raw = d.value
        const num = raw == null ? NaN : typeof raw === 'number' ? raw : Number(raw)
        if (raw == null || Number.isNaN(num)) {
          setStat({
            ...template,
            value: t('faizVadeliPage.policyRate.liveNoData'),
            sub1: t('faizVadeliPage.policyRate.liveNoDataHint'),
            badge: undefined,
            delta: undefined,
          })
          return
        }
        const v = num
        const badge =
          d.changeVsPrior === 'UP'
            ? t('faizVadeliPage.policyRate.liveBadgeUp')
            : d.changeVsPrior === 'DOWN'
              ? t('faizVadeliPage.policyRate.liveBadgeDown')
              : t('faizVadeliPage.policyRate.liveBadgeUnchanged')
        setStat({
          ...template,
          value: formatPercent(v, locale),
          sub1: `${t('faizVadeliPage.policyRate.liveDecision')}: ${formatDecisionDate(d.decisionDate, locale)}`,
          badge,
          delta: undefined,
        })
      })
      .catch(() => {
        if (!cancelled) {
          setStat({
            ...template,
            value: t('faizVadeliPage.policyRate.liveLoadError'),
            sub1: '',
            badge: undefined,
            delta: undefined,
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
      valueSkeleton={loading}
      valueSkeletonAria={t('faizVadeliPage.policyRate.liveSkeletonAria')}
      interactive={!loading}
      onActivate={onShowHistory}
      interactiveAriaLabel={t('faizVadeliPage.policyRate.chartOpenHint')}
      interactiveTitle={t('faizVadeliPage.policyRate.chartOpenHint')}
    />
  )
}
