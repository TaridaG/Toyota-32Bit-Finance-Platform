import { useEffect, useState } from 'react'
import { createPortfolio } from '../../features/portfolio/api/portfolioApi'
import { usePortfolioStore } from '../../app/store'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'

const numberFormat = new Intl.NumberFormat('tr-TR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export function ExternalPortfolioPage() {
  useDocumentTitle('Dış Portföy | Finans Platformu')

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
    const name = window.prompt('Portföy adı')
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
        <h2>Dış Portföy</h2>
        <button onClick={handleCreatePortfolio} disabled={creating}>
          {creating ? 'Oluşturuluyor...' : 'Portföy Oluştur'}
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}

      <div className="portfolio-grid">
        <article className="card">
          <h3>Portföyler</h3>
          {loading && portfolios.length === 0 ? <p>Portföyler yükleniyor...</p> : null}
          {portfolios.length === 0 ? <p>Portföy bulunamadı.</p> : null}
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
          <h3>Özet</h3>
          {!summary ? (
            <p>Özeti görmek için bir portföy seçin.</p>
          ) : (
            <div className="summary">
              <p>
                Toplam Maliyet: <strong>{numberFormat.format(summary.totalCost)}</strong>
              </p>
              <p>
                Piyasa Değeri: <strong>{numberFormat.format(summary.totalMarketValue)}</strong>
              </p>
              <p>
                Kar/Zarar: <strong>{numberFormat.format(summary.totalPnL)}</strong>
              </p>
              <p>
                Kar/Zarar %: <strong>{numberFormat.format(summary.totalPnLPercentage)}%</strong>
              </p>
            </div>
          )}
        </article>

        <article className="card">
          <h3>Dağılım</h3>
          {allocation.length === 0 ? (
            <p>Dağılımı görmek için bir portföy seçin.</p>
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

