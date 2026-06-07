import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { StatCardCopy } from '../faizVadeliDashboardCopy'
import { fetchBondYieldLatest } from '../api/bondMarketApi'
import { BOND_TENOR_CODES, type BondTenorCode } from '../lib/bondTenor'
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

export function FaizVadeliBondStatCard({
  template,
  tenor,
  onTenorChange,
  active,
  onShowHistory,
}: {
  template: StatCardCopy
  tenor: BondTenorCode
  onTenorChange: (t: BondTenorCode) => void
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

  const tenorSelect = useMemo(
    () => (
      <div className="fi-faiz-tl-dep-maturity-wrap" data-fi-faiz-skip-card-activate>
        <select
          id="fi-faiz-bond-tenor"
          className="fi-faiz-tl-dep-maturity-select"
          value={tenor}
          aria-label={t('faizVadeliPage.bond.tenorSelectAria')}
          onChange={(e) => {
            const v = e.target.value
            if (BOND_TENOR_CODES.includes(v as BondTenorCode)) {
              onTenorChange(v as BondTenorCode)
            }
          }}
          onClick={(e) => e.stopPropagation()}
        >
          {BOND_TENOR_CODES.map((code) => (
            <option key={code} value={code}>
              {t(`faizVadeliPage.bond.tenorShort.${code}`)}
            </option>
          ))}
        </select>
      </div>
    ),
    [tenor, onTenorChange, t],
  )

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

    fetchBondYieldLatest(tenor)
      .then((d) => {
        if (cancelled) {
          return
        }
        const raw = d.value
        const num = raw == null ? NaN : typeof raw === 'number' ? raw : Number(raw)
        if (raw == null || Number.isNaN(num)) {
          setStat({
            ...template,
            value: t('faizVadeliPage.bond.liveNoData'),
            sub1: t('faizVadeliPage.bond.liveNoDataHint'),
            delta: undefined,
            deltaTone: undefined,
          })
          return
        }
        const bp = d.change1dBasisPoints
        let sub1 = `${t('faizVadeliPage.bond.liveObservation')}: ${formatObservationDate(d.observationDate, locale)}`
        let delta: string | undefined
        let deltaTone: StatCardCopy['deltaTone']
        if (bp != null && Number.isFinite(bp)) {
          const sign = bp > 0 ? '+' : ''
          sub1 = t('faizVadeliPage.bond.change1dSub', { bp: `${sign}${bp}` })
          const pct = bp / 100
          const nf = new Intl.NumberFormat(locale, {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
            signDisplay: 'exceptZero',
          })
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
            value: t('faizVadeliPage.bond.liveLoadError'),
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
  }, [template.title, template.icon, locale, t, tenor])

  return (
    <FaizVadeliStatCard
      stat={stat}
      helpTerm="TR Bono"
      helpPageKey="FAIZ_VADELI"
      helpElementId="FAIZ_VADELI:tr-bono"
      headEndSlot={tenorSelect}
      valueSkeleton={loading}
      valueSkeletonAria={t('faizVadeliPage.bond.liveSkeletonAria')}
      interactive={!loading}
      active={active}
      onActivate={onShowHistory}
      interactiveAriaLabel={t('faizVadeliPage.bond.chartOpenHint')}
      interactiveTitle={t('faizVadeliPage.bond.chartOpenHint')}
    />
  )
}
