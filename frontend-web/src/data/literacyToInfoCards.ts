import type { LiteracyEntry, LiteracyPortalPage } from '../pages/finansal-okuryazarlik/types/financialLiteracy'
import type { InfoCard, InfoCardCategory, PortalPageKey } from '../types/infoCards'
import { FINANCIAL_LITERACY_TERMS } from '../pages/finansal-okuryazarlik/data/financialLiteracyTerms'

const CATEGORY_DEFAULT_PAGE: Record<InfoCardCategory, PortalPageKey> = {
  BASIC_FINANCE: 'DASHBOARD',
  MARKET_DATA: 'MARKETS',
  PORTFOLIO_ANALYSIS: 'PORTFOLIO',
  CHARTS: 'MARKETS',
  TURKEY_ECONOMY: 'TURKEY_ECONOMY',
  SIMULATION: 'SIMULATION',
  NOTIFICATIONS: 'NOTIFICATIONS',
  SYSTEM_OBSERVABILITY: 'ADMIN',
}

function mapPortalPage(page: LiteracyPortalPage): PortalPageKey {
  if (page === 'FAIZ_VADELI') return 'FAIZ_VADELI'
  return page as PortalPageKey
}

export function literacyEntryToInfoCard(entry: LiteracyEntry, now: string): InfoCard {
  const pagesFromUsage = [...new Set(entry.usedInPortal.map((u) => mapPortalPage(u.page)))]
  const pages = pagesFromUsage.length > 0 ? pagesFromUsage : [CATEGORY_DEFAULT_PAGE[entry.category as InfoCardCategory]]

  const targetTerms = [...new Set([entry.title, ...entry.tags.filter((t) => t.length > 1)])]

  return {
    id: entry.id,
    title: entry.title,
    slug: entry.slug,
    targetTerms,
    pages,
    category: entry.category as InfoCardCategory,
    type: entry.type,
    difficulty: entry.difficulty,
    status: 'ACTIVE',
    shortDescription: entry.shortDefinition,
    detailedDescription: entry.financialMeaning,
    howToInterpret: entry.howToInterpret,
    commonMistake: entry.commonMistake,
    example: entry.example,
    relatedTerms: entry.relatedTerms,
    adminOnly: entry.adminOnly,
    createdAt: now,
    updatedAt: now,
  }
}

export function buildDefaultInfoCards(): InfoCard[] {
  const now = new Date().toISOString()
  return FINANCIAL_LITERACY_TERMS.map((e) => literacyEntryToInfoCard(e, now))
}
