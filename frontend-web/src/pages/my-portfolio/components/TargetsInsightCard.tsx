import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import {
  fetchPortfolioGoals,
  type PortfolioGoalCard,
  type PortfolioGoalsView,
} from '../../../features/portfolio/api/portfolioGoalsApi'
import type { SupportedCurrency } from '../../../shared/preferences/preferences'

type Props = {
  portfolioId: number | null
  displayCurrency: SupportedCurrency
  moneyFormat: Intl.NumberFormat
  percentFormat: Intl.NumberFormat
  hideMoney: boolean
}

const MASK = '••••'

function clamp01(ratio: number): number {
  if (!Number.isFinite(ratio)) return 0
  return Math.max(0, Math.min(1, ratio))
}

function barFillPercent(card: PortfolioGoalCard): number {
  return clamp01(card.progressRatio ?? 0) * 100
}

function barFillVariantClass(card: PortfolioGoalCard): string {
  if (card.barVariant === 'PORTFOLIO_TOWARD') return 'my-portfolio-goal-bar-fill-portfolio'
  return card.barVariant === 'PROFIT_LOSS'
    ? 'my-portfolio-goal-bar-fill-loss'
    : 'my-portfolio-goal-bar-fill-gain'
}

function pickPrimaryGoal(view: PortfolioGoalsView): {
  card: PortfolioGoalCard
  kind: 'portfolio' | 'profit'
} {
  if (view.portfolioValueGoal.configured || !view.profitGoal.configured) {
    return { card: view.portfolioValueGoal, kind: 'portfolio' }
  }
  return { card: view.profitGoal, kind: 'profit' }
}

export function TargetsInsightCard({
  portfolioId,
  displayCurrency,
  moneyFormat,
  percentFormat,
  hideMoney,
}: Props) {
  const { t } = useTranslation(['portfolio', 'common'])
  const [searchParams] = useSearchParams()
  const [view, setView] = useState<PortfolioGoalsView | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadFailed, setLoadFailed] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setLoadFailed(false)
    void fetchPortfolioGoals(portfolioId, displayCurrency)
      .then((data) => {
        if (!cancelled) {
          setView(data)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setView(null)
          setLoadFailed(true)
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [displayCurrency, portfolioId])

  const targetHref = useMemo(() => {
    const next = new URLSearchParams(searchParams)
    next.set('section', 'targets')
    return `/app/my-portfolio?${next.toString()}`
  }, [searchParams])

  const goalSummary = useMemo(() => {
    if (!view) return null
    const { card, kind } = pickPrimaryGoal(view)
    const label =
      kind === 'portfolio' ? t('portfolio:goals.portfolioTitle') : t('portfolio:goals.profitTitle')
    const scopeLabel =
      view.scope === 'USER' ? t('portfolio:goals.scopeUser') : t('portfolio:goals.scopePortfolio')

    return {
      card,
      label,
      title: card.title?.trim() || label,
      detail: card.configured
        ? card.description?.trim() || scopeLabel
        : t('portfolio:targetInsightEmpty'),
    }
  }, [t, view])

  const ratioLabel =
    goalSummary?.card.configured ? `${Math.round((goalSummary.card.progressRatio ?? 0) * 100)}%` : '—'

  const currentLabel =
    hideMoney || goalSummary?.card.currentAmount == null
      ? MASK
      : moneyFormat.format(goalSummary.card.currentAmount)

  const targetLabel =
    goalSummary?.card.configured && goalSummary.card.profitTargetMode === 'PERCENT' && goalSummary.card.targetPercent != null
      ? `${percentFormat.format(goalSummary.card.targetPercent)}%`
      : goalSummary?.card.configured
        ? hideMoney || goalSummary.card.targetAmount == null
          ? MASK
          : moneyFormat.format(goalSummary.card.targetAmount)
        : t('portfolio:goals.setTargetHint')

  return (
    <article className="card my-portfolio-card my-portfolio-card--dashboard-goal">
      <div className="my-portfolio-card-head">
        <h3>{t('portfolio:sidebar.items.targets')}</h3>
        <Link to={targetHref} className="my-portfolio-card-link">
          {t('portfolio:actions.viewAll')}
        </Link>
      </div>
      {loading ? (
        <p className="my-portfolio-insight-empty">{t('common:loading')}</p>
      ) : loadFailed || goalSummary == null ? (
        <p className="my-portfolio-insight-empty">{t('portfolio:targetInsightLoadError')}</p>
      ) : (
        <>
          <div className="my-portfolio-goal-insight-copy">
            <p className="my-portfolio-goal-insight-kicker">{goalSummary.label}</p>
            <p className="my-portfolio-goal-insight-name">{goalSummary.title}</p>
            <p className="my-portfolio-goal-insight-body">{goalSummary.detail}</p>
          </div>
          <div className="my-portfolio-goal-bar-block">
            <div className="my-portfolio-goal-bar-track" aria-hidden>
              <div
                className={`my-portfolio-goal-bar-fill ${barFillVariantClass(goalSummary.card)}`}
                style={{ width: `${barFillPercent(goalSummary.card)}%` }}
              />
            </div>
            <div className="my-portfolio-goal-bar-meta">
              <span className="my-portfolio-goal-bar-progress">{ratioLabel}</span>
              <span className="my-portfolio-goal-bar-current">{currentLabel}</span>
              <span
                className={
                  goalSummary.card.configured
                    ? 'my-portfolio-goal-bar-target'
                    : 'my-portfolio-goal-bar-target-muted'
                }
              >
                {goalSummary.card.configured ? `→ ${targetLabel}` : targetLabel}
              </span>
            </div>
          </div>
        </>
      )}
    </article>
  )
}
