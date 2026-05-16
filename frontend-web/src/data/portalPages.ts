import type { PortalPageKey } from '../types/infoCards'

export type PortalPageDef = {
  key: PortalPageKey
  labelKey: string
  route: string
  userVisible: boolean
  adminOnly?: boolean
}

/** All portal page definitions (keys must stay stable for saved info cards). */
const PORTAL_PAGE_DEFS: Record<PortalPageKey, PortalPageDef> = {
  TURKEY_ECONOMY: {
    key: 'TURKEY_ECONOMY',
    labelKey: 'bilgiKartlariPage.pages.TURKEY_ECONOMY',
    route: '/app/turkiye-ekonomisi',
    userVisible: true,
  },
  FINANCIAL_LITERACY: {
    key: 'FINANCIAL_LITERACY',
    labelKey: 'bilgiKartlariPage.pages.FINANCIAL_LITERACY',
    route: '/app/finansal-okuryazarlik',
    userVisible: true,
  },
  BANK_RATES: {
    key: 'BANK_RATES',
    labelKey: 'bilgiKartlariPage.pages.BANK_RATES',
    route: '/app/bank-rates',
    userVisible: true,
  },
  MARKETS: {
    key: 'MARKETS',
    labelKey: 'bilgiKartlariPage.pages.MARKETS',
    route: '/app/markets',
    userVisible: true,
  },
  FAIZ_VADELI: {
    key: 'FAIZ_VADELI',
    labelKey: 'bilgiKartlariPage.pages.FAIZ_VADELI',
    route: '/app/faiz-vadeli',
    userVisible: true,
  },
  PORTFOLIO: {
    key: 'PORTFOLIO',
    labelKey: 'bilgiKartlariPage.pages.PORTFOLIO',
    route: '/app/my-portfolio',
    userVisible: true,
  },
  ANALYSIS: {
    key: 'ANALYSIS',
    labelKey: 'bilgiKartlariPage.pages.ANALYSIS',
    route: '/app/analysis',
    userVisible: true,
  },
  NEWS: {
    key: 'NEWS',
    labelKey: 'bilgiKartlariPage.pages.NEWS',
    route: '/app/news',
    userVisible: true,
  },
  SIMULATION: {
    key: 'SIMULATION',
    labelKey: 'bilgiKartlariPage.pages.SIMULATION',
    route: '/app/simulation',
    userVisible: true,
  },
  PROFILE: {
    key: 'PROFILE',
    labelKey: 'bilgiKartlariPage.pages.PROFILE',
    route: '/app/profile',
    userVisible: true,
  },
  DASHBOARD: {
    key: 'DASHBOARD',
    labelKey: 'bilgiKartlariPage.pages.DASHBOARD',
    route: '/app/dashboard',
    userVisible: true,
  },
  ADMIN: {
    key: 'ADMIN',
    labelKey: 'bilgiKartlariPage.pages.ADMIN',
    route: '/admin',
    userVisible: false,
    adminOnly: true,
  },
  ADMIN_KPI_USERS: {
    key: 'ADMIN_KPI_USERS',
    labelKey: 'bilgiKartlariPage.pages.ADMIN_KPI_USERS',
    route: '/admin/kpi/total-users',
    userVisible: false,
    adminOnly: true,
  },
  ADMIN_KPI_PORTFOLIOS: {
    key: 'ADMIN_KPI_PORTFOLIOS',
    labelKey: 'bilgiKartlariPage.pages.ADMIN_KPI_PORTFOLIOS',
    route: '/admin/kpi/active-portfolios',
    userVisible: false,
    adminOnly: true,
  },
  ADMIN_KPI_MARKETS: {
    key: 'ADMIN_KPI_MARKETS',
    labelKey: 'bilgiKartlariPage.pages.ADMIN_KPI_MARKETS',
    route: '/admin/kpi/market-streams',
    userVisible: false,
    adminOnly: true,
  },
  ADMIN_KPI_NEWS: {
    key: 'ADMIN_KPI_NEWS',
    labelKey: 'bilgiKartlariPage.pages.ADMIN_KPI_NEWS',
    route: '/admin/kpi/news-sources',
    userVisible: false,
    adminOnly: true,
  },
  ADMIN_KPI_SYSTEM: {
    key: 'ADMIN_KPI_SYSTEM',
    labelKey: 'bilgiKartlariPage.pages.ADMIN_KPI_SYSTEM',
    route: '/admin/kpi/system-status',
    userVisible: false,
    adminOnly: true,
  },
  ADMIN_KPI_LATENCY: {
    key: 'ADMIN_KPI_LATENCY',
    labelKey: 'bilgiKartlariPage.pages.ADMIN_KPI_LATENCY',
    route: '/admin/kpi/avg-latency',
    userVisible: false,
    adminOnly: true,
  },
  INFO_CARDS: {
    key: 'INFO_CARDS',
    labelKey: 'bilgiKartlariPage.pages.INFO_CARDS',
    route: '/app/bilgi-kartlari',
    userVisible: false,
    adminOnly: true,
  },
  NOTIFICATIONS: {
    key: 'NOTIFICATIONS',
    labelKey: 'bilgiKartlariPage.pages.PROFILE',
    route: '/app/profile',
    userVisible: false,
  },
  AUDIT_LOGS: {
    key: 'AUDIT_LOGS',
    labelKey: 'bilgiKartlariPage.pages.ADMIN',
    route: '/admin',
    userVisible: false,
    adminOnly: true,
  },
}

/** Header nav order + secondary app routes. */
const USER_PAGE_ORDER: PortalPageKey[] = [
  'TURKEY_ECONOMY',
  'FINANCIAL_LITERACY',
  'BANK_RATES',
  'MARKETS',
  'FAIZ_VADELI',
  'PORTFOLIO',
  'ANALYSIS',
  'NEWS',
  'SIMULATION',
  'PROFILE',
  'DASHBOARD',
]

export const PORTAL_PAGES: PortalPageDef[] = Object.values(PORTAL_PAGE_DEFS)

/** Pages where end users see info cards (?+ pick + help mode). */
export const USER_PORTAL_PAGES = USER_PAGE_ORDER.map((key) => PORTAL_PAGE_DEFS[key])

export const USER_PORTAL_PAGE_KEYS = new Set(USER_PORTAL_PAGES.map((p) => p.key))

export function isUserPortalPageKey(key: PortalPageKey): boolean {
  return USER_PORTAL_PAGE_KEYS.has(key)
}

const LEGACY_PAGE_ALIASES: Partial<Record<PortalPageKey, PortalPageKey[]>> = {
  PROFILE: ['NOTIFICATIONS'],
  ADMIN: ['AUDIT_LOGS'],
}

/** Count cards for sidebar, including legacy page keys. */
export function countCardsForPortalPage(
  cards: { pages: PortalPageKey[] }[],
  pageKey: PortalPageKey,
): number {
  const keys = new Set<PortalPageKey>([pageKey, ...(LEGACY_PAGE_ALIASES[pageKey] ?? [])])
  return cards.filter((card) => card.pages.some((p) => keys.has(p))).length
}

export function resolvePageKeyFromPath(pathname: string): PortalPageKey | null {
  const normalized = pathname.replace(/\/+$/, '') || '/'
  const sorted = [...USER_PORTAL_PAGES].sort((a, b) => b.route.length - a.route.length)
  for (const page of sorted) {
    if (normalized === page.route || normalized.startsWith(`${page.route}/`)) {
      return page.key
    }
  }
  return null
}

export function getPortalPageRoute(key: PortalPageKey): string {
  return PORTAL_PAGE_DEFS[key]?.route ?? '/app'
}
