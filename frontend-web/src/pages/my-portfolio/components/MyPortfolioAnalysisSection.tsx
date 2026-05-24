import { useTranslation } from 'react-i18next'
import type {
  PortfolioOverview,
  PortfolioTradeFlow,
  PortfolioValueSnapshot,
} from '../../../shared/types/portfolio'
import { AllocationDonut, type AllocationDonutRow } from './AllocationDonut'
import { PnlSplitDonut } from './PnlSplitDonut'
import { PortfolioHistorySparkline } from './PortfolioHistorySparkline'

type DistributionPreview = {
  bar: { key: string; colorClass: string; widthPct: number }[]
}

type Props = {
  selectedPortfolioId: number | null
  overview: PortfolioOverview | null
  valueSnapshots: PortfolioValueSnapshot[]
  tradeFlow: PortfolioTradeFlow | null
  tradeFlowHydrated: boolean
  instrumentDonutRows: AllocationDonutRow[]
  categoryDonutRows: AllocationDonutRow[]
  distribution: DistributionPreview
  hideMoney: boolean
  isDarkTheme: boolean
  locale: string
  moneyFormat: Intl.NumberFormat
  dayChangeFormat: Intl.NumberFormat
  pctFormat: Intl.NumberFormat
  sharePctDisplay: Intl.NumberFormat
  parseTotal: (overview: PortfolioOverview) => {
    totalValue: number
    totalCost: number
    totalPnl: number
    totalPnlPercent: number
    dayOverDay: number
    priorDayValue: number | null
  }
  onOpenAllocation: () => void
}

