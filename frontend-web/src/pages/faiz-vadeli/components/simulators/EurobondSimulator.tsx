import { forwardRef, useEffect, useImperativeHandle, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  fetchTrEurobondCashflow,
  toNum,
  type EurobondCashflowWire,
  type EurobondHistoryPointWire,
  type EurobondInstrumentWire,
} from '../../api/eurobondMarketApi'
import { computeEurobondPastPnl } from '../../lib/eurobondPastPnl'
import {
  FaizVadeliSimulatorCard,
  SimulatorDisclaimer,
  SimulatorDivider,
  SimulatorHint,
  SimulatorInput,
  SimulatorLabelField,
  SimulatorMetrics,
  SimulatorPickButton,
  SimulatorSectionTitle,
} from '../FaizVadeliSimulatorCard'

const EM = '—'

export type EurobondChartPickMode =
  | 'purchaseDate'
  | 'purchasePrice'
  | 'saleDate'
  | 'salePrice'
  | 'maturityDate'
  | null

function findHistPoint(points: EurobondHistoryPointWire[], isoDay: string): EurobondHistoryPointWire | undefined {
  return points.find((p) => {
    const d = p.date
    if (!d) return false
    return (d.length >= 10 ? d.slice(0, 10) : d) === isoDay
  })
}

export type EurobondSimulatorHandle = {
  handleChartPick: (iso: string) => void
}

export const EurobondSimulator = forwardRef<
  EurobondSimulatorHandle,
  {
    selectedIsin: string
    instruments: EurobondInstrumentWire[]
    histPoints: EurobondHistoryPointWire[]
    pickMode: EurobondChartPickMode
    onPickModeChange: (mode: EurobondChartPickMode) => void
  }
