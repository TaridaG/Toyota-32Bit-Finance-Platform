import { useTranslation } from 'react-i18next'
import type { CategoryPerformanceRow } from '../../../features/markets/api/marketService'
import type { MarketCategory } from '../../../shared/types/market'
import { useCategoryMovers } from '../hooks/useCategoryMovers'

type CategoryWinnersLosersPanelProps = {
  segment: MarketCategory
  onSelectSymbol?: (symbol: string) => void
}

const SEGMENT_TITLE_KEYS: Record<MarketCategory, { winners: string; losers: string }> = {
  all: { winners: 'winnersLosers.titles.winners.all', losers: 'winnersLosers.titles.losers.all' },
  bist: { winners: 'winnersLosers.titles.winners.bist', losers: 'winnersLosers.titles.losers.bist' },
  nasdaq: { winners: 'winnersLosers.titles.winners.nasdaq', losers: 'winnersLosers.titles.losers.nasdaq' },
  crypto: { winners: 'winnersLosers.titles.winners.crypto', losers: 'winnersLosers.titles.losers.crypto' },
  forex: { winners: 'winnersLosers.titles.winners.forex', losers: 'winnersLosers.titles.losers.forex' },
  metals: { winners: 'winnersLosers.titles.winners.metals', losers: 'winnersLosers.titles.losers.metals' },
  globalFutures: {
    winners: 'winnersLosers.titles.winners.globalFutures',
    losers: 'winnersLosers.titles.losers.globalFutures',
  },
  funds: { winners: 'winnersLosers.titles.winners.funds', losers: 'winnersLosers.titles.losers.funds' },
  bonds: { winners: 'winnersLosers.titles.winners.bonds', losers: 'winnersLosers.titles.losers.bonds' },
  eurobond: { winners: 'winnersLosers.titles.winners.eurobond', losers: 'winnersLosers.titles.losers.eurobond' },
}

function pctCell(value: number): string {
  return `${value.toFixed(2)}%`
}

function MoversTable({
  rows,
  onSelectSymbol,
  emptyLabel,
}: {
  rows: CategoryPerformanceRow[]
  onSelectSymbol?: (symbol: string) => void
  emptyLabel: string
}) {
  const { t } = useTranslation('analysis')

  if (rows.length === 0) {
    return <p className="fi-empty fi-category-movers-empty">{emptyLabel}</p>
  }

  return (
    <table>
      <thead>
        <tr>
          <th>{t('columns.asset')}</th>
          <th>{t('columns.weekly')}</th>
          <th>{t('columns.monthly')}</th>
          <th>{t('columns.yearly')}</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.symbol}>
            <td>
              {onSelectSymbol ? (
                <button type="button" className="fi-category-movers-symbol" onClick={() => onSelectSymbol(row.symbol)}>
                  {row.symbol}
                </button>
              ) : (
                row.symbol
              )}
            </td>
            <td className={row.weeklyPct >= 0 ? 'fi-up' : 'fi-down'}>{pctCell(row.weeklyPct)}</td>
            <td className={row.monthlyPct >= 0 ? 'fi-up' : 'fi-down'}>{pctCell(row.monthlyPct)}</td>
            <td className={row.yearlyPct >= 0 ? 'fi-up' : 'fi-down'}>{pctCell(row.yearlyPct)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export function CategoryWinnersLosersPanel({ segment, onSelectSymbol }: CategoryWinnersLosersPanelProps) {
  const { t } = useTranslation(['analysis', 'markets', 'common'])
  const { loading, error, winners, losers } = useCategoryMovers(segment)

  const titleKeys = SEGMENT_TITLE_KEYS[segment]
  const winnersTitle = t(titleKeys.winners)
  const losersTitle = t(titleKeys.losers)
  const segmentLabel = t(`markets:categories.${segment}`)

  return (
    <article className="card fi-category-movers">
      {loading ? (
        <div className="markets-skeleton-row fi-category-movers-skel" aria-busy="true" aria-label={t('common:loading')} />
      ) : null}

      {error ? (
        <p className="fi-empty">{t('winnersLosers.loadError')}</p>
      ) : (
        <>
          <section className="fi-category-movers-block">
            <div className="fi-panel-head">
              <h3>{winnersTitle}</h3>
              <small>{t('winnersLosers.subtitle', { segment: segmentLabel })}</small>
            </div>
            <MoversTable
              rows={winners}
              onSelectSymbol={onSelectSymbol}
              emptyLabel={t('winnersLosers.emptyWinners')}
            />
          </section>

          <section className="fi-category-movers-block fi-category-movers-block--losers">
            <div className="fi-panel-head">
              <h3>{losersTitle}</h3>
            </div>
            <MoversTable
              rows={losers}
              onSelectSymbol={onSelectSymbol}
              emptyLabel={t('winnersLosers.emptyLosers')}
            />
          </section>
        </>
      )}
    </article>
  )
}
