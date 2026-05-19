import type { SupportedLocale } from '../shared/i18n'

export type InfoCardStatus = 'ACTIVE' | 'PASSIVE'

export type InfoCardLocaleContent = {
  title: string
  shortDescription: string
  detailedDescription: string
  howToInterpret?: string
  commonMistake?: string
  example?: string
  relatedTerms: string[]
}

export type InfoCardTranslations = Partial<Record<SupportedLocale, InfoCardLocaleContent>>

export type InfoCardType =
  | 'TERM'
  | 'ASSET'
  | 'CHART'
  | 'ANALYSIS_TOOL'
  | 'MACRO_INDICATOR'
  | 'SYSTEM_TERM'

export type InfoCardDifficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

export type InfoCardCategory =
  | 'BASIC_FINANCE'
  | 'MARKET_DATA'
  | 'PORTFOLIO_ANALYSIS'
  | 'CHARTS'
  | 'TURKEY_ECONOMY'
  | 'SIMULATION'
  | 'NOTIFICATIONS'
  | 'SYSTEM_OBSERVABILITY'

export type PortalPageKey =
  | 'TURKEY_ECONOMY'
  | 'FINANCIAL_LITERACY'
  | 'BANK_RATES'
  | 'MARKETS'
  | 'FAIZ_VADELI'
  | 'PORTFOLIO'
  | 'ANALYSIS'
  | 'NEWS'
  | 'SIMULATION'
  | 'PROFILE'
  | 'DASHBOARD'
  | 'ADMIN'
  | 'ADMIN_KPI_USERS'
  | 'ADMIN_KPI_PORTFOLIOS'
  | 'ADMIN_KPI_MARKETS'
  | 'ADMIN_KPI_NEWS'
  | 'ADMIN_KPI_SYSTEM'
  | 'ADMIN_KPI_LATENCY'
  | 'INFO_CARDS'
  /** @deprecated Legacy key — treated as PROFILE in filters */
  | 'NOTIFICATIONS'
  /** @deprecated Legacy key — counted under ADMIN */
  | 'AUDIT_LOGS'

export interface InfoCard {
  id: string
  title: string
  slug: string
  targetTerms: string[]
  /** Portal UI elements (buttons, labels) that open this card in help mode */
  targetElementIds?: string[]
  /** Market/portfolio instrument symbols (e.g. MSFT, FUND_TP2) */
  targetInstrumentSymbols?: string[]
  pages: PortalPageKey[]
  category: InfoCardCategory
  type: InfoCardType
  difficulty: InfoCardDifficulty
  status: InfoCardStatus
  shortDescription: string
  detailedDescription: string
  howToInterpret?: string
  commonMistake?: string
  example?: string
  relatedTerms: string[]
  adminOnly?: boolean
  translations?: InfoCardTranslations
  createdAt: string
  updatedAt: string
}

export interface InfoCardsDashboard {
  activeCards: number
  passiveCards: number
  averageWordCount: number
  coveredPages: number
  mostCoveredPage: PortalPageKey | string | null
  beginnerCount: number
  intermediateCount: number
  advancedCount: number
  lastUpdatedCardTitle: string | null
}

export type InfoCardInput = Omit<InfoCard, 'id' | 'slug' | 'createdAt' | 'updatedAt'> & {
  id?: string
  slug?: string
  translations?: InfoCardTranslations
}
