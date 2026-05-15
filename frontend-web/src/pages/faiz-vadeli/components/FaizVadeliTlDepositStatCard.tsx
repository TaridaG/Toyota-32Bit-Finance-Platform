import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { StatCardCopy } from '../faizVadeliDashboardCopy'
import { fetchTlDepositLatest } from '../api/tlDepositApi'
import { FaizVadeliStatCard } from './FaizVadeliStatCard'
import { TL_DEPOSIT_MATURITY_CODES, type TlDepositMaturityCode } from '../lib/tlDepositMaturity'

function formatPercent(value: number, locale: string): string {
  const nf = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return `%${nf.format(value)}`
}

export function FaizVadeliTlDepositStatCard({
  template,
  maturity,
  onMaturityChange,
  onShowHistory,
}: {
  template: StatCardCopy
  maturity: TlDepositMaturityCode
  onMaturityChange: (m: TlDepositMaturityCode) => void
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

  const maturitySelect = useMemo(
    () => (
      <div className="fi-faiz-tl-dep-maturity-wrap" data-fi-faiz-skip-card-activate>
        <select
          id="fi-faiz-tl-dep-maturity"
          className="fi-faiz-tl-dep-maturity-select"
          value={maturity}
          aria-label={t('faizVadeliPage.tlDeposit.maturitySelectAria')}
          onChange={(e) => {
            const v = e.target.value
            if (TL_DEPOSIT_MATURITY_CODES.includes(v as TlDepositMaturityCode)) {
              onMaturityChange(v as TlDepositMaturityCode)
            }
          }}
          onClick={(e) => e.stopPropagation()}
        >
          {TL_DEPOSIT_MATURITY_CODES.map((code) => (
            <option key={code} value={code}>
              {t(`faizVadeliPage.tlDeposit.maturityLong.${code}`)}
            </option>
          ))}
        </select>
      </div>
    ),
    [maturity, onMaturityChange, t],
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

    fetchTlDepositLatest(maturity)
      .then((d) => {
        if (cancelled) {
          return
        }
        const raw = d.value
        const num = raw == null ? NaN : typeof raw === 'number' ? raw : Number(raw)
        if (raw == null || Number.isNaN(num)) {
          setStat({
            ...template,
            value: t('faizVadeliPage.tlDeposit.liveNoData'),
            sub1: t('faizVadeliPage.tlDeposit.liveNoDataHint'),
            sub2: undefined,
            badge: undefined,
            delta: undefined,
            deltaTone: undefined,
          })
          return
        }
        setStat({
          ...template,
          value: formatPercent(num, locale),
          sub1: '',
          sub2: undefined,
          badge: undefined,
          delta: undefined,
          deltaTone: undefined,
        })
      })
      .catch(() => {
        if (!cancelled) {
          setStat({
            ...template,
            value: t('faizVadeliPage.tlDeposit.liveLoadError'),
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
  }, [template.title, template.icon, locale, t, maturity])

  return (
    <FaizVadeliStatCard
      stat={stat}
      headEndSlot={maturitySelect}
      valueSkeleton={loading}
      valueSkeletonAria={t('faizVadeliPage.tlDeposit.liveSkeletonAria')}
      interactive={!loading}
      onActivate={onShowHistory}
      interactiveAriaLabel={t('faizVadeliPage.tlDeposit.chartOpenHint')}
      interactiveTitle={t('faizVadeliPage.tlDeposit.chartOpenHint')}
    />
  )
}
