import type { AdminOverviewMock } from '../types'

function kpiSparkline(length: number, phase: number): number[] {
  return Array.from({ length }, (_, i) => {
    const t = i * 0.42 + phase
    return 46 + Math.sin(t) * 22 + Math.cos(t * 0.7) * 12
  })
}

/** Fallback KPI shapes when live admin metrics are loading or unavailable. */
export function getAdminOverviewMock(): AdminOverviewMock {
  return {
    dashboardKpis: [
      {
        variant: 'trend',
        id: 'users',
        labelKey: 'dashboard.kpi.totalUsers',
        value: '12.847',
        deltaPctKey: 'dashboard.kpi.deltaUsersPct',
        trend: 'up',
        sparkline: kpiSparkline(14, 0.2),
        accent: 'blue',
      },
      {
        variant: 'trend',
        id: 'portfolios',
        labelKey: 'dashboard.kpi.activePortfolios',
        value: '8.243',
        deltaPctKey: 'dashboard.kpi.deltaPortfoliosPct',
        trend: 'up',
        sparkline: kpiSparkline(14, 1.1),
        accent: 'green',
      },
      {
        variant: 'streams',
        id: 'marketStreams',
        labelKey: 'dashboard.kpi.marketStreams',
        total: 28,
        active: 26,
        passive: 2,
        sparkline: kpiSparkline(14, 2.4),
        accent: 'cyan',
      },
      {
        variant: 'news',
        id: 'news',
        labelKey: 'dashboard.kpi.newsArticles',
        total: 1842,
        active: 16,
        errors: 1,
        sparkline: [12, 22, 18, 28, 24, 31, 35],
        accent: 'purple',
      },
      {
        variant: 'system',
        id: 'system',
        labelKey: 'dashboard.kpi.systemStatus',
        statusKey: 'dashboard.kpi.statusHealthy',
        detailKey: 'dashboard.kpi.allSystemsRunning',
        healthSegments: [true, true, true, true, false, false],
      },
      {
        variant: 'latency',
        id: 'latency',
        labelKey: 'dashboard.kpi.avgLatency',
        valueSec: '1.42',
        deltaShortKey: 'dashboard.kpi.latencyDeltaShort',
        trend: 'down',
        targetSec: 2,
        sparkline: kpiSparkline(14, 4.8),
        accent: 'amber',
      },
    ],
  }
}
