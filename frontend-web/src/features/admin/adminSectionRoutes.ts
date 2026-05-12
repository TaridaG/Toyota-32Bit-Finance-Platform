/** Routes and i18n keys for admin dashboard sections (sidebar + detail pages). */

/** Dedicated KPI page with live API metrics (not generic placeholder). */
export const ADMIN_KPI_TOTAL_USERS_PATH = 'kpi/total-users'

export const ADMIN_SECTION_ROUTES = [
  {
    path: ADMIN_KPI_TOTAL_USERS_PATH,
    titleKey: 'dashboard.kpi.totalUsers',
    leadKey: 'sectionPages.kpiTotalUsers',
  },
  {
    path: 'kpi/active-portfolios',
    titleKey: 'dashboard.kpi.activePortfolios',
    leadKey: 'sectionPages.kpiActivePortfolios',
  },
  {
    path: 'kpi/market-streams',
    titleKey: 'dashboard.kpi.marketStreams',
    leadKey: 'sectionPages.kpiMarketStreams',
  },
  {
    path: 'kpi/news-sources',
    titleKey: 'dashboard.kpi.newsArticles',
    leadKey: 'sectionPages.kpiNewsSources',
  },
  {
    path: 'kpi/system-status',
    titleKey: 'dashboard.kpi.systemStatus',
    leadKey: 'sectionPages.kpiSystemStatus',
  },
  {
    path: 'kpi/avg-latency',
    titleKey: 'dashboard.kpi.avgLatency',
    leadKey: 'sectionPages.kpiAvgLatency',
  },
] as const

export type AdminSectionRoute = (typeof ADMIN_SECTION_ROUTES)[number]

export const ADMIN_SIDEBAR_LINKS: { to: string; labelKey: string; end?: boolean }[] = [
  { to: '/admin', labelKey: 'dashboard.title', end: true },
  ...ADMIN_SECTION_ROUTES.map((r) => ({
    to: `/admin/${r.path}`,
    labelKey: r.titleKey,
  })),
]

const KPI_PATH_BY_ID: Record<string, string> = {
  users: '/admin/kpi/total-users',
  portfolios: '/admin/kpi/active-portfolios',
  marketStreams: '/admin/kpi/market-streams',
  news: '/admin/kpi/news-sources',
  system: '/admin/kpi/system-status',
  latency: '/admin/kpi/avg-latency',
}

export function adminKpiSectionPath(kpiId: string): string | undefined {
  return KPI_PATH_BY_ID[kpiId]
}
