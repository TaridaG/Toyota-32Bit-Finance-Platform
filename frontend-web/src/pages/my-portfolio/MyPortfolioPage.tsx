import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'

type PortfolioAsset = {
  symbol: string
  sharePercent: number
  value: number
  changePercent: number
  colorClass: string
}

const topGainers = [
  { symbol: 'AAPL', price: 120, change: 12.04 },
  { symbol: 'AIRBNB', price: 120, change: 12.04 },
  { symbol: 'NVDA', price: 120, change: 12.04 },
  { symbol: 'AMZN', price: 120, change: 12.04 },
  { symbol: 'SPTP', price: 120, change: 12.04 },
]

const assets: PortfolioAsset[] = [
  { symbol: 'AAPL', sharePercent: 40, value: 7518, changePercent: 12.04, colorClass: 'my-portfolio-dot-blue' },
  { symbol: 'AIRBNB', sharePercent: 29, value: 5102, changePercent: 1.8, colorClass: 'my-portfolio-dot-purple' },
  { symbol: 'NVDA', sharePercent: 17, value: 3916, changePercent: -2.3, colorClass: 'my-portfolio-dot-pink' },
  { symbol: 'AMZN', sharePercent: 14, value: 2518, changePercent: 7.01, colorClass: 'my-portfolio-dot-green' },
]

const marketInsights = [
  {
    titleKey: 'insights.items.tesla.title',
    detailKey: 'insights.items.tesla.detail',
    thumb: '🚗',
  },
  {
    titleKey: 'insights.items.apple.title',
    detailKey: 'insights.items.apple.detail',
    thumb: '📱',
  },
  {
    titleKey: 'insights.items.nvidia.title',
    detailKey: 'insights.items.nvidia.detail',
    thumb: '🧠',
  },
  {
    titleKey: 'insights.items.banking.title',
    detailKey: 'insights.items.banking.detail',
    thumb: '🏦',
  },
]

const sidebarMainKeys = ['dashboard', 'markets', 'portfolio'] as const
const sidebarSecondaryKeys = ['news', 'analysis', 'targets', 'watchlist', 'settings'] as const
const portfolioOptions = ['Core Portfolio', 'Growth Portfolio', 'Dividend Portfolio'] as const

function SidebarItemIcon({ item }: { item: string }) {
  switch (item) {
    case 'dashboard':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <rect x="3" y="3" width="8" height="8" rx="2" />
          <rect x="13" y="3" width="8" height="5" rx="2" />
          <rect x="13" y="10" width="8" height="11" rx="2" />
          <rect x="3" y="13" width="8" height="8" rx="2" />
        </svg>
      )
    case 'markets':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M4 18h16" />
          <path d="M6 15l3-4 3 2 4-6 2 2" />
          <circle cx="6" cy="15" r="1.2" />
          <circle cx="9" cy="11" r="1.2" />
          <circle cx="12" cy="13" r="1.2" />
          <circle cx="16" cy="7" r="1.2" />
          <circle cx="18" cy="9" r="1.2" />
        </svg>
      )
    case 'portfolio':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <rect x="3" y="6" width="18" height="14" rx="3" />
          <path d="M3 11h18" />
          <path d="M8 3h8" />
        </svg>
      )
    case 'news':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <rect x="4" y="4" width="16" height="16" rx="2" />
          <path d="M8 8h8" />
          <path d="M8 12h8" />
          <path d="M8 16h5" />
        </svg>
      )
    case 'analysis':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M4 19V5" />
          <path d="M4 19h16" />
          <rect x="7" y="12" width="3" height="5" rx="1" />
          <rect x="12" y="9" width="3" height="8" rx="1" />
          <rect x="17" y="6" width="3" height="11" rx="1" />
        </svg>
      )
    case 'targets':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <circle cx="12" cy="12" r="8" />
          <circle cx="12" cy="12" r="4" />
          <circle cx="12" cy="12" r="1.2" />
        </svg>
      )
    case 'watchlist':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M6 4h12v16l-6-3-6 3z" />
        </svg>
      )
    case 'settings':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <circle cx="12" cy="12" r="3.2" />
          <path d="M19.4 15a1 1 0 0 0 .2 1.1l.1.1a1 1 0 0 1 0 1.4l-1 1a1 1 0 0 1-1.4 0l-.1-.1a1 1 0 0 0-1.1-.2 1 1 0 0 0-.6.9V20a1 1 0 0 1-1 1h-1.4a1 1 0 0 1-1-1v-.1a1 1 0 0 0-.6-.9 1 1 0 0 0-1.1.2l-.1.1a1 1 0 0 1-1.4 0l-1-1a1 1 0 0 1 0-1.4l.1-.1a1 1 0 0 0 .2-1.1 1 1 0 0 0-.9-.6H4a1 1 0 0 1-1-1v-1.4a1 1 0 0 1 1-1h.1a1 1 0 0 0 .9-.6 1 1 0 0 0-.2-1.1l-.1-.1a1 1 0 0 1 0-1.4l1-1a1 1 0 0 1 1.4 0l.1.1a1 1 0 0 0 1.1.2 1 1 0 0 0 .6-.9V4a1 1 0 0 1 1-1h1.4a1 1 0 0 1 1 1v.1a1 1 0 0 0 .6.9 1 1 0 0 0 1.1-.2l.1-.1a1 1 0 0 1 1.4 0l1 1a1 1 0 0 1 0 1.4l-.1.1a1 1 0 0 0-.2 1.1 1 1 0 0 0 .9.6H20a1 1 0 0 1 1 1v1.4a1 1 0 0 1-1 1h-.1a1 1 0 0 0-.9.6z" />
        </svg>
      )
    default:
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <circle cx="12" cy="12" r="8" />
        </svg>
      )
  }
}

