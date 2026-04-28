import { useEffect, useMemo, useState } from 'react'
import { createPortfolio } from '../../features/portfolio/api/portfolioApi'
import { usePortfolioStore } from '../../app/store'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useTranslation } from 'react-i18next'

export function ExternalPortfolioPage() {
  const { t, i18n } = useTranslation('portfolio')
  useDocumentTitle(t('external.titleDoc'))

  const numberFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.resolvedLanguage ?? i18n.language ?? 'en', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }),
    [i18n.language, i18n.resolvedLanguage],
  )

  const [creating, setCreating] = useState(false)
  const [selectedPortfolioId, setSelectedPortfolioId] = useState<number | null>(null)

  const {
    portfolios,
    summary,
    allocation,
    loading,
    error,
    fetchPortfolios,
    fetchSummary,
    fetchAllocation,
  } = usePortfolioStore()

  useEffect(() => {
    void fetchPortfolios()
  }, [fetchPortfolios])

  const handleCreatePortfolio = async () => {
    const name = window.prompt(t('external.promptName'))
    if (!name?.trim()) {
      return
    }

    setCreating(true)
    try {
      await createPortfolio({ name: name.trim() })
      await fetchPortfolios()
    } finally {
      setCreating(false)
    }
  }

  const handleSelectPortfolio = async (portfolioId: number) => {
    setSelectedPortfolioId(portfolioId)
    await Promise.all([fetchSummary(portfolioId), fetchAllocation(portfolioId)])
  }

  return (
    <section>
      <div className="section-header">
        <h2>{t('external.title')}</h2>
        <button onClick={handleCreatePortfolio} disabled={creating}>
          {creating ? t('external.creating') : t('external.create')}
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}

      <div className="portfolio-grid">
        <article className="card">
          <h3>{t('external.portfolios')}</h3>
          {loading && portfolios.length === 0 ? <p>{t('external.loadingPortfolios')}</p> : null}
          {portfolios.length === 0 ? <p>{t('external.noPortfolios')}</p> : null}
          <ul className="portfolio-list">
            {portfolios.map((portfolio) => (
              <li key={portfolio.id}>
                <button
                  className={`portfolio-item${
                    selectedPortfolioId === portfolio.id ? ' portfolio-item-active' : ''
                  }`}
                  onClick={() => void handleSelectPortfolio(portfolio.id)}
                >
                  <span>{portfolio.name}</span>
                  <small>{portfolio.baseCurrency}</small>
                </button>
              </li>
            ))}
          </ul>
        </article>

        <article className="card">
          <h3>{t('external.summary')}</h3>
          {!summary ? (
            <p>{t('external.summaryHint')}</p>
          ) : (
            <div className="summary">
              <p>
                {t('external.totalCost')}: <strong>{numberFormat.format(summary.totalCost)}</strong>
              </p>
              <p>
                {t('external.marketValue')}: <strong>{numberFormat.format(summary.totalMarketValue)}</strong>
              </p>
              <p>
                {t('external.pnl')}: <strong>{numberFormat.format(summary.totalPnL)}</strong>
              </p>
              <p>
                {t('external.pnlPercent')}:{' '}
                <strong>{numberFormat.format(summary.totalPnLPercentage)}%</strong>
              </p>
            </div>
          )}
        </article>

        <article className="card">
          <h3>{t('external.allocation')}</h3>
          {allocation.length === 0 ? (
            <p>{t('external.allocationHint')}</p>
          ) : (
            <ul className="allocation-list">
              {allocation.map((item) => (
                <li key={item.symbol} className="allocation-item">
                  <span>{item.symbol}</span>
                  <span>{numberFormat.format(item.percentage)}%</span>
                </li>
              ))}
            </ul>
          )}
        </article>
      </div>
    </section>
  )
}

