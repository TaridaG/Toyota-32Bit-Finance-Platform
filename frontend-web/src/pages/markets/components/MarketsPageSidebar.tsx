import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { resolveInstrumentDisplayLabel } from '../../../features/markets/lib/tefasFundDisplay'
import type { MarketChampion, MarketChampionPeriod, MarketChampionsSnapshot } from '../lib/marketChampions'

type MarketsPageSidebarProps = {
  champions: MarketChampionsSnapshot | null
  loading: boolean
}

const PERIODS: MarketChampionPeriod[] = ['day', 'week', 'month', 'year']

export function MarketsPageSidebar({ champions, loading }: MarketsPageSidebarProps) {
  const { t, i18n } = useTranslation('markets')

  const percentFormat = new Intl.NumberFormat(i18n.language, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
    signDisplay: 'always',
  })

  return (
    <aside className="fi-news-sidebar fi-markets-sidebar" aria-label={t('sidebar.aria')}>
      <header className="fi-news-sidebar-weekly-head">
        <h2 className="fi-news-sidebar-weekly-title">{t('sidebar.heading')}</h2>
      </header>

      {PERIODS.map((period) => (
        <SidebarCard key={period} title={t(`sidebar.champions.${period}`)}>
          {loading ? (
            <p className="fi-news-side-muted">{t('common:loading')}</p>
          ) : (
            <ChampionRow champion={champions?.[period] ?? null} formatPct={percentFormat.format} emptyLabel={t('sidebar.noChampion')} />
          )}
        </SidebarCard>
      ))}
    </aside>
  )
}

function ChampionRow({
  champion,
  formatPct,
  emptyLabel,
}: {
  champion: MarketChampion | null
  formatPct: (value: number) => string
  emptyLabel: string
}) {
  if (!champion) {
    return <p className="fi-news-side-muted">{emptyLabel}</p>
  }

  const positive = champion.changePct >= 0
  const display = resolveInstrumentDisplayLabel(champion.symbol, champion.name)

  return (
    <div className="fi-markets-champion-row">
      <span className="fi-news-side-asset-icon" aria-hidden>
        {display.symbol.slice(0, 1)}
      </span>
      <div className="fi-markets-champion-copy">
        <strong className="fi-markets-champion-symbol">{display.symbol}</strong>
        <span className="fi-markets-champion-name">{display.name}</span>
      </div>
      <span className={`fi-markets-champion-pct${positive ? ' markets-positive' : ' markets-negative'}`}>
        {formatPct(champion.changePct)}%
      </span>
    </div>
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
