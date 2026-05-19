import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { fetchCpiLatest } from '../../faiz-vadeli/api/cpiApi'
import { fetchTlDepositLatest } from '../../faiz-vadeli/api/tlDepositApi'
import { TL_DEPOSIT_MATURITY_CODES, type TlDepositMaturityCode } from '../../faiz-vadeli/lib/tlDepositMaturity'

function formatPercent(value: number, locale: string): string {
  const nf = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return `%${nf.format(value)}`
}

function formatDelta(value: number, locale: string): string {
  const nf = new Intl.NumberFormat(locale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
    signDisplay: 'exceptZero',
  })
  return nf.format(value)
}

function formatMonthLabel(iso: string | null | undefined, locale: string): string {
  if (!iso) {
    return ''
  }
  const d = new Date(`${iso}T12:00:00`)
  if (Number.isNaN(d.getTime())) {
    return iso
  }
  return new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' }).format(d)
}

type StatRow = {
  value: string
  sub: string
  delta?: string
  deltaTone?: 'positive' | 'negative' | 'neutral'
}

const EMPTY_ROW: StatRow = { value: '', sub: '' }

export function AnalysisInflationDepositCard() {
  const { t, i18n } = useTranslation(['analysis', 'common'])
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [inflationLoading, setInflationLoading] = useState(true)
  const [depositLoading, setDepositLoading] = useState(true)
  const [inflation, setInflation] = useState<StatRow>(EMPTY_ROW)
  const [deposit, setDeposit] = useState<StatRow>(EMPTY_ROW)
  const [maturity, setMaturity] = useState<TlDepositMaturityCode>('MT01')

  useEffect(() => {
    let cancelled = false
    setInflationLoading(true)
    setInflation(EMPTY_ROW)

    fetchCpiLatest('YEARLY_PCT')
      .then((d) => {
        if (cancelled) {
          return
        }
        const raw = d.value
        const num = raw == null ? NaN : typeof raw === 'number' ? raw : Number(raw)
        if (raw == null || Number.isNaN(num)) {
          setInflation({
            value: t('common:faizVadeliPage.inflation.liveNoData'),
            sub: t('common:faizVadeliPage.inflation.liveNoDataHint'),
          })
          return
        }
        const deltaRaw = d.deltaVsPriorMonth
        const deltaNum =
          deltaRaw == null ? NaN : typeof deltaRaw === 'number' ? deltaRaw : Number(deltaRaw)
        const deltaStr = !Number.isNaN(deltaNum) ? formatDelta(deltaNum, locale) : undefined
        const deltaTone =
          deltaStr == null ? undefined : deltaNum > 0 ? 'negative' : deltaNum < 0 ? 'positive' : 'neutral'
        setInflation({
          value: formatPercent(num, locale),
          sub: formatMonthLabel(d.observationMonth, locale),
          delta: deltaStr,
          deltaTone,
        })
      })
      .catch(() => {
        if (!cancelled) {
          setInflation({
            value: t('common:faizVadeliPage.inflation.liveLoadError'),
            sub: '',
          })
        }
      })
      .finally(() => {
        if (!cancelled) {
          setInflationLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [locale, t])

  useEffect(() => {
    let cancelled = false
    setDepositLoading(true)
    setDeposit(EMPTY_ROW)

    fetchTlDepositLatest(maturity)
      .then((d) => {
        if (cancelled) {
          return
        }
        const raw = d.value
        const num = raw == null ? NaN : typeof raw === 'number' ? raw : Number(raw)
        if (raw == null || Number.isNaN(num)) {
          setDeposit({
            value: t('common:faizVadeliPage.tlDeposit.liveNoData'),
            sub: t('common:faizVadeliPage.tlDeposit.liveNoDataHint'),
          })
          return
        }
        setDeposit({
          value: formatPercent(num, locale),
          sub: d.asOfDate ?? '',
        })
      })
      .catch(() => {
        if (!cancelled) {
          setDeposit({
            value: t('common:faizVadeliPage.tlDeposit.liveLoadError'),
            sub: '',
          })
        }
      })
      .finally(() => {
        if (!cancelled) {
          setDepositLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [locale, maturity, t])

  return (
    <article className="card fi-analysis-inflation-deposit-card">
      <div className="fi-panel-head">
        <h3>{t('analysis:inflationDeposit.title')}</h3>
        <small>{t('analysis:inflationDeposit.subtitle')}</small>
      </div>

      <div className="fi-analysis-id-rows">
        <section className="fi-analysis-id-row" aria-busy={inflationLoading}>
          <div className="fi-analysis-id-row-head">
            <span className="fi-analysis-id-row-label">{t('analysis:inflationDeposit.inflationLabel')}</span>
          </div>
          <div className="fi-analysis-id-row-body">
            {inflationLoading ? (
              <span className="fi-analysis-id-skeleton" aria-hidden />
            ) : (
              <>
                <span className="fi-analysis-id-value">{inflation.value}</span>
                {inflation.delta ? (
                  <span className={`fi-analysis-id-delta fi-analysis-id-delta--${inflation.deltaTone ?? 'neutral'}`}>
                    {inflation.delta}
                  </span>
                ) : null}
              </>
            )}
          </div>
          {!inflationLoading && inflation.sub ? (
            <p className="fi-analysis-id-row-sub">{inflation.sub}</p>
          ) : null}
        </section>

        <section className="fi-analysis-id-row" aria-busy={depositLoading}>
          <div className="fi-analysis-id-row-head">
            <span className="fi-analysis-id-row-label">{t('analysis:inflationDeposit.depositLabel')}</span>
            <select
              className="fi-analysis-id-maturity"
              value={maturity}
              aria-label={t('common:faizVadeliPage.tlDeposit.maturitySelectAria')}
              onChange={(e) => {
                const v = e.target.value
                if (TL_DEPOSIT_MATURITY_CODES.includes(v as TlDepositMaturityCode)) {
                  setMaturity(v as TlDepositMaturityCode)
                }
              }}
            >
              {TL_DEPOSIT_MATURITY_CODES.map((code) => (
                <option key={code} value={code}>
                  {t(`common:faizVadeliPage.tlDeposit.maturityLong.${code}`)}
                </option>
              ))}
            </select>
          </div>
          <div className="fi-analysis-id-row-body">
            {depositLoading ? (
              <span className="fi-analysis-id-skeleton" aria-hidden />
            ) : (
              <span className="fi-analysis-id-value">{deposit.value}</span>
            )}
          </div>
          {!depositLoading && deposit.sub ? (
            <p className="fi-analysis-id-row-sub">{deposit.sub}</p>
          ) : null}
        </section>
      </div>

      <p className="fi-analysis-id-foot">
        <Link className="fi-analysis-id-link" to="/faiz-vadeli">
          {t('analysis:inflationDeposit.viewDetails')}
        </Link>
      </p>
    </article>
  )
}