export function MyPortfolioAnalysisSection({
  selectedPortfolioId,
  overview,
  valueSnapshots,
  tradeFlow,
  tradeFlowHydrated,
  instrumentDonutRows,
  categoryDonutRows,
  distribution,
  hideMoney,
  isDarkTheme,
  locale,
  moneyFormat,
  dayChangeFormat,
  pctFormat,
  sharePctDisplay,
  parseTotal,
  onOpenAllocation,
}: Props) {
  const { t } = useTranslation('portfolio')

  if (selectedPortfolioId == null) {
    return (
      <article className={`card my-portfolio-trade-card my-portfolio-analysis${isDarkTheme ? ' is-dark' : ' is-light'}`}>
        <h2 className="my-portfolio-analysis-title">{t('portfolioAnalysis.title')}</h2>
        <p className="my-portfolio-trade-subtitle">{t('portfolioAnalysis.pickPortfolio')}</p>
      </article>
    )
  }

  const totals = overview ? parseTotal(overview) : null

  return (
    <article className={`card my-portfolio-trade-card my-portfolio-analysis${isDarkTheme ? ' is-dark' : ' is-light'}`}>
      <header className="my-portfolio-analysis-head">
        <div>
          <h2 className="my-portfolio-analysis-title">{t('portfolioAnalysis.title')}</h2>
          <p className="my-portfolio-analysis-lead">{t('portfolioAnalysis.lead')}</p>
        </div>
        <button type="button" className="auth-submit auth-submit-secondary" onClick={onOpenAllocation}>
          {t('portfolioAnalysis.openAllocation')}
        </button>
      </header>

      {!overview ? (
        <p className="my-portfolio-trade-subtitle">…</p>
      ) : (
        <>
          <div className="my-portfolio-analysis-kpis">
            <div className="my-portfolio-analysis-kpi">
              <span>{t('valueTitle')}</span>
              <strong>{hideMoney ? '•••' : moneyFormat.format(totals!.totalValue)}</strong>
            </div>
            <div className="my-portfolio-analysis-kpi">
              <span>{t('profitTitle')}</span>
              <strong className={totals!.totalPnl >= 0 ? 'is-pos' : 'is-neg'}>
                {hideMoney ? '•••' : moneyFormat.format(totals!.totalPnl)}
                {!hideMoney ? ` (${pctFormat.format(totals!.totalPnlPercent)}%)` : null}
              </strong>
            </div>
            <div className="my-portfolio-analysis-kpi">
              <span>{t('sinceYesterday')}</span>
              <strong className={totals!.dayOverDay >= 0 ? 'is-pos' : 'is-neg'}>
                {hideMoney ? '•••' : dayChangeFormat.format(totals!.dayOverDay)}
              </strong>
            </div>
            <div className="my-portfolio-analysis-kpi">
              <span>{t('allocation.cost')}</span>
              <strong>{hideMoney ? '•••' : moneyFormat.format(totals!.totalCost)}</strong>
            </div>
          </div>

          <section className="my-portfolio-analysis-block">
            <h3 className="my-portfolio-analysis-block-title">{t('portfolioAnalysis.valueTrend')}</h3>
            <p className="my-portfolio-analysis-block-hint">{t('portfolioAnalysis.valueTrendHint')}</p>
            <PortfolioHistorySparkline
              variant="value"
              snapshots={valueSnapshots}
              liveTotalValue={totals!.totalValue}
              priorDayValue={totals!.priorDayValue}
              isDark={isDarkTheme}
              locale={locale}
              maskAmounts={hideMoney}
              formatValue={(v) => moneyFormat.format(v)}
              emptyLabel={t('valueChart.empty')}
              chartHeight={200}
            />
          </section>

          <div className="my-portfolio-analysis-dual">
            <section className="my-portfolio-analysis-block my-portfolio-analysis-block--pnl">
              <h3 className="my-portfolio-analysis-block-title">{t('portfolioAnalysis.pnlBreakdown')}</h3>
              <PnlSplitDonut
                items={overview.items}
                totalPnl={totals!.totalPnl}
                totalPnlPercent={totals!.totalPnlPercent}
                currencyFormat={moneyFormat}
                pctFormat={pctFormat}
                sharePctDisplay={sharePctDisplay}
                hideAmounts={hideMoney}
              />
            </section>
            <section className="my-portfolio-analysis-block">
              <h3 className="my-portfolio-analysis-block-title">{t('portfolioAnalysis.dailyFlow')}</h3>
              <p className="my-portfolio-analysis-block-hint">{t('portfolioAnalysis.dailyFlowHint')}</p>
              {!tradeFlowHydrated ? (
                <p className="my-portfolio-trade-subtitle">{t('tradeFlow.loading')}</p>
              ) : (
                <PortfolioHistorySparkline
                  variant="tradeFlow"
                  tradeFlowPoints={tradeFlow?.points ?? []}
                  isDark={isDarkTheme}
                  locale={locale}
                  maskAmounts={hideMoney}
                  formatValue={(v) => moneyFormat.format(v)}
                  emptyLabel={t('tradeFlow.empty')}
                  chartHeight={200}
                />
              )}
            </section>
          </div>

          <section className="my-portfolio-analysis-block">
            <h3 className="my-portfolio-analysis-block-title">{t('portfolioAnalysis.allocation')}</h3>
            {distribution.bar.length === 0 ? (
              <p className="my-portfolio-trade-subtitle">{t('allocation.empty')}</p>
            ) : (
              <div className="my-portfolio-allocation-chart-row">
                <div className="my-portfolio-allocation-dual">
                  <section className="my-portfolio-allocation-panel">
                    <header className="my-portfolio-allocation-panel-head">
                      <h4 className="my-portfolio-allocation-panel-title">{t('allocation.byInstrumentTitle')}</h4>
                    </header>
                    <AllocationDonut
                      rows={instrumentDonutRows}
                      currencyFormat={moneyFormat}
                      sharePctDisplay={sharePctDisplay}
                      hideAmounts={hideMoney}
                      ariaLabel={`${t('allocation.byInstrumentTitle')} — ${t('distributionTitle')}`}
                    />
                  </section>
                  <section className="my-portfolio-allocation-panel">
                    <header className="my-portfolio-allocation-panel-head">
                      <h4 className="my-portfolio-allocation-panel-title">{t('allocation.byCategoryTitle')}</h4>
                    </header>
                    <AllocationDonut
                      rows={categoryDonutRows}
                      currencyFormat={moneyFormat}
                      sharePctDisplay={sharePctDisplay}
                      hideAmounts={hideMoney}
                      ariaLabel={`${t('allocation.byCategoryTitle')} — ${t('distributionTitle')}`}
                    />
                  </section>
                </div>
              </div>
            )}
          </section>
        </>
      )}
    </article>
  )
}
