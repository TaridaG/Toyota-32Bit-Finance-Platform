import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchCpiHistory } from '../../api/cpiApi'
import {
  computeInflationProjection,
  normalizeCpiIndexSeries,
  parseAmountLoose,
} from '../../lib/inflationSimulator'
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

export function InflationSimulator({
  chartStartDate,
  pickStartDateActive,
  onRequestPickStartDate,
}: {
  chartStartDate?: string | null
  pickStartDateActive?: boolean
  onRequestPickStartDate?: () => void
}) {
  const { t, i18n } = useTranslation('common')
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [principal, setPrincipal] = useState('100000')
  const [startDate, setStartDate] = useState('')
  const [useToday, setUseToday] = useState(true)
  const [endDate, setEndDate] = useState('')
  const [indexLoadState, setIndexLoadState] = useState<'loading' | 'error' | 'ready'>('loading')
  const [indexSeries, setIndexSeries] = useState<ReturnType<typeof normalizeCpiIndexSeries>>([])

  useEffect(() => {
    let cancelled = false
    setIndexLoadState('loading')
    void fetchCpiHistory('INDEX')
      .then((r) => {
        if (cancelled) return
        const series = normalizeCpiIndexSeries(r.points ?? [])
        setIndexSeries(series)
        setIndexLoadState('ready')
        if (series.length > 0 && !startDate) {
          const first = series[0].date
          const yearAgo = series.length > 12 ? series[series.length - 13].date : first
          setStartDate(yearAgo.slice(0, 10))
        }
      })
      .catch(() => {
        if (!cancelled) setIndexLoadState('error')
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (chartStartDate) {
      setStartDate(chartStartDate.slice(0, 10))
    }
  }, [chartStartDate])

  useEffect(() => {
    if (useToday) {
      setEndDate(new Date().toISOString().slice(0, 10))
    }
  }, [useToday])

  const fmtTry = (v: number | null) =>
    v == null
      ? EM
      : new Intl.NumberFormat(locale, { style: 'currency', currency: 'TRY', maximumFractionDigits: 2 }).format(v)

  const fmtPct = (v: number | null, digits = 2) =>
    v == null
      ? EM
      : `${new Intl.NumberFormat(locale, { minimumFractionDigits: digits, maximumFractionDigits: digits }).format(v)}%`

  const fmtIndex = (v: number | null) =>
    v == null
      ? EM
      : new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)

  const endIso = useToday ? new Date().toISOString().slice(0, 10) : endDate.length >= 10 ? endDate.slice(0, 10) : null
  const startIso = startDate.length >= 10 ? startDate.slice(0, 10) : null

  const projection = useMemo(() => {
    const P = parseAmountLoose(principal)
    if (P == null || P <= 0 || !startIso || !endIso || indexSeries.length < 2) return null
    return computeInflationProjection(P, startIso, endIso, indexSeries)
  }, [principal, startIso, endIso, indexSeries])

  const dataReady = indexLoadState === 'ready' && indexSeries.length >= 2

  const headlineRows = useMemo(
    () => [
      {
        label: t('faizVadeliPage.simulator.inflation.cumulativeInflation'),
        value: projection ? fmtPct(projection.cumulativeInflationPct) : EM,
      },
      {
        label: t('faizVadeliPage.simulator.inflation.purchasingPowerLoss'),
        value: projection ? fmtTry(projection.purchasingPowerLoss) : EM,
      },
      {
        label: t('faizVadeliPage.simulator.inflation.nominalToPreserve'),
        value: projection ? fmtTry(projection.nominalToPreservePower) : EM,
      },
    ],
    [projection, locale, t],
  )

  const scenarioRows = useMemo(
    () => [
      {
        label: t('faizVadeliPage.simulator.inflation.purchasingPowerToday'),
        value: projection ? fmtTry(projection.purchasingPowerNominal) : EM,
      },
      {
        label: t('faizVadeliPage.simulator.inflation.lossPct'),
        value: projection ? fmtPct(projection.purchasingPowerLossPct) : EM,
      },
      {
        label: t('faizVadeliPage.simulator.inflation.annualizedInflation'),
        value: projection ? fmtPct(projection.annualizedInflationPct) : EM,
      },
      {
        label: t('faizVadeliPage.simulator.inflation.indexRange'),
        value: projection ? `${fmtIndex(projection.indexStart)} → ${fmtIndex(projection.indexEnd)}` : EM,
      },
      {
        label: t('faizVadeliPage.simulator.inflation.monthsCovered'),
        value: projection ? String(projection.months) : EM,
      },
    ],
    [projection, locale, t],
  )

  return (
    <FaizVadeliSimulatorCard title={t('faizVadeliPage.simulator.inflation.title')}>
      <SimulatorLabelField label={t('faizVadeliPage.simulator.inflation.principal')}>
        <SimulatorInput inputMode="decimal" value={principal} onChange={(e) => setPrincipal(e.target.value)} />
      </SimulatorLabelField>
      <SimulatorHint>{t('faizVadeliPage.simulator.inflation.principalHint')}</SimulatorHint>

      {indexLoadState === 'loading' ? (
        <SimulatorHint>{t('faizVadeliPage.simulator.inflation.indexLoading')}</SimulatorHint>
      ) : null}
      {indexLoadState === 'error' ? (
        <SimulatorDisclaimer>{t('faizVadeliPage.simulator.inflation.indexError')}</SimulatorDisclaimer>
      ) : null}

      <SimulatorMetrics rows={headlineRows} />
      <SimulatorDisclaimer>
        {dataReady
          ? t('faizVadeliPage.simulator.inflation.headlineDisclaimer')
          : t('faizVadeliPage.simulator.inflation.headlineDisclaimerNoData')}
      </SimulatorDisclaimer>

      <SimulatorDivider />

      <SimulatorSectionTitle>{t('faizVadeliPage.simulator.inflation.scenarioTitle')}</SimulatorSectionTitle>

      <SimulatorLabelField label={t('faizVadeliPage.simulator.inflation.startDate')}>
        <div className="fi-faiz-simulator-field-row">
          <SimulatorInput type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          {onRequestPickStartDate ? (
            <button
              type="button"
              className={`fi-faiz-simulator-pick-btn${pickStartDateActive ? ' fi-faiz-simulator-pick-btn--on' : ''}`}
              title={t('faizVadeliPage.simulator.inflation.pickFromChart')}
              aria-label={t('faizVadeliPage.simulator.inflation.pickFromChart')}
              onClick={onRequestPickStartDate}
            >
              ◎
            </button>
          ) : null}
        </div>
      </SimulatorLabelField>

      <SimulatorLabelField label={t('faizVadeliPage.simulator.inflation.endDate')} className="fi-faiz-simulator-check-row">
        <label className="fi-faiz-simulator-inline-check">
          <input
            type="checkbox"
            checked={useToday}
            onChange={(e) => {
              setUseToday(e.target.checked)
              if (e.target.checked) setEndDate('')
            }}
          />
          <span>{t('faizVadeliPage.simulator.inflation.endToday')}</span>
        </label>
      </SimulatorLabelField>

      {!useToday ? (
        <SimulatorLabelField label={t('faizVadeliPage.simulator.inflation.endDateManual')}>
          <SimulatorInput type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </SimulatorLabelField>
      ) : null}

      <SimulatorMetrics rows={scenarioRows} />
      <SimulatorDisclaimer>{t('faizVadeliPage.simulator.inflation.scenarioDisclaimer')}</SimulatorDisclaimer>
    </FaizVadeliSimulatorCard>
  )
}
