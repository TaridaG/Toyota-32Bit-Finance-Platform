import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchBondYieldLatest } from '../../api/bondMarketApi'
import { computeBondHoldProjection, parseAmountLoose } from '../../lib/bondSimulator'
import type { BondTenorCode } from '../../lib/bondTenor'
import { tenorYearsFromCode } from '../../lib/bondTenor'
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

export function BondSimulator({
  tenor,
  chartYieldPercent,
}: {
  tenor: BondTenorCode
  chartYieldPercent?: number | null
}) {
  const { t, i18n } = useTranslation('common')
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [investment, setInvestment] = useState('100000')
  const [yieldRate, setYieldRate] = useState('')

  useEffect(() => {
    let cancelled = false
    void fetchBondYieldLatest(tenor)
      .then((d) => {
        if (cancelled) return
        const v = d.value
        if (v != null && Number.isFinite(Number(v))) {
          setYieldRate(String(Number(v)))
        }
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [tenor])

  useEffect(() => {
    if (chartYieldPercent != null && Number.isFinite(chartYieldPercent)) {
      setYieldRate(String(chartYieldPercent))
    }
  }, [chartYieldPercent])

  const fmtTry = (v: number | null) =>
    v == null
      ? EM
      : new Intl.NumberFormat(locale, { style: 'currency', currency: 'TRY', maximumFractionDigits: 2 }).format(v)

  const fmtPct = (v: number | null) =>
    v == null ? EM : `${new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)}%`

  const projection = useMemo(() => {
    const inv = parseAmountLoose(investment)
    const y = parseAmountLoose(yieldRate)
    if (inv == null || y == null) return null
    return computeBondHoldProjection(inv, y, tenor)
  }, [investment, yieldRate, tenor])

  const headlineRows = useMemo(
    () => [
      {
        label: t('faizVadeliPage.simulator.bond.yieldUsed'),
        value: parseAmountLoose(yieldRate) == null ? EM : fmtPct(parseAmountLoose(yieldRate)),
      },
      {
        label: t('faizVadeliPage.simulator.bond.pricePer100'),
        value: projection ? fmtTry(projection.pricePer100) : EM,
      },
      {
        label: t('faizVadeliPage.simulator.bond.maturityProceeds'),
        value: projection ? fmtTry(projection.maturityProceeds) : EM,
      },
      {
        label: t('faizVadeliPage.simulator.bond.totalReturn'),
        value: projection ? fmtTry(projection.totalReturn) : EM,
      },
    ],
    [yieldRate, projection, locale, t],
  )

  const years = tenorYearsFromCode(tenor)

  return (
    <FaizVadeliSimulatorCard title={t('faizVadeliPage.simulator.bond.title')}>
      <SimulatorMetrics rows={headlineRows} />
      <SimulatorDisclaimer>{t('faizVadeliPage.simulator.bond.headlineDisclaimer')}</SimulatorDisclaimer>
      <SimulatorDivider />
      <SimulatorSectionTitle>{t('faizVadeliPage.simulator.bond.scenarioTitle')}</SimulatorSectionTitle>
      <SimulatorLabelField label={t('faizVadeliPage.simulator.bond.investment')}>
        <SimulatorInput value={investment} onChange={setInvestment} inputMode="decimal" />
      </SimulatorLabelField>
      <SimulatorHint>{t('faizVadeliPage.simulator.bond.investmentHint')}</SimulatorHint>
      <SimulatorLabelField label={t('faizVadeliPage.simulator.bond.annualYield')}>
        <SimulatorInput value={yieldRate} onChange={setYieldRate} inputMode="decimal" />
      </SimulatorLabelField>
      <SimulatorHint>
        {t('faizVadeliPage.simulator.bond.tenorHint', { years, tenor })}
      </SimulatorHint>
      <SimulatorMetrics
        rows={[
          {
            label: t('faizVadeliPage.simulator.bond.returnPct'),
            value: projection ? fmtPct(projection.returnPercent) : EM,
          },
          {
            label: t('faizVadeliPage.simulator.bond.faceValue'),
            value: projection ? fmtTry(projection.faceValue) : EM,
          },
        ]}
      />
      <SimulatorDisclaimer>{t('faizVadeliPage.simulator.bond.scenarioDisclaimer')}</SimulatorDisclaimer>
    </FaizVadeliSimulatorCard>
  )
}
