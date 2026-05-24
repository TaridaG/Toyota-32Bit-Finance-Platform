import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchTlDepositLatest } from '../../api/tlDepositApi'
import { computeDepositProjection, parseAmountLoose } from '../../lib/depositSimulator'
import type { TlDepositMaturityCode } from '../../lib/tlDepositMaturity'
import {
  FaizVadeliSimulatorCard,
  SimulatorDisclaimer,
  SimulatorDivider,
  SimulatorHint,
  SimulatorInput,
  SimulatorLabelField,
  SimulatorMetrics,
  SimulatorSectionTitle,
} from '../FaizVadeliSimulatorCard'

const EM = '—'

export function TlDepositSimulator({
  maturity,
  chartEndDate,
  onRequestPickDepositDate,
  pickDepositDateActive,
}: {
  maturity: TlDepositMaturityCode
  chartEndDate?: string | null
  onRequestPickDepositDate?: () => void
  pickDepositDateActive?: boolean
}) {
  const { t, i18n } = useTranslation('common')
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [principal, setPrincipal] = useState('100000')
  const [annualRate, setAnnualRate] = useState('')
  const [depositDate, setDepositDate] = useState('')
  const [useToday, setUseToday] = useState(true)
  const [endDate, setEndDate] = useState('')

  useEffect(() => {
    let cancelled = false
    void fetchTlDepositLatest(maturity)
      .then((d) => {
        if (cancelled) return
        const v = d.value
        if (v != null && Number.isFinite(Number(v))) {
          setAnnualRate(String(Number(v)))
        }
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [maturity])

  useEffect(() => {
    if (chartEndDate) {
      setDepositDate(chartEndDate.slice(0, 10))
    }
  }, [chartEndDate])

  useEffect(() => {
    if (useToday) {
      setEndDate(new Date().toISOString().slice(0, 10))
    }
  }, [useToday])

  const fmtTry = (v: number | null) =>
    v == null
      ? EM
      : new Intl.NumberFormat(locale, { style: 'currency', currency: 'TRY', maximumFractionDigits: 2 }).format(v)

  const fmtPct = (v: number | null) =>
    v == null ? EM : `${new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)}%`

  const projection = useMemo(() => {
    const P = parseAmountLoose(principal)
    const r = parseAmountLoose(annualRate)
    const start = depositDate.length >= 10 ? depositDate.slice(0, 10) : null
    const end = useToday ? new Date().toISOString().slice(0, 10) : endDate.length >= 10 ? endDate.slice(0, 10) : null
    if (P == null || P <= 0 || r == null || r < 0 || !start || !end) return null
    return computeDepositProjection(P, r, start, end)
  }, [principal, annualRate, depositDate, useToday, endDate])

  const headlineRows = useMemo(() => {
    const r = parseAmountLoose(annualRate)
    return [
      {
        label: t('faizVadeliPage.simulator.deposit.rateUsed'),
        value: r == null ? EM : fmtPct(r),
      },
      {
        label: t('faizVadeliPage.simulator.deposit.projectedBalance'),
        value: projection ? fmtTry(projection.projectedBalance) : EM,
      },
      {
        label: t('faizVadeliPage.simulator.deposit.interestEarned'),
        value: projection ? fmtTry(projection.interestEarned) : EM,
      },
    ]
  }, [principal, annualRate, projection, locale, t])

  const scenarioRows = useMemo(
    () => [
      {
        label: t('faizVadeliPage.simulator.deposit.daysHeld'),
        value: projection ? String(projection.days) : EM,
      },
      {
        label: t('faizVadeliPage.simulator.deposit.returnPct'),
        value: projection ? fmtPct(projection.returnPercent) : EM,
      },
      {
        label: t('faizVadeliPage.simulator.deposit.principalAtStart'),
        value: (() => {
          const P = parseAmountLoose(principal)
          return P == null ? EM : fmtTry(P)
        })(),
      },
    ],
    [projection, principal, locale, t],
  )

  return (
    <FaizVadeliSimulatorCard title={t('faizVadeliPage.simulator.deposit.title')}>
      <SimulatorLabelField label={t('faizVadeliPage.simulator.deposit.principal')}>
        <SimulatorInput inputMode="decimal" value={principal} onChange={(e) => setPrincipal(e.target.value)} />
      </SimulatorLabelField>
      <SimulatorHint>{t('faizVadeliPage.simulator.deposit.principalHint')}</SimulatorHint>

      <SimulatorMetrics rows={headlineRows} />
      <SimulatorDisclaimer>{t('faizVadeliPage.simulator.deposit.headlineDisclaimer')}</SimulatorDisclaimer>

      <SimulatorDivider />

      <SimulatorSectionTitle>{t('faizVadeliPage.simulator.deposit.scenarioTitle')}</SimulatorSectionTitle>

      <SimulatorLabelField label={t('faizVadeliPage.simulator.deposit.annualRate')}>
        <SimulatorInput inputMode="decimal" value={annualRate} onChange={(e) => setAnnualRate(e.target.value)} />
      </SimulatorLabelField>

      <SimulatorLabelField label={t('faizVadeliPage.simulator.deposit.depositDate')}>
        <div className="fi-faiz-simulator-field-row">
          <SimulatorInput type="date" value={depositDate} onChange={(e) => setDepositDate(e.target.value)} />
          {onRequestPickDepositDate ? (
            <button
              type="button"
              className={`fi-faiz-simulator-pick-btn${pickDepositDateActive ? ' fi-faiz-simulator-pick-btn--on' : ''}`}
              title={t('faizVadeliPage.simulator.deposit.pickFromChart')}
              aria-label={t('faizVadeliPage.simulator.deposit.pickFromChart')}
              onClick={onRequestPickDepositDate}
            >
              ◎
            </button>
          ) : null}
        </div>
      </SimulatorLabelField>

      <SimulatorLabelField label={t('faizVadeliPage.simulator.deposit.endDate')} className="fi-faiz-simulator-check-row">
        <label className="fi-faiz-simulator-inline-check">
          <input
            type="checkbox"
            checked={useToday}
            onChange={(e) => {
              setUseToday(e.target.checked)
              if (e.target.checked) setEndDate('')
            }}
          />
          <span>{t('faizVadeliPage.simulator.deposit.endToday')}</span>
        </label>
      </SimulatorLabelField>

      {!useToday ? (
        <SimulatorLabelField label={t('faizVadeliPage.simulator.deposit.endDateManual')}>
          <SimulatorInput type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </SimulatorLabelField>
      ) : null}

      <SimulatorMetrics rows={scenarioRows} />
      <SimulatorDisclaimer>{t('faizVadeliPage.simulator.deposit.scenarioDisclaimer')}</SimulatorDisclaimer>
    </FaizVadeliSimulatorCard>
  )
}
