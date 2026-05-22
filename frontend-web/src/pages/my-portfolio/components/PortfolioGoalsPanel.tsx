import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  fetchPortfolioGoals,
  savePortfolioValueGoal,
  saveProfitGoal,
  type PortfolioGoalCard,
  type PortfolioGoalsView,
  type ProfitTargetMode,
} from '../../../features/portfolio/api/portfolioGoalsApi'
import type { SupportedCurrency } from '../../../shared/preferences/preferences'

type Props = {
  portfolioId: number | null
  displayCurrency: SupportedCurrency
  moneyFormat: Intl.NumberFormat
  percentFormat: Intl.NumberFormat
  hideMoney: boolean
  isDarkTheme: boolean
}

type EditKind = 'portfolio' | 'profit' | null

const MASK = '••••'

function clamp01(ratio: number): number {
  if (!Number.isFinite(ratio)) return 0
  return Math.max(0, Math.min(1, ratio))
}

function barFillPercent(card: PortfolioGoalCard): number {
  const ratio = card.progressRatio ?? 0
  return clamp01(ratio) * 100
}

function IconEdit() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden>
      <path
        d="M4 20h4l10.5-10.5a1.5 1.5 0 0 0 0-2.12L14.62 3.5a1.5 1.5 0 0 0-2.12 0L4 12v8z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
      <path d="M13.5 6.5l4 4" fill="none" stroke="currentColor" strokeWidth="1.6" />
    </svg>
  )
}

function GoalHorizontalBar({
  card,
  moneyFormat,
  percentFormat,
  hideMoney,
  emptyHint,
}: {
  card: PortfolioGoalCard
  moneyFormat: Intl.NumberFormat
  percentFormat: Intl.NumberFormat
  hideMoney: boolean
  emptyHint: string
}) {
  const fillPct = barFillPercent(card)
  const variantClass =
    card.barVariant === 'PORTFOLIO_TOWARD'
      ? 'my-portfolio-goal-bar-fill-portfolio'
      : card.barVariant === 'PROFIT_LOSS'
        ? 'my-portfolio-goal-bar-fill-loss'
        : 'my-portfolio-goal-bar-fill-gain'

  const ratioLabel = card.configured
    ? `${Math.round((card.progressRatio ?? 0) * 100)}%`
    : '—'

  const currentMoney =
    hideMoney || card.currentAmount == null ? MASK : moneyFormat.format(card.currentAmount)
  const targetMoney =
    hideMoney || card.targetAmount == null ? MASK : moneyFormat.format(card.targetAmount)
  const targetLabel =
    card.configured && card.profitTargetMode === 'PERCENT' && card.targetPercent != null
      ? `${percentFormat.format(card.targetPercent)}%`
      : card.configured
        ? targetMoney
        : emptyHint

  return (
    <div className="my-portfolio-goal-bar-block">
      <div className="my-portfolio-goal-bar-track" aria-hidden>
        <div
          className={`my-portfolio-goal-bar-fill ${variantClass}`}
          style={{ width: `${fillPct}%` }}
        />
      </div>
      <div className="my-portfolio-goal-bar-meta">
        <span className="my-portfolio-goal-bar-progress">{ratioLabel}</span>
        <span className="my-portfolio-goal-bar-current">{currentMoney}</span>
        <span className={card.configured ? 'my-portfolio-goal-bar-target' : 'my-portfolio-goal-bar-target-muted'}>
          {card.configured ? `→ ${targetLabel}` : targetLabel}
        </span>
      </div>
    </div>
  )
}

