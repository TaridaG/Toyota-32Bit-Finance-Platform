import { useEffect, useMemo, useState } from 'react'
import { createPortfolio } from '../../features/portfolio/api/portfolioApi'
import { usePortfolioStore } from '../../app/store'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useTranslation } from 'react-i18next'

export function ExternalPortfolioPage() {
  const { t, i18n } = useTranslation()
  useDocumentTitle(t('portfolio.titleDoc'))

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
    const name = window.prompt(t('portfolio.promptName'))
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
        <h2>{t('portfolio.title')}</h2>
        <button onClick={handleCreatePortfolio} disabled={creating}>
          {creating ? t('portfolio.creating') : t('portfolio.create')}
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}

      <div className="portfolio-grid">
        <article className="card">
          <h3>{t('portfolio.portfolios')}</h3>
          {loading && portfolios.length === 0 ? <p>{t('portfolio.loadingPortfolios')}</p> : null}
          {portfolios.length === 0 ? <p>{t('portfolio.noPortfolios')}</p> : null}
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
          <h3>{t('portfolio.summary')}</h3>
          {!summary ? (
            <p>{t('portfolio.summaryHint')}</p>
          ) : (
            <div className="summary">
              <p>
                {t('portfolio.totalCost')}: <strong>{numberFormat.format(summary.totalCost)}</strong>
              </p>
              <p>
                {t('portfolio.marketValue')}: <strong>{numberFormat.format(summary.totalMarketValue)}</strong>
              </p>
              <p>
                {t('portfolio.pnl')}: <strong>{numberFormat.format(summary.totalPnL)}</strong>
              </p>
              <p>
                {t('portfolio.pnlPercent')}:{' '}
                <strong>{numberFormat.format(summary.totalPnLPercentage)}%</strong>
              </p>
            </div>
          )}
        </article>

        <article className="card">
          <h3>{t('portfolio.allocation')}</h3>
          {allocation.length === 0 ? (
            <p>{t('portfolio.allocationHint')}</p>
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

