import { apiClient } from '../../../shared/api/client'
import type { ApiResponse } from '../../../shared/types/portfolio'
import type { SupportedCurrency } from '../../../shared/preferences/preferences'

export type ProfitTargetMode = 'PERCENT' | 'ABSOLUTE'

export type PortfolioGoalCard = {
  goalType: string
  profitTargetMode: ProfitTargetMode | null
  targetAmount: number | null
  targetPercent: number | null
  title: string | null
  description: string | null
  currentAmount: number | null
  currentPercent: number | null
  progressRatio: number | null
  barVariant: 'PORTFOLIO_TOWARD' | 'PROFIT_GAIN' | 'PROFIT_LOSS'
  configured: boolean
}

export type PortfolioGoalsView = {
  scope: 'USER' | 'PORTFOLIO'
  portfolioId: number | null
  currency: string
  portfolioValueGoal: PortfolioGoalCard
  profitGoal: PortfolioGoalCard
}

function parseGoalCard(raw: unknown): PortfolioGoalCard {
  const r = (raw ?? {}) as Record<string, unknown>
  return {
    goalType: String(r.goalType ?? ''),
    profitTargetMode: (r.profitTargetMode as ProfitTargetMode | null) ?? null,
    targetAmount: r.targetAmount != null ? Number(r.targetAmount) : null,
    targetPercent: r.targetPercent != null ? Number(r.targetPercent) : null,
    title: typeof r.title === 'string' ? r.title : null,
    description: typeof r.description === 'string' ? r.description : null,
    currentAmount: r.currentAmount != null ? Number(r.currentAmount) : null,
    currentPercent: r.currentPercent != null ? Number(r.currentPercent) : null,
    progressRatio: r.progressRatio != null ? Number(r.progressRatio) : null,
    barVariant: (r.barVariant as PortfolioGoalCard['barVariant']) ?? 'PORTFOLIO_TOWARD',
    configured: Boolean(r.configured),
  }
}

function parseView(raw: unknown): PortfolioGoalsView {
  const r = (raw ?? {}) as Record<string, unknown>
  return {
    scope: r.scope === 'PORTFOLIO' ? 'PORTFOLIO' : 'USER',
    portfolioId: r.portfolioId != null ? Number(r.portfolioId) : null,
    currency: typeof r.currency === 'string' ? r.currency : 'USD',
    portfolioValueGoal: parseGoalCard(r.portfolioValueGoal),
    profitGoal: parseGoalCard(r.profitGoal),
  }
}

export async function fetchPortfolioGoals(
  portfolioId: number | null,
  displayCurrency: SupportedCurrency,
): Promise<PortfolioGoalsView> {
  const params: Record<string, number> = {}
  if (portfolioId != null && portfolioId > 0) {
    params.portfolioId = portfolioId
  }
  const { data } = await apiClient.get<ApiResponse<unknown>>('/api/v1/portfolio/goals', {
    params,
    headers: { 'X-Currency': displayCurrency },
  })
  if (!data.success) {
    throw new Error('Goals request failed')
  }
  return parseView(data.data)
}

export async function savePortfolioValueGoal(
  portfolioId: number | null,
  body: { targetAmount: number; title?: string; description?: string },
  displayCurrency: SupportedCurrency,
): Promise<PortfolioGoalsView> {
  const { data } = await apiClient.put<ApiResponse<unknown>>('/api/v1/portfolio/goals/portfolio-value', {
    portfolioId: portfolioId != null && portfolioId > 0 ? portfolioId : null,
    ...body,
  }, { headers: { 'X-Currency': displayCurrency } })
  if (!data.success) {
    throw new Error('Save failed')
  }
  return parseView(data.data)
}

export async function saveProfitGoal(
  portfolioId: number | null,
  body: {
    profitTargetMode: ProfitTargetMode
    targetAmount?: number
    targetPercent?: number
    title?: string
    description?: string
  },
  displayCurrency: SupportedCurrency,
): Promise<PortfolioGoalsView> {
  const { data } = await apiClient.put<ApiResponse<unknown>>('/api/v1/portfolio/goals/profit', {
    portfolioId: portfolioId != null && portfolioId > 0 ? portfolioId : null,
    ...body,
  }, { headers: { 'X-Currency': displayCurrency } })
  if (!data.success) {
    throw new Error('Save failed')
  }
  return parseView(data.data)
}