export function PortfolioGoalsPanel({
  portfolioId,
  displayCurrency,
  moneyFormat,
  percentFormat,
  hideMoney,
  isDarkTheme,
}: Props) {
  const { t } = useTranslation('portfolio')
  const [view, setView] = useState<PortfolioGoalsView | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editKind, setEditKind] = useState<EditKind>(null)
  const [saving, setSaving] = useState(false)

  const [pvTarget, setPvTarget] = useState('')
  const [pvTitle, setPvTitle] = useState('')
  const [pvDescription, setPvDescription] = useState('')

  const [profitMode, setProfitMode] = useState<ProfitTargetMode>('ABSOLUTE')
  const [profitTargetAmount, setProfitTargetAmount] = useState('')
  const [profitTargetPercent, setProfitTargetPercent] = useState('')
  const [profitTitle, setProfitTitle] = useState('')
  const [profitDescription, setProfitDescription] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchPortfolioGoals(portfolioId, displayCurrency)
      setView(data)
    } catch (err) {
      setView(null)
      setError(err instanceof Error ? err.message : t('goals.loadError'))
    } finally {
      setLoading(false)
    }
  }, [displayCurrency, portfolioId, t])

  useEffect(() => {
    void load()
  }, [load])

  const scopeHint = useMemo(() => {
    if (!view) return ''
    return view.scope === 'USER' ? t('goals.scopeUser') : t('goals.scopePortfolio')
  }, [t, view])

  const openPortfolioEdit = () => {
    const g = view?.portfolioValueGoal
    setPvTarget(g?.targetAmount != null ? String(g.targetAmount) : '')
    setPvTitle(g?.title ?? t('goals.defaultPortfolioTitle'))
    setPvDescription(g?.description ?? '')
    setEditKind('portfolio')
  }

  const openProfitEdit = () => {
    const g = view?.profitGoal
    const mode = g?.profitTargetMode === 'PERCENT' ? 'PERCENT' : 'ABSOLUTE'
    setProfitMode(mode)
    setProfitTargetAmount(g?.targetAmount != null ? String(g.targetAmount) : '')
    setProfitTargetPercent(g?.targetPercent != null ? String(g.targetPercent) : '')
    setProfitTitle(g?.title ?? t('goals.defaultProfitTitle'))
    setProfitDescription(g?.description ?? '')
    setEditKind('profit')
  }

  const handleSavePortfolio = async () => {
    const amount = Number(pvTarget.replace(/\s/g, '').replace(',', '.'))
    if (!Number.isFinite(amount) || amount <= 0) {
      setError(t('goals.errors.targetRequired'))
      return
    }
    setSaving(true)
    setError(null)
    try {
      const data = await savePortfolioValueGoal(
        portfolioId,
        { targetAmount: amount, title: pvTitle.trim(), description: pvDescription.trim() || undefined },
        displayCurrency,
      )
      setView(data)
      setEditKind(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('goals.saveError'))
    } finally {
      setSaving(false)
    }
  }

  const handleSaveProfit = async () => {
    setSaving(true)
    setError(null)
    try {
      if (profitMode === 'PERCENT') {
        const pct = Number(profitTargetPercent.replace(/\s/g, '').replace(',', '.'))
        if (!Number.isFinite(pct) || pct <= 0) {
          setError(t('goals.errors.percentRequired'))
          setSaving(false)
          return
        }
        const data = await saveProfitGoal(
          portfolioId,
          {
            profitTargetMode: 'PERCENT',
            targetPercent: pct,
            title: profitTitle.trim(),
            description: profitDescription.trim() || undefined,
          },
          displayCurrency,
        )
        setView(data)
      } else {
        const amount = Number(profitTargetAmount.replace(/\s/g, '').replace(',', '.'))
        if (!Number.isFinite(amount) || amount <= 0) {
          setError(t('goals.errors.targetRequired'))
          setSaving(false)
          return
        }
        const data = await saveProfitGoal(
          portfolioId,
          {
            profitTargetMode: 'ABSOLUTE',
            targetAmount: amount,
            title: profitTitle.trim(),
            description: profitDescription.trim() || undefined,
          },
          displayCurrency,
        )
        setView(data)
      }
      setEditKind(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('goals.saveError'))
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <div className="markets-skeleton-row" style={{ minHeight: 280 }} />
  }

  return (
    <article className={`card my-portfolio-goals-card${isDarkTheme ? ' is-dark' : ' is-light'}`}>
      <header className="my-portfolio-goals-header">
        <div>
          <h2 className="my-portfolio-goals-page-title">{t('goals.pageTitle')}</h2>
          <p className="my-portfolio-goals-scope">{scopeHint}</p>
        </div>
      </header>

      {error ? <p className="auth-error">{error}</p> : null}

      {view ? (
        <div className="my-portfolio-goals-stack">
          <section className="my-portfolio-goal-row">
            <div className="my-portfolio-goal-row-head">
              <h3 className="my-portfolio-goal-row-title">{t('goals.portfolioTitle')}</h3>
              <button
                type="button"
                className="my-portfolio-goal-edit-btn"
                aria-label={t('goals.editPortfolio')}
                onClick={openPortfolioEdit}
              >
                <IconEdit />
              </button>
            </div>
            {view.portfolioValueGoal.title ? (
              <p className="my-portfolio-goal-planned-title">{view.portfolioValueGoal.title}</p>
            ) : null}
            {view.portfolioValueGoal.description ? (
              <p className="my-portfolio-goal-planned-desc">{view.portfolioValueGoal.description}</p>
            ) : null}
            <GoalHorizontalBar
              card={view.portfolioValueGoal}
              moneyFormat={moneyFormat}
              percentFormat={percentFormat}
              hideMoney={hideMoney}
              emptyHint={t('goals.setTargetHint')}
            />
          </section>

          <section className="my-portfolio-goal-row">
            <div className="my-portfolio-goal-row-head">
              <h3 className="my-portfolio-goal-row-title">{t('goals.profitTitle')}</h3>
              <button
                type="button"
                className="my-portfolio-goal-edit-btn"
                aria-label={t('goals.editProfit')}
                onClick={openProfitEdit}
              >
                <IconEdit />
              </button>
            </div>
            {view.profitGoal.title ? (
              <p className="my-portfolio-goal-planned-title">{view.profitGoal.title}</p>
            ) : null}
            {view.profitGoal.description ? (
              <p className="my-portfolio-goal-planned-desc">{view.profitGoal.description}</p>
            ) : null}
            <GoalHorizontalBar
              card={view.profitGoal}
              moneyFormat={moneyFormat}
              percentFormat={percentFormat}
              hideMoney={hideMoney}
              emptyHint={t('goals.setTargetHint')}
            />
          </section>
        </div>
      ) : null}

      {editKind === 'portfolio' ? (
        <div className="my-portfolio-modal-overlay" role="dialog" aria-modal="true">
          <div className="my-portfolio-modal card">
            <h3 id="goal-pv-title">{t('goals.editPortfolio')}</h3>
            <p className="my-portfolio-trade-subtitle">{scopeHint}</p>
            <label className="my-portfolio-modal-field">
              <span>{t('goals.targetAmountLabel')}</span>
              <input
                type="text"
                inputMode="decimal"
                value={pvTarget}
                onChange={(e) => setPvTarget(e.target.value)}
              />
            </label>
            <label className="my-portfolio-modal-field">
              <span>{t('goals.plannedTitleLabel')}</span>
              <input type="text" value={pvTitle} onChange={(e) => setPvTitle(e.target.value)} />
            </label>
            <label className="my-portfolio-modal-field">
              <span>{t('goals.plannedDescLabel')}</span>
              <textarea
                rows={3}
                value={pvDescription}
                onChange={(e) => setPvDescription(e.target.value)}
              />
            </label>
            <div className="my-portfolio-modal-actions">
              <button type="button" className="markets-filter" onClick={() => setEditKind(null)} disabled={saving}>
                {t('goals.cancel')}
              </button>
              <button type="button" className="auth-submit" disabled={saving} onClick={() => void handleSavePortfolio()}>
                {saving ? t('goals.saving') : t('goals.save')}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {editKind === 'profit' ? (
        <div className="my-portfolio-modal-overlay" role="dialog" aria-modal="true">
          <div className="my-portfolio-modal card">
            <h3>{t('goals.editProfit')}</h3>
            <p className="my-portfolio-trade-subtitle">{scopeHint}</p>
            <fieldset className="my-portfolio-goal-mode-fieldset">
              <legend>{t('goals.profitModeLegend')}</legend>
              <label className="my-portfolio-settings-toggle">
                <input
                  type="radio"
                  name="profit-mode"
                  checked={profitMode === 'ABSOLUTE'}
                  onChange={() => setProfitMode('ABSOLUTE')}
                />
                <span>{t('goals.profitModeAbsolute')}</span>
              </label>
              <label className="my-portfolio-settings-toggle">
                <input
                  type="radio"
                  name="profit-mode"
                  checked={profitMode === 'PERCENT'}
                  onChange={() => setProfitMode('PERCENT')}
                />
                <span>{t('goals.profitModePercent')}</span>
              </label>
            </fieldset>
            {profitMode === 'ABSOLUTE' ? (
              <label className="my-portfolio-modal-field">
                <span>{t('goals.profitAmountLabel')}</span>
                <input
                  type="text"
                  inputMode="decimal"
                  value={profitTargetAmount}
                  onChange={(e) => setProfitTargetAmount(e.target.value)}
                />
              </label>
            ) : (
              <label className="my-portfolio-modal-field">
                <span>{t('goals.profitPercentLabel')}</span>
                <input
                  type="text"
                  inputMode="decimal"
                  value={profitTargetPercent}
                  onChange={(e) => setProfitTargetPercent(e.target.value)}
                />
              </label>
            )}
            <label className="my-portfolio-modal-field">
              <span>{t('goals.plannedTitleLabel')}</span>
              <input type="text" value={profitTitle} onChange={(e) => setProfitTitle(e.target.value)} />
            </label>
            <label className="my-portfolio-modal-field">
              <span>{t('goals.plannedDescLabel')}</span>
              <textarea
                rows={3}
                value={profitDescription}
                onChange={(e) => setProfitDescription(e.target.value)}
              />
            </label>
            <div className="my-portfolio-modal-actions">
              <button type="button" className="markets-filter" onClick={() => setEditKind(null)} disabled={saving}>
                {t('goals.cancel')}
              </button>
              <button type="button" className="auth-submit" disabled={saving} onClick={() => void handleSaveProfit()}>
                {saving ? t('goals.saving') : t('goals.save')}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </article>
  )
}