>(function EurobondSimulator(
  { selectedIsin, instruments, histPoints, pickMode, onPickModeChange },
  ref,
) {
  const { t, i18n } = useTranslation('common')
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [nominal, setNominal] = useState('10000')
  const [cf, setCf] = useState<EurobondCashflowWire | null>(null)
  const [cfErr, setCfErr] = useState(false)
  const [purchaseDate, setPurchaseDate] = useState('')
  const [purchasePrice, setPurchasePrice] = useState('')
  const [saleUseLast, setSaleUseLast] = useState(true)
  const [saleDateStr, setSaleDateStr] = useState('')
  const [salePriceStr, setSalePriceStr] = useState('')
  const [maturityDateStr, setMaturityDateStr] = useState('')

  const selected = useMemo(
    () => instruments.find((x) => (x.isin ?? '').toUpperCase() === selectedIsin.toUpperCase()),
    [instruments, selectedIsin],
  )

  useEffect(() => {
    const n = Number(String(nominal).replace(/\s/g, '').replace(',', '.'))
    if (!Number.isFinite(n) || n <= 0) {
      setCf(null)
      setCfErr(false)
      return
    }
    const tmr = window.setTimeout(() => {
      fetchTrEurobondCashflow(selectedIsin, n)
        .then((r) => {
          setCf(r)
          setCfErr(false)
        })
        .catch(() => {
          setCf(null)
          setCfErr(true)
        })
    }, 400)
    return () => window.clearTimeout(tmr)
  }, [nominal, selectedIsin])

  useEffect(() => {
    const mat = instruments.find((x) => (x.isin ?? '').toUpperCase() === selectedIsin.toUpperCase())?.maturityDate
    setPurchaseDate('')
    setPurchasePrice('')
    setSaleUseLast(true)
    setSaleDateStr('')
    setSalePriceStr('')
    setMaturityDateStr((mat ?? '').slice(0, 10))
    onPickModeChange(null)
  }, [selectedIsin, instruments, onPickModeChange])

  useEffect(() => {
    if (!saleUseLast || histPoints.length === 0) return
    const sorted = [...histPoints]
      .filter((x) => x.date)
      .sort((a, b) => String(a.date).localeCompare(String(b.date)))
    const last = sorted[sorted.length - 1]
    const d = last?.date ? String(last.date).slice(0, 10) : ''
    if (d) setSaleDateStr(d)
    const px = toNum(last?.closePrice)
    if (px != null) setSalePriceStr(String(Math.round(px * 100) / 100))
  }, [histPoints, saleUseLast])

  const fmtPct = (v: number | null) =>
    v == null ? EM : `${new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)}%`
  const fmtUsd = (v: number | null) =>
    v == null
      ? EM
      : new Intl.NumberFormat(locale, { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(v)

  useImperativeHandle(
    ref,
    () => ({
      handleChartPick: (iso: string) => {
        applyEurobondChartPick(pickMode, iso, histPoints, {
          setPurchaseDate,
          setPurchasePrice,
          setSaleDateStr,
          setSalePriceStr,
          setMaturityDateStr,
        })
        onPickModeChange(null)
      },
    }),
    [pickMode, histPoints, onPickModeChange],
  )

  const pastPnl = useMemo(
    () =>
      computeEurobondPastPnl({
        nominal,
        purchaseDate,
        purchasePrice,
        saleUseLast,
        saleDateStr,
        salePriceStr,
        maturityDateStr,
        histPoints,
        selected,
      }),
    [
      nominal,
      purchaseDate,
      purchasePrice,
      saleUseLast,
      saleDateStr,
      salePriceStr,
      maturityDateStr,
      histPoints,
      selected,
    ],
  )

  const couponRows = useMemo(
    () => [
      { label: t('faizVadeliPage.eurobond.simAnnual'), value: cfErr ? EM : fmtUsd(toNum(cf?.annualCouponUsd)) },
      { label: t('faizVadeliPage.eurobond.simSemi'), value: cfErr ? EM : fmtUsd(toNum(cf?.semiAnnualCouponUsd)) },
      { label: t('faizVadeliPage.eurobond.simPurchase'), value: cfErr ? EM : fmtUsd(toNum(cf?.approximatePurchaseAmountUsd)) },
      { label: t('faizVadeliPage.eurobond.simPrincipal'), value: cfErr ? EM : fmtUsd(toNum(cf?.maturityPrincipalUsd)) },
    ],
    [cf, cfErr, locale, t],
  )

  const pastRows = useMemo(
    () => [
      { label: t('faizVadeliPage.eurobond.pastCost'), value: pastPnl ? fmtUsd(pastPnl.cost) : EM },
      { label: t('faizVadeliPage.eurobond.pastProceeds'), value: pastPnl ? fmtUsd(pastPnl.proceeds) : EM },
      { label: t('faizVadeliPage.eurobond.pastCoupons'), value: pastPnl ? fmtUsd(pastPnl.couponCash) : EM },
      { label: t('faizVadeliPage.eurobond.pastPnl'), value: pastPnl ? fmtUsd(pastPnl.pnl) : EM },
      {
        label: t('faizVadeliPage.eurobond.pastPnlPct'),
        value: pastPnl && pastPnl.pct != null ? fmtPct(pastPnl.pct) : EM,
      },
    ],
    [pastPnl, locale, t],
  )

  return (
    <FaizVadeliSimulatorCard
      title={t('faizVadeliPage.simulator.eurobond.title')}
      footer={
        selected?.lastUpdatedAt ? (
          <p className="fi-faiz-simulator-meta">
            {t('faizVadeliPage.eurobond.lastUpdated')}: {new Date(selected.lastUpdatedAt).toLocaleString(locale)}
          </p>
        ) : null
      }
    >
      <SimulatorLabelField label={t('faizVadeliPage.eurobond.simNominal')}>
        <SimulatorInput inputMode="decimal" value={nominal} onChange={(e) => setNominal(e.target.value)} />
      </SimulatorLabelField>
      <SimulatorHint>{t('faizVadeliPage.eurobond.simNominalSharedHint')}</SimulatorHint>
      <SimulatorMetrics rows={couponRows} />
      <SimulatorDisclaimer>{t('faizVadeliPage.eurobond.simDisclaimer')}</SimulatorDisclaimer>

      <SimulatorDivider />

      <SimulatorSectionTitle>{t('faizVadeliPage.eurobond.pastTitle')}</SimulatorSectionTitle>

      <SimulatorLabelField label={t('faizVadeliPage.eurobond.pastBuyPrice')}>
        <div className="fi-faiz-simulator-field-row">
          <SimulatorInput
            inputMode="decimal"
            value={purchasePrice}
            onChange={(e) => setPurchasePrice(e.target.value)}
            placeholder="100.00"
          />
          <SimulatorPickButton
            title={t('faizVadeliPage.eurobond.pastPickFromChart')}
            ariaLabel={t('faizVadeliPage.eurobond.pastPickFromChart')}
            onClick={() => onPickModeChange('purchasePrice')}
          />
        </div>
      </SimulatorLabelField>

      <SimulatorLabelField label={t('faizVadeliPage.eurobond.pastBuyDate')}>
        <div className="fi-faiz-simulator-field-row">
          <SimulatorInput type="date" value={purchaseDate} onChange={(e) => setPurchaseDate(e.target.value)} />
          <SimulatorPickButton
            title={t('faizVadeliPage.eurobond.pastPickFromChart')}
            ariaLabel={t('faizVadeliPage.eurobond.pastPickFromChart')}
            onClick={() => onPickModeChange('purchaseDate')}
          />
        </div>
      </SimulatorLabelField>

      <label className="fi-faiz-simulator-inline-check">
        <input
          type="checkbox"
          checked={saleUseLast}
          onChange={(e) => {
            setSaleUseLast(e.target.checked)
            if (e.target.checked) setSalePriceStr('')
          }}
        />
        <span>{t('faizVadeliPage.eurobond.pastSaleUseLast')}</span>
      </label>

      {!saleUseLast ? (
        <>
          <SimulatorLabelField label={t('faizVadeliPage.eurobond.pastSaleDate')}>
            <div className="fi-faiz-simulator-field-row">
              <SimulatorInput type="date" value={saleDateStr} onChange={(e) => setSaleDateStr(e.target.value)} />
              <SimulatorPickButton
                title={t('faizVadeliPage.eurobond.pastPickFromChart')}
                ariaLabel={t('faizVadeliPage.eurobond.pastPickFromChart')}
                onClick={() => onPickModeChange('saleDate')}
              />
            </div>
          </SimulatorLabelField>
          <SimulatorLabelField label={t('faizVadeliPage.eurobond.pastSalePrice')}>
            <div className="fi-faiz-simulator-field-row">
              <SimulatorInput
                inputMode="decimal"
                value={salePriceStr}
                onChange={(e) => setSalePriceStr(e.target.value)}
                placeholder={t('faizVadeliPage.eurobond.pastSalePricePlaceholder')}
              />
              <SimulatorPickButton
                title={t('faizVadeliPage.eurobond.pastPickFromChart')}
                ariaLabel={t('faizVadeliPage.eurobond.pastPickFromChart')}
                onClick={() => onPickModeChange('salePrice')}
              />
            </div>
          </SimulatorLabelField>
        </>
      ) : null}

      <SimulatorLabelField label={t('faizVadeliPage.eurobond.pastMaturityRef')}>
        <div className="fi-faiz-simulator-field-row">
          <SimulatorInput type="date" value={maturityDateStr} onChange={(e) => setMaturityDateStr(e.target.value)} />
          <SimulatorPickButton
            title={t('faizVadeliPage.eurobond.pastPickFromChart')}
            ariaLabel={t('faizVadeliPage.eurobond.pastPickFromChart')}
            onClick={() => onPickModeChange('maturityDate')}
          />
        </div>
      </SimulatorLabelField>

      <SimulatorMetrics rows={pastRows} />
      <SimulatorDisclaimer>{t('faizVadeliPage.eurobond.pastDisclaimer')}</SimulatorDisclaimer>
    </FaizVadeliSimulatorCard>
  )
})

export function applyEurobondChartPick(
  mode: EurobondChartPickMode,
  iso: string,
  histPoints: EurobondHistoryPointWire[],
  handlers: {
    setPurchaseDate: (v: string) => void
    setPurchasePrice: (v: string) => void
    setSaleDateStr: (v: string) => void
    setSalePriceStr: (v: string) => void
    setMaturityDateStr: (v: string) => void
  },
) {
  if (!mode) return
  if (mode === 'purchaseDate') handlers.setPurchaseDate(iso)
  if (mode === 'saleDate') handlers.setSaleDateStr(iso)
  if (mode === 'maturityDate') handlers.setMaturityDateStr(iso)
  if (mode === 'purchasePrice') {
    const pt = findHistPoint(histPoints, iso)
    const px = toNum(pt?.closePrice)
    if (px != null) handlers.setPurchasePrice(String(Math.round(px * 100) / 100))
    handlers.setPurchaseDate(iso)
  }
  if (mode === 'salePrice') {
    const pt = findHistPoint(histPoints, iso)
    const px = toNum(pt?.closePrice)
    if (px != null) handlers.setSalePriceStr(String(Math.round(px * 100) / 100))
    handlers.setSaleDateStr(iso)
  }
}