function MiniLineChart() {
  const points = '0,96 20,70 40,78 60,54 80,46 100,58 120,44 140,36 160,52 180,50 200,34 220,28 240,22 260,26 280,48 300,44'
  return (
    <svg viewBox="0 0 300 110" className="my-portfolio-line-chart" aria-hidden>
      <defs>
        <linearGradient id="portfolioArea" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="rgba(99, 102, 241, 0.28)" />
          <stop offset="100%" stopColor="rgba(99, 102, 241, 0.02)" />
        </linearGradient>
      </defs>
      <path d="M0,110 L0,96 L20,70 L40,78 L60,54 L80,46 L100,58 L120,44 L140,36 L160,52 L180,50 L200,34 L220,28 L240,22 L260,26 L280,48 L300,44 L300,110 Z" />
      <polyline points={points} />
      <circle cx="200" cy="34" r="4" />
    </svg>
  )
}

export function MyPortfolioPage() {
  const { t, i18n } = useTranslation('portfolio')
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [activeSection, setActiveSection] = useState<string>('dashboard')
  const [selectedPortfolio, setSelectedPortfolio] = useState<(typeof portfolioOptions)[number]>(portfolioOptions[0])
  useDocumentTitle(t('titleDoc'))

  const currencyFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 2,
      }),
    [i18n.language],
  )

  const percentFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        maximumFractionDigits: 2,
        signDisplay: 'always',
      }),
    [i18n.language],
  )

  return (
    <section className="my-portfolio-page">
      <div className="my-portfolio-shell">
        <aside className={`my-portfolio-sidebar card${sidebarOpen ? ' my-portfolio-sidebar-open' : ''}`}>
          <div className="my-portfolio-sidebar-top">
            <button
              type="button"
              className="my-portfolio-sidebar-toggle"
              onClick={() => setSidebarOpen((prev) => !prev)}
              aria-label={sidebarOpen ? t('sidebar.collapse') : t('sidebar.expand')}
            >
              <span className="my-portfolio-sidebar-toggle-icon" aria-hidden>
                {sidebarOpen ? '‹' : '›'}
              </span>
            </button>

            <label className="my-portfolio-select-wrap">
              {sidebarOpen ? (
                <>
                  <span>{t('sidebar.portfolios')}</span>
                  <select
                    value={selectedPortfolio}
                    onChange={(event) => setSelectedPortfolio(event.target.value as (typeof portfolioOptions)[number])}
                  >
                    {portfolioOptions.map((portfolioName) => (
                      <option key={portfolioName} value={portfolioName}>
                        {portfolioName}
                      </option>
                    ))}
                  </select>
                </>
              ) : (
                <button type="button" className="my-portfolio-portfolio-icon" aria-label={t('sidebar.portfolios')}>
                  <svg viewBox="0 0 24 24" aria-hidden>
                    <path d="M4 7h16v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z" />
                    <path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                  </svg>
                </button>
              )}
            </label>
          </div>

          <nav className="my-portfolio-sidebar-nav" aria-label={t('sidebar.navAria')}>
            {sidebarMainKeys.map((item) => (
              <button
                key={item}
                type="button"
                className={`my-portfolio-sidebar-item${activeSection === item ? ' my-portfolio-sidebar-item-active' : ''}`}
                onClick={() => setActiveSection(item)}
              >
                <span className="my-portfolio-sidebar-item-icon" aria-hidden>
                  <SidebarItemIcon item={item} />
                </span>
                {sidebarOpen ? <span>{t(`sidebar.items.${item}`)}</span> : null}
              </button>
            ))}
          </nav>

          <nav className="my-portfolio-sidebar-nav my-portfolio-sidebar-nav-secondary" aria-label={t('sidebar.quickAria')}>
            {sidebarSecondaryKeys.map((item) => (
              <button
                key={item}
                type="button"
                className={`my-portfolio-sidebar-item${activeSection === item ? ' my-portfolio-sidebar-item-active' : ''}`}
                onClick={() => setActiveSection(item)}
              >
                <span className="my-portfolio-sidebar-item-icon" aria-hidden>
                  <SidebarItemIcon item={item} />
                </span>
                {sidebarOpen ? <span>{t(`sidebar.items.${item}`)}</span> : null}
              </button>
            ))}
          </nav>

          <button type="button" className="my-portfolio-sidebar-logout">
            <span className="my-portfolio-sidebar-item-icon" aria-hidden>
              <svg viewBox="0 0 24 24" aria-hidden>
                <path d="M10 6H7a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h3" />
                <path d="M14 16l4-4-4-4" />
                <path d="M18 12H9" />
              </svg>
            </span>
            {sidebarOpen ? <span>{t('sidebar.logout')}</span> : null}
          </button>
        </aside>

        <div className="my-portfolio-content">
          <div className="my-portfolio-gainers">
            <span>{t('topGainers')}</span>
            <ul>
              {topGainers.map((item) => (
                <li key={item.symbol}>
                  <strong>{item.symbol}</strong>
                  <span>{currencyFormat.format(item.price)}</span>
                  <small>{percentFormat.format(item.change)}</small>
                </li>
              ))}
            </ul>
          </div>

          <div className="my-portfolio-grid">
            <article className="card my-portfolio-card my-portfolio-card-wide">
              <div className="my-portfolio-card-head">
                <h3>{t('valueTitle')}</h3>
                <button type="button">{t('actions.yearly')}</button>
              </div>
              <p className="my-portfolio-main-value">{currencyFormat.format(134815)}</p>
              <p className="my-portfolio-sub-value">
                + {currencyFormat.format(19698)} {t('fromLastYear')}
              </p>
              <MiniLineChart />
            </article>

            <article className="card my-portfolio-card">
              <div className="my-portfolio-card-head">
                <h3>{t('profitTitle')}</h3>
                <button type="button">{t('actions.yearly')}</button>
              </div>
              <div className="my-portfolio-donut-wrap">
                <div className="my-portfolio-donut">
                  <div>
                    <strong>{currencyFormat.format(8436)}</strong>
                    <span>-{currencyFormat.format(268.2)}</span>
                  </div>
                </div>
              </div>
              <ul className="my-portfolio-legend">
                <li>{t('legend.stocks')}</li>
                <li>{t('legend.funds')}</li>
                <li>{t('legend.bonds')}</li>
                <li>{t('legend.reits')}</li>
              </ul>
            </article>

            <article className="card my-portfolio-card">
              <div className="my-portfolio-card-head">
                <h3>{t('distributionTitle')}</h3>
                <button type="button">{t('actions.viewAll')}</button>
              </div>
              <div className="my-portfolio-distribution-bar">
                {assets.map((asset) => (
                  <span key={asset.symbol} className={asset.colorClass} style={{ width: `${asset.sharePercent}%` }} />
                ))}
              </div>
              <ul className="my-portfolio-asset-list">
                {assets.map((asset) => (
                  <li key={asset.symbol}>
                    <div>
                      <span className={`my-portfolio-dot ${asset.colorClass}`} />
                      <strong>{asset.symbol}</strong>
                      <small>{asset.sharePercent}%</small>
                    </div>
                    <span>{currencyFormat.format(asset.value)}</span>
                  </li>
                ))}
              </ul>
            </article>

            <article className="card my-portfolio-card">
              <div className="my-portfolio-card-head">
                <h3>{t('assetsTitle')}</h3>
                <button type="button">{t('actions.viewAll')}</button>
              </div>
              <ul className="my-portfolio-my-assets">
                {assets.map((asset) => (
                  <li key={asset.symbol}>
                    <strong>{asset.symbol}</strong>
                    <span>{currencyFormat.format(asset.value)}</span>
                    <small className={asset.changePercent >= 0 ? 'my-portfolio-up' : 'my-portfolio-down'}>
                      {percentFormat.format(asset.changePercent)}
                    </small>
                  </li>
                ))}
              </ul>
            </article>

            <article className="card my-portfolio-card">
              <div className="my-portfolio-card-head">
                <h3>{t('insightTitle')}</h3>
                <button type="button">{t('actions.viewAll')}</button>
              </div>
              <ul className="my-portfolio-insight-list">
                {marketInsights.map((item) => (
                  <li key={item.titleKey}>
                    <span>{item.thumb}</span>
                    <div>
                      <strong>{t(item.titleKey)}</strong>
                      <small>{t(item.detailKey)}</small>
                    </div>
                  </li>
                ))}
              </ul>
            </article>
          </div>
        </div>
      </div>
    </section>
  )
}
