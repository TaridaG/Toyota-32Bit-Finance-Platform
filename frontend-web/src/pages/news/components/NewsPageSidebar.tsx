import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { NewsSidebarStats, NewsTopicKey } from '../lib/buildNewsSidebarStats'

type NewsPageSidebarProps = {
  stats: NewsSidebarStats | null
  loading: boolean
  authenticated: boolean
  activeTopicKey?: NewsTopicKey | null
  activeAssetSymbol?: string | null
  activeSourceName?: string | null
  portfolioActive?: boolean
  onPortfolioNewsClick?: () => void
  onTopicClick?: (key: NewsTopicKey) => void
  onAssetClick?: (symbol: string) => void
  onSourceClick?: (name: string) => void
}

export function NewsPageSidebar({
  stats,
  loading,
  authenticated,
  activeTopicKey = null,
  activeAssetSymbol = null,
  activeSourceName = null,
  portfolioActive = false,
  onPortfolioNewsClick,
  onTopicClick,
  onAssetClick,
  onSourceClick,
}: NewsPageSidebarProps) {
  const { t } = useTranslation('newsPage')

  return (
    <aside className="fi-news-sidebar" aria-label={t('sidebar.aria')}>
      <header className="fi-news-sidebar-weekly-head">
        <h2 className="fi-news-sidebar-weekly-title">{t('sidebar.weeklyHeading')}</h2>
      </header>

      <SidebarCard title={t('sidebar.categoryBreakdownTitle')}>
        {loading ? (
          <p className="fi-news-side-muted">{t('common:loading')}</p>
        ) : stats && stats.topics.length > 0 ? (
          <ul className="fi-news-side-list">
            {stats.topics.map((row) => (
              <li key={row.key}>
                <SidebarRowButton
                  active={activeTopicKey === row.key}
                  onClick={onTopicClick ? () => onTopicClick(row.key) : undefined}
                >
                  <span className={`fi-news-side-dot fi-news-side-dot-${row.key}`} aria-hidden />
                  <span className="fi-news-side-label">{t(`categories.${row.key}`)}</span>
                  <span className="fi-news-side-value">
                    {t('sidebar.weeklyNewsCount', { count: row.count })}
                  </span>
                  <span className="fi-news-side-pct">%{row.percent}</span>
                </SidebarRowButton>
              </li>
            ))}
          </ul>
        ) : (
          <p className="fi-news-side-muted">{t('sidebar.noWeeklyData')}</p>
        )}
      </SidebarCard>

      <SidebarCard title={t('sidebar.topAssetsTitle')}>
        {loading ? (
          <p className="fi-news-side-muted">{t('common:loading')}</p>
        ) : stats && stats.topAssets.length > 0 ? (
          <ul className="fi-news-side-list fi-news-side-list-compact">
            {stats.topAssets.map((row) => (
              <li key={row.symbol}>
                <SidebarRowButton
                  active={activeAssetSymbol === row.symbol}
                  onClick={onAssetClick ? () => onAssetClick(row.symbol) : undefined}
                >
                  <span className="fi-news-side-asset-icon" aria-hidden>
                    {row.symbol.slice(0, 1)}
                  </span>
                  <span className="fi-news-side-label">{row.symbol}</span>
                  <span className="fi-news-side-value">
                    {t('sidebar.weeklyNewsCount', { count: row.count })}
                  </span>
                </SidebarRowButton>
              </li>
            ))}
          </ul>
        ) : (
          <p className="fi-news-side-muted">{t('sidebar.noWeeklyData')}</p>
        )}
      </SidebarCard>

      <SidebarCard title={t('sidebar.portfolioTitle')}>
        {!authenticated ? (
          <p className="fi-news-side-login">{t('sidebar.loginRequired')}</p>
        ) : loading ? (
          <p className="fi-news-side-muted">{t('common:loading')}</p>
        ) : (
          <>
            <p className="fi-news-side-portfolio-count">
              {t('sidebar.weeklyNewsCount', { count: stats?.portfolioRelatedCount ?? 0 })}
            </p>
            {onPortfolioNewsClick ? (
              <button
                type="button"
                className={`fi-news-side-action${portfolioActive ? ' is-active' : ''}`}
                onClick={onPortfolioNewsClick}
              >
                {t('sidebar.portfolioAction')}
              </button>
            ) : null}
          </>
        )}
      </SidebarCard>

      <SidebarCard title={t('sidebar.sourcesTitle')}>
        {loading ? (
          <p className="fi-news-side-muted">{t('common:loading')}</p>
        ) : stats && stats.sources.length > 0 ? (
          <ul className="fi-news-side-list fi-news-side-list-compact">
            {stats.sources.map((row) => (
              <li key={row.name}>
                <SidebarRowButton
                  active={activeSourceName === row.name}
                  onClick={onSourceClick ? () => onSourceClick(row.name) : undefined}
                >
                  <span className="fi-news-side-source-icon" aria-hidden>
                    {row.name.slice(0, 1).toUpperCase()}
                  </span>
                  <span className="fi-news-side-label">{row.name}</span>
                  <span className="fi-news-side-value">
                    {t('sidebar.weeklyNewsCount', { count: row.count })}
                  </span>
                </SidebarRowButton>
              </li>
            ))}
          </ul>
        ) : (
          <p className="fi-news-side-muted">{t('sidebar.noWeeklyData')}</p>
        )}
      </SidebarCard>

      {!authenticated ? (
        <Link to="/login" className="fi-news-side-action fi-news-side-action-link">
          {t('sidebar.loginAction')}
        </Link>
      ) : null}
    </aside>
  )
}

function SidebarRowButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick?: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      className={`fi-news-side-row${active ? ' is-active' : ''}`}
      onClick={onClick}
      disabled={!onClick}
    >
      {children}
    </button>
  )
}

function SidebarCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <article className="card fi-news-side-card">
      <h3 className="fi-news-side-card-title">{title}</h3>
      {children}
    </article>
  )
}
