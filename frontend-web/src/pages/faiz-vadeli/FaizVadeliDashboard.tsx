import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getFaizVadeliDashboardCopy } from './faizVadeliDashboardCopy'
import { FaizVadeliStatCard } from './components/FaizVadeliStatCard'
import { FaizVadeliPolicyRateStatCard } from './components/FaizVadeliPolicyRateStatCard'
import { FaizVadeliPolicyRateChartPanel } from './components/FaizVadeliPolicyRateChartPanel'
import { FaizVadeliRepoStatCard } from './components/FaizVadeliRepoStatCard'
import { FaizVadeliRepoChartPanel } from './components/FaizVadeliRepoChartPanel'
import { FaizVadeliInflationStatCard } from './components/FaizVadeliInflationStatCard'
import { FaizVadeliInflationChartPanel } from './components/FaizVadeliInflationChartPanel'
import type { CpiMetricCode } from './api/cpiApi'
import { FaizVadeliTlDepositStatCard } from './components/FaizVadeliTlDepositStatCard'
import { FaizVadeliTlDepositChartPanel } from './components/FaizVadeliTlDepositChartPanel'
import { FaizVadeliTahvilStatCard } from './components/FaizVadeliTahvilStatCard'
import { FaizVadeliViopDetailPanel } from './components/FaizVadeliViopDetailPanel'
import { FaizVadeliEurobondStatCard } from './components/FaizVadeliEurobondStatCard'
import { FaizVadeliEurobondDetailPanel } from './components/FaizVadeliEurobondDetailPanel'
import { FaizVadeliBondStatCard } from './components/FaizVadeliBondStatCard'
import { FaizVadeliBondDetailPanel } from './components/FaizVadeliBondDetailPanel'
import type { TlDepositMaturityCode } from './lib/tlDepositMaturity'
import { DEFAULT_BOND_TENOR, type BondTenorCode } from './lib/bondTenor'
import { TR_USD_EUROBOND_DEFAULT_ISIN } from './lib/trUsdEurobondIsins'
import type { EurobondInstrumentWire } from './api/eurobondMarketApi'

type MidPanel = 'policy' | 'tl_deposit' | 'tahvil' | 'tr_bond' | 'repo' | 'eurobond' | 'inflation'

export function FaizVadeliDashboard() {
  const { i18n } = useTranslation()
  const [midChart, setMidChart] = useState<MidPanel>('policy')
  const [tlDepositMaturity, setTlDepositMaturity] = useState<TlDepositMaturityCode>('MT04')
  const [bondTenor, setBondTenor] = useState<BondTenorCode>(DEFAULT_BOND_TENOR)
  const [eurobondInstruments, setEurobondInstruments] = useState<EurobondInstrumentWire[]>([])
  const [eurobondIsin, setEurobondIsin] = useState(TR_USD_EUROBOND_DEFAULT_ISIN)
  const [cpiMetric, setCpiMetric] = useState<CpiMetricCode>('YEARLY_PCT')
  const copy = useMemo(() => getFaizVadeliDashboardCopy(i18n.resolvedLanguage ?? i18n.language), [i18n.language, i18n.resolvedLanguage])

  const goPolicy = () => setMidChart('policy')

  return (
    <div className="fi-faiz-dash">
      <div className="fi-faiz-stat-grid fi-faiz-stat-grid--cols-7" role="list">
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
                onShowHistory={() => setMidChart('tahvil')}
              />
            ) : stat.statSlot === 'tr_bond' ? (
              <FaizVadeliBondStatCard
                template={stat}
                tenor={bondTenor}
                onTenorChange={setBondTenor}
                active={midChart === 'tr_bond'}
                onShowHistory={() => setMidChart('tr_bond')}
              />
            ) : stat.statSlot === 'repo' ? (
              <FaizVadeliRepoStatCard
                template={stat}
                active={midChart === 'repo'}
                onShowHistory={() => setMidChart('repo')}
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
          <FaizVadeliViopDetailPanel onBack={goPolicy} />
        ) : midChart === 'tr_bond' ? (
          <FaizVadeliBondDetailPanel tenor={bondTenor} onBack={goPolicy} />
        ) : midChart === 'repo' ? (
          <FaizVadeliRepoChartPanel onBack={goPolicy} />
        ) : midChart === 'eurobond' ? (
          <FaizVadeliEurobondDetailPanel
            instruments={eurobondInstruments}
            selectedIsin={eurobondIsin}
            onSelectIsin={setEurobondIsin}
            onBack={goPolicy}
          />
        ) : (
          <FaizVadeliPolicyRateChartPanel />
        )}
      </div>
    </div>
  )
}
