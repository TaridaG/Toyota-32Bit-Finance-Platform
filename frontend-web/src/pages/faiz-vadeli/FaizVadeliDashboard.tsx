import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { TabId } from './faizVadeliDashboardCopy'
import { getFaizVadeliDashboardCopy } from './faizVadeliDashboardCopy'
import { FaizVadeliStatCard } from './components/FaizVadeliStatCard'
import { FaizVadeliPolicyRateStatCard } from './components/FaizVadeliPolicyRateStatCard'
import { FaizVadeliPolicyRateChartPanel } from './components/FaizVadeliPolicyRateChartPanel'
import { FaizVadeliInflationStatCard } from './components/FaizVadeliInflationStatCard'
import { FaizVadeliInflationChartPanel } from './components/FaizVadeliInflationChartPanel'
import type { CpiMetricCode } from './api/cpiApi'
import { FaizVadeliTlDepositStatCard } from './components/FaizVadeliTlDepositStatCard'
import { FaizVadeliTlDepositChartPanel } from './components/FaizVadeliTlDepositChartPanel'
import { FaizVadeliTahvilStatCard } from './components/FaizVadeliTahvilStatCard'
import { FaizVadeliTahvilChartPanel } from './components/FaizVadeliTahvilChartPanel'
import { FaizVadeliEurobondStatCard } from './components/FaizVadeliEurobondStatCard'
import { FaizVadeliEurobondDetailPanel } from './components/FaizVadeliEurobondDetailPanel'
import type { TlDepositMaturityCode } from './lib/tlDepositMaturity'
import type { TahvilSymbol } from './lib/tahvilSymbol'
import { TR_USD_EUROBOND_DEFAULT_ISIN } from './lib/trUsdEurobondIsins'
import type { EurobondInstrumentWire } from './api/eurobondMarketApi'

type MidPanel = 'policy' | 'tl_deposit' | 'tahvil' | 'eurobond' | 'inflation'

export function FaizVadeliDashboard() {
  const { i18n } = useTranslation()
  const [tab, setTab] = useState<TabId>('deposit')
  const [midChart, setMidChart] = useState<MidPanel>('policy')
  const [tlDepositMaturity, setTlDepositMaturity] = useState<TlDepositMaturityCode>('MT04')
  const [tahvilSymbol, setTahvilSymbol] = useState<TahvilSymbol>('TRBOND1Y')
  const [eurobondInstruments, setEurobondInstruments] = useState<EurobondInstrumentWire[]>([])
  const [eurobondIsin, setEurobondIsin] = useState(TR_USD_EUROBOND_DEFAULT_ISIN)
  const [cpiMetric, setCpiMetric] = useState<CpiMetricCode>('YEARLY_PCT')
  const copy = useMemo(() => getFaizVadeliDashboardCopy(i18n.resolvedLanguage ?? i18n.language), [i18n.language, i18n.resolvedLanguage])

  const goPolicy = () => setMidChart('policy')

  return (
    <div className="fi-faiz-dash">
      <div className="fi-faiz-stat-grid" role="list">
        {copy.stats.map((stat) => (
          <div key={stat.statSlot ?? stat.title} role="listitem">
            {stat.statSlot === 'policy_rate' ? (
              <FaizVadeliPolicyRateStatCard template={stat} onShowHistory={() => setMidChart('policy')} />
            ) : stat.statSlot === 'tl_deposit' ? (
              <FaizVadeliTlDepositStatCard
                template={stat}
                maturity={tlDepositMaturity}
                onMaturityChange={setTlDepositMaturity}
                onShowHistory={() => setMidChart('tl_deposit')}
              />
            ) : stat.statSlot === 'tahvil' ? (
              <FaizVadeliTahvilStatCard
                template={stat}
                symbol={tahvilSymbol}
                onSymbolChange={setTahvilSymbol}
                onShowHistory={() => setMidChart('tahvil')}
              />
            ) : stat.statSlot === 'eurobond' ? (
              <FaizVadeliEurobondStatCard
                template={stat}
                selectedIsin={eurobondIsin}
                instruments={eurobondInstruments}
                onInstrumentsLoaded={setEurobondInstruments}
                onIsinChange={setEurobondIsin}
                onShowHistory={() => setMidChart('eurobond')}
              />
            ) : stat.statSlot === 'inflation' ? (
              <FaizVadeliInflationStatCard
                template={stat}
                active={midChart === 'inflation'}
                onShowHistory={() => {
                  setCpiMetric('YEARLY_PCT')
                  setMidChart('inflation')
                }}
              />
            ) : (
              <FaizVadeliStatCard stat={stat} />
            )}
          </div>
        ))}
      </div>

      <div className="fi-faiz-mid-grid fi-faiz-mid-grid--solo">
        {midChart === 'policy' ? (
          <FaizVadeliPolicyRateChartPanel />
        ) : midChart === 'inflation' ? (
          <FaizVadeliInflationChartPanel metric={cpiMetric} onMetricChange={setCpiMetric} />
        ) : midChart === 'tl_deposit' ? (
          <FaizVadeliTlDepositChartPanel maturity={tlDepositMaturity} onBack={goPolicy} />
        ) : midChart === 'tahvil' ? (
          <FaizVadeliTahvilChartPanel symbol={tahvilSymbol} onBack={goPolicy} />
        ) : (
          <FaizVadeliEurobondDetailPanel
            instruments={eurobondInstruments}
            selectedIsin={eurobondIsin}
            onSelectIsin={setEurobondIsin}
            onBack={goPolicy}
          />
        )}
      </div>

      <div className="fi-faiz-tabs" role="tablist" aria-label={copy.tabsAria}>
        {copy.tabs.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={tab === t.id}
            className={`fi-faiz-tab${tab === t.id ? ' fi-faiz-tab--active' : ''}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="fi-faiz-tab-placeholder">
        <p>
          <strong>{copy.tabs.find((x) => x.id === tab)?.label}</strong>
          <span className="fi-faiz-tab-placeholder-muted"> — {copy.tabPlaceholder}</span>
        </p>
      </div>
    </div>
  )
}
