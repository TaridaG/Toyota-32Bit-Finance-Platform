import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchMarketPricesSummary } from '../../markets/api/marketService'
import { createAlarm } from '../api/alarmApi'
import { notifyAlarmsChanged } from '../api/alarmApi'
import { useAlarmUi } from '../AlarmUiContext'
import {
  formatPercentInput,
  formatPriceInput,
  parseDecimalInput,
  percentFromPrices,
  resolvePriceAlarmCondition,
  targetFromPercent,
} from '../lib/alarmPricing'

const QUICK_OFFSETS = [-10, -5, 5, 10] as const

export function CreateAlarmModal() {
  const { t } = useTranslation('markets')
  const { createTarget, closeCreateAlarm, bumpAlarmsRefresh } = useAlarmUi()
  const [currentPrice, setCurrentPrice] = useState<number | null>(null)
  const [priceLoading, setPriceLoading] = useState(false)
  const [targetPrice, setTargetPrice] = useState('')
  const [changePercent, setChangePercent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!createTarget) {
      return
    }
    setError(null)
    setTargetPrice('')
    setChangePercent('')

    const seed = createTarget.currentPrice
    if (seed != null && Number.isFinite(seed) && seed > 0) {
      setCurrentPrice(seed)
    } else {
      setCurrentPrice(null)
    }

    setPriceLoading(true)
    void fetchMarketPricesSummary([createTarget.symbol])
      .then((summary) => {
        const live = summary[createTarget.symbol]?.price
        if (live != null && Number.isFinite(live) && live > 0) {
          setCurrentPrice(live)
        }
      })
      .finally(() => setPriceLoading(false))
  }, [createTarget])

  const currentLabel = useMemo(() => {
    if (priceLoading && currentPrice == null) {
      return t('alarms.modal.priceLoading')
    }
    if (currentPrice == null || currentPrice <= 0) {
      return '—'
    }
    return formatPriceInput(currentPrice)
  }, [currentPrice, priceLoading, t])

  if (!createTarget) {
    return null
  }

  const applyTarget = (target: number) => {
    if (currentPrice == null || currentPrice <= 0) {
      return
    }
    setTargetPrice(formatPriceInput(target))
    const pct = percentFromPrices(currentPrice, target)
    setChangePercent(pct == null ? '' : formatPercentInput(pct))
  }

  const onTargetPriceChange = (raw: string) => {
    setTargetPrice(raw)
    const target = parseDecimalInput(raw)
    const pct =
      currentPrice != null && currentPrice > 0 && target != null
        ? percentFromPrices(currentPrice, target)
        : null
    setChangePercent(pct == null ? '' : formatPercentInput(pct))
  }

  const onPercentChange = (raw: string) => {
    setChangePercent(raw)
    const pct = parseDecimalInput(raw)
    const target =
      currentPrice != null && currentPrice > 0 && pct != null
        ? targetFromPercent(currentPrice, pct)
        : null
    setTargetPrice(target == null ? '' : formatPriceInput(target))
  }

  const onQuickOffset = (offset: number) => {
    if (currentPrice == null || currentPrice <= 0) {
      return
    }
    const target = targetFromPercent(currentPrice, offset)
    if (target == null) {
      return
    }
    applyTarget(target)
  }

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const target = parseDecimalInput(targetPrice)
    if (currentPrice == null || currentPrice <= 0) {
      setError(t('alarms.errors.noCurrentPrice'))
      return
    }
    if (target == null || target <= 0) {
      setError(t('alarms.errors.invalidThreshold'))
      return
    }
    if (target === currentPrice) {
      setError(t('alarms.errors.sameAsCurrent'))
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      await createAlarm({
        instrumentId: createTarget.instrumentId,
        condition: resolvePriceAlarmCondition(currentPrice, target),
        threshold: target,
      })
      bumpAlarmsRefresh()
      notifyAlarmsChanged()
      closeCreateAlarm()
    } catch {
      setError(t('alarms.errors.createFailed'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="my-portfolio-modal-overlay" role="presentation" onMouseDown={closeCreateAlarm}>
      <div
        className="my-portfolio-modal card portal-alarm-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="portal-alarm-modal-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <header className="portal-alarm-modal-header">
          <h2 id="portal-alarm-modal-title">{t('alarms.modal.title', { symbol: createTarget.symbol })}</h2>
          {createTarget.name ? <p className="portal-alarm-modal-sub">{createTarget.name}</p> : null}
          <p className="portal-alarm-modal-current">
            <span>{t('alarms.modal.currentPrice')}</span>
            <strong>{currentLabel}</strong>
          </p>
        </header>

        <form className="portal-alarm-modal-form" onSubmit={(e) => void onSubmit(e)}>
          <div className="portal-alarm-quick-row">
            <span className="portal-alarm-quick-label">{t('alarms.modal.quick')}</span>
            <div className="portal-alarm-quick-buttons">
              {QUICK_OFFSETS.map((offset) => (
                <button
                  key={offset}
                  type="button"
                  className="portal-alarm-quick-btn"
                  disabled={currentPrice == null || currentPrice <= 0 || submitting}
                  onClick={() => onQuickOffset(offset)}
                >
                  {offset > 0 ? `+${offset}%` : `${offset}%`}
                </button>
              ))}
            </div>
          </div>

          <div className="portal-alarm-dual-inputs">
            <label className="portal-alarm-modal-threshold">
              <span>{t('alarms.modal.targetPrice')}</span>
              <input
                type="text"
                inputMode="decimal"
                value={targetPrice}
                onChange={(e) => onTargetPriceChange(e.target.value)}
                placeholder={currentPrice != null ? formatPriceInput(currentPrice) : '0'}
                required
              />
            </label>
            <label className="portal-alarm-modal-threshold">
              <span>{t('alarms.modal.changePercent')}</span>
              <input
                type="text"
                inputMode="decimal"
                value={changePercent}
                onChange={(e) => onPercentChange(e.target.value)}
                placeholder="0"
              />
            </label>
          </div>

          <p className="portal-alarm-modal-hint">{t('alarms.modal.hint')}</p>

          {error ? <p className="portal-alarm-modal-error">{error}</p> : null}

          <div className="portal-alarm-modal-actions">
            <button type="button" className="markets-filter" onClick={closeCreateAlarm} disabled={submitting}>
              {t('alarms.modal.cancel')}
            </button>
            <button type="submit" className="markets-filter markets-filter-active" disabled={submitting}>
              {submitting ? t('alarms.modal.saving') : t('alarms.modal.save')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
