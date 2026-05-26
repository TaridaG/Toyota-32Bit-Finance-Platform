import { useTranslation } from 'react-i18next'

const HOLDING_VALUES = [
  { symbol: 'THYAO', weight: '28%', change: '+3.2%', barWidth: '72%' },
  { symbol: 'XAUTRY', weight: '24%', change: '+1.4%', barWidth: '58%' },
  { symbol: 'US10Y', weight: '18%', change: '+0.8%', barWidth: '44%' },
] as const

const METRIC_VALUES = ['₺1.284M', '+₺24.800', '12']

export function PortfolioShowcasePreview() {
  const { t } = useTranslation('landing')

  const navItems = [
    t('finalCta.preview.nav.overview'),
    t('finalCta.preview.nav.markets'),
    t('finalCta.preview.nav.allocation'),
    t('finalCta.preview.nav.goals'),
  ]

  const metricLabels = [
    t('finalCta.preview.stats.value'),
    t('finalCta.preview.stats.dailyChange'),
    t('finalCta.preview.stats.positions'),
  ]

  const assetLabels = [
    t('finalCta.preview.assets.airlines'),
    t('finalCta.preview.assets.gold'),
    t('finalCta.preview.assets.eurobond'),
  ]

  return (
    <div className="portfolio-preview" aria-hidden="true">
      <div className="portfolio-preview-shell">
        <aside className="portfolio-preview-sidebar">
          <div className="portfolio-preview-logo">
            <span />
            <strong>{t('finalCta.preview.sidebarTitle')}</strong>
          </div>

          <div className="portfolio-preview-portfolio-pill">{t('finalCta.preview.portfolioName')}</div>

          <nav className="portfolio-preview-nav">
            {navItems.map((item, index) => (
              <div key={item} className={`portfolio-preview-nav-item${index === 0 ? ' is-active' : ''}`}>
                {item}
              </div>
            ))}
          </nav>
        </aside>

        <div className="portfolio-preview-main">
          <div className="portfolio-preview-head">
            <div>
              <h3>{t('finalCta.preview.heading')}</h3>
              <p>{t('finalCta.preview.subheading')}</p>
            </div>
            <span className="portfolio-preview-status">{t('finalCta.preview.status')}</span>
          </div>

          <div className="portfolio-preview-metrics">
            {metricLabels.map((label, index) => (
              <div key={label} className="portfolio-preview-metric">
                <strong>{METRIC_VALUES[index]}</strong>
                <span>{label}</span>
              </div>
            ))}
          </div>

          <div className="portfolio-preview-panels">
            <section className="portfolio-preview-chart-card">
              <div className="portfolio-preview-panel-head">
                <h4>{t('finalCta.preview.panels.performance')}</h4>
                <span>30G</span>
              </div>
              <svg viewBox="0 0 320 150" className="portfolio-preview-chart" role="presentation">
                <defs>
                  <linearGradient id="portfolioPreviewLine" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="rgba(96, 165, 250, 0.95)" />
                    <stop offset="100%" stopColor="rgba(59, 130, 246, 0.24)" />
                  </linearGradient>
                </defs>
                <path
                  className="portfolio-preview-chart-area"
                  d="M0 118C18 112 32 106 49 97C66 88 83 80 101 84C118 88 135 97 152 92C170 86 187 70 204 63C222 56 238 58 255 45C273 31 292 20 320 14V150H0Z"
                />
                <path
                  className="portfolio-preview-chart-line"
                  d="M0 118C18 112 32 106 49 97C66 88 83 80 101 84C118 88 135 97 152 92C170 86 187 70 204 63C222 56 238 58 255 45C273 31 292 20 320 14"
                />
                <circle cx="255" cy="45" r="5.5" className="portfolio-preview-chart-dot-glow" />
                <circle cx="255" cy="45" r="3.4" className="portfolio-preview-chart-dot" />
              </svg>
            </section>

            <section className="portfolio-preview-holdings-card">
              <div className="portfolio-preview-panel-head">
                <h4>{t('finalCta.preview.panels.holdings')}</h4>
                <span>{t('finalCta.preview.positionsCount')}</span>
              </div>

              <div className="portfolio-preview-holdings">
                {HOLDING_VALUES.map((holding, index) => (
                  <article key={holding.symbol} className="portfolio-preview-holding">
                    <div className="portfolio-preview-holding-head">
                      <div>
                        <strong>{holding.symbol}</strong>
                        <p>{assetLabels[index]}</p>
                      </div>
                      <div className="portfolio-preview-holding-meta">
                        <span>{holding.weight}</span>
                        <em>{holding.change}</em>
                      </div>
                    </div>
                    <div className="portfolio-preview-holding-bar">
                      <span style={{ width: holding.barWidth }} />
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  )
}
