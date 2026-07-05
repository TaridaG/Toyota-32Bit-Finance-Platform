import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { StatCardCopy } from '../faizVadeliDashboardCopy'
import { fetchTahvilSummary } from '../api/tahvilMarketApi'
import { FaizVadeliStatCard } from './FaizVadeliStatCard'

function formatNumber(value: number, locale: string): string {
  return new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)
}

export function FaizVadeliTahvilStatCard({
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

    Promise.all([fetchTahvilSummary('VIOP_TLREF_NEAR'), fetchTahvilSummary('VIOP_DIBS_NEAR')])
      .then(([tlref, dibs]) => {
        if (cancelled) return
        const tlrefPrice = tlref?.price
        const tlrefNum = tlrefPrice == null ? NaN : Number(tlrefPrice)
        if (tlrefPrice == null || Number.isNaN(tlrefNum)) {
          setStat({
            ...template,
            value: t('faizVadeliPage.tahvil.liveNoData'),
            sub1: t('faizVadeliPage.tahvil.liveNoDataHint'),
            delta: undefined,
            deltaTone: undefined,
          })
          return
        }

        const dibsPrice = dibs?.price
        const dibsNum = dibsPrice == null ? NaN : Number(dibsPrice)
        const dibsLabel =
          dibsPrice != null && !Number.isNaN(dibsNum)
            ? `${t('faizVadeliPage.tahvil.cardDibsLabel')}: ${formatNumber(dibsNum, locale)}`
            : t('faizVadeliPage.tahvil.cardDibsNoData')

        const change = tlref?.change1D
        let delta: string | undefined
        let deltaTone: StatCardCopy['deltaTone']
        if (change != null && Number.isFinite(Number(change))) {
          const pct = Number(change)
          delta = new Intl.NumberFormat(locale, {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
            signDisplay: 'exceptZero',
          }).format(pct)
          deltaTone = pct > 0 ? 'positive' : pct < 0 ? 'negative' : 'neutral'
        }

        setStat({
          ...template,
          value: formatNumber(tlrefNum, locale),
          sub1: dibsLabel,
          delta,
          deltaTone,
        })
      })
      .catch(() => {
        if (!cancelled) {
          setStat({
            ...template,
            value: t('faizVadeliPage.tahvil.liveLoadError'),
            sub1: '',
            delta: undefined,
            deltaTone: undefined,
          })
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [template.title, template.icon, locale, t])

  return (
    <FaizVadeliStatCard
      stat={stat}
      valueSkeleton={loading}
      valueSkeletonAria={t('faizVadeliPage.tahvil.liveSkeletonAria')}
      interactive={!loading}
      onActivate={onShowHistory}
      interactiveAriaLabel={t('faizVadeliPage.tahvil.detailOpenHint')}
      interactiveTitle={t('faizVadeliPage.tahvil.detailOpenHint')}
    />
  )
}
