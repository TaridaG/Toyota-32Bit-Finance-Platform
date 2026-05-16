export type LiteracyContentType =
  | 'TERM'
  | 'ASSET'
  | 'CHART'
  | 'ANALYSIS_TOOL'
  | 'MACRO_INDICATOR'
  | 'SYSTEM_TERM'

export type LiteracyDifficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

export type LiteracyCategory =
  | 'BASIC_FINANCE'
  | 'MARKET_DATA'
  | 'PORTFOLIO_ANALYSIS'
  | 'CHARTS'
  | 'TURKEY_ECONOMY'
  | 'SIMULATION'
  | 'NOTIFICATIONS'
  | 'SYSTEM_OBSERVABILITY'

export type LiteracyPortalPage =
  | 'DASHBOARD'
  | 'MARKETS'
  | 'PORTFOLIO'
  | 'SIMULATION'
  | 'TURKEY_ECONOMY'
  | 'FAIZ_VADELI'
  | 'ANALYSIS'
  | 'NEWS'
  | 'NOTIFICATIONS'
  | 'ADMIN'
  | 'BANK_RATES'

export interface LiteracyPortalUsage {
  page: LiteracyPortalPage
  description: string
  route?: string
}

export interface LiteracyEntry {
  id: string
  title: string
  slug: string
  type: LiteracyContentType
  category: LiteracyCategory
  difficulty: LiteracyDifficulty
  shortDefinition: string
  financialMeaning: string
  usedInPortal: LiteracyPortalUsage[]
  howToInterpret: string
  commonMistake?: string
  example?: string
  relatedTerms: string[]
  tags: string[]
  adminOnly?: boolean
}

export const LITERACY_PORTAL_ROUTES: Record<LiteracyPortalPage, string> = {
  DASHBOARD: '/app/markets',
  MARKETS: '/app/markets',
  PORTFOLIO: '/app/my-portfolio',
  SIMULATION: '/app/simulation',
  TURKEY_ECONOMY: '/app/turkiye-ekonomisi',
  FAIZ_VADELI: '/app/faiz-vadeli',
  ANALYSIS: '/app/analysis',
  NEWS: '/app/news',
  NOTIFICATIONS: '/app/profile',
  ADMIN: '/admin',
  BANK_RATES: '/app/bank-rates',
}

export const LITERACY_CATEGORIES: LiteracyCategory[] = [
  'BASIC_FINANCE',
  'MARKET_DATA',
  'PORTFOLIO_ANALYSIS',
  'CHARTS',
  'TURKEY_ECONOMY',
  'SIMULATION',
  'NOTIFICATIONS',
  'SYSTEM_OBSERVABILITY',
]

export const LITERACY_CONTENT_TYPES: LiteracyContentType[] = [
  'TERM',
  'ASSET',
  'CHART',
  'ANALYSIS_TOOL',
  'MACRO_INDICATOR',
  'SYSTEM_TERM',
]

export const LITERACY_DIFFICULTIES: LiteracyDifficulty[] = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED']

export const LITERACY_PORTAL_PAGES: LiteracyPortalPage[] = [
  'DASHBOARD',
  'MARKETS',
  'PORTFOLIO',
  'SIMULATION',
  'TURKEY_ECONOMY',
  'FAIZ_VADELI',
  'ANALYSIS',
  'NEWS',
  'NOTIFICATIONS',
  'ADMIN',
  'BANK_RATES',
]
