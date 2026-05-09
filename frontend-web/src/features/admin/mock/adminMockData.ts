import type { AdminOverviewMock } from '../types'

function kpiSparkline(length: number, phase: number): number[] {
  return Array.from({ length }, (_, i) => {
    const t = i * 0.42 + phase
    return 46 + Math.sin(t) * 22 + Math.cos(t * 0.7) * 12
  })
}

function hoursLast24(): { label: string; success: number; error: number; warn: number }[] {
  const out: { label: string; success: number; error: number; warn: number }[] = []
  for (let h = 0; h < 24; h += 2) {
    const phase = h / 24
    out.push({
      label: `${h.toString().padStart(2, '0')}:00`,
      success: 98.4 + Math.sin(phase * Math.PI * 2) * 0.35,
      error: 1.05 + Math.cos(phase * Math.PI) * 0.12,
      warn: 0.15 + Math.sin(phase * Math.PI * 3) * 0.06,
    })
  }
  return out
}

export function getAdminOverviewMock(): AdminOverviewMock {
  return {
    generatedAt: new Date().toISOString(),
    dataFlowSummary: { successPct: 98.7, errorPct: 1.1, warnPct: 0.2 },
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
        labelKey: 'dashboard.kpi.newsSources',
        total: 16,
        active: 15,
        errors: 1,
        sparkline: kpiSparkline(14, 3.6),
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
    dataFlowSeries: hoursLast24(),
    latencyBands: [
      { bandKey: 'live', count: 12, percent: 42.9, color: '#22c55e' },
      { bandKey: 'good', count: 10, percent: 35.7, color: '#3b82f6' },
      { bandKey: 'medium', count: 4, percent: 14.3, color: '#f59e0b' },
      { bandKey: 'bad', count: 2, percent: 7.1, color: '#ef4444' },
    ],
    latencyAverageSec: 1.42,
    streamRows: [
      {
        name: 'Binance API',
        categoryKey: 'dashboard.category.crypto',
        status: 'live',
        latencyMs: 320,
        lastUpdate: '2026-05-08T14:42:10Z',
        successRate: 99.98,
      },
      {
        name: 'Nasdaq Data Link',
        categoryKey: 'dashboard.category.equity',
        status: 'good',
        latencyMs: 890,
        lastUpdate: '2026-05-08T14:41:55Z',
        successRate: 99.72,
      },
      {
        name: 'Finnhub API',
        categoryKey: 'dashboard.category.equity',
        status: 'live',
        latencyMs: 412,
        lastUpdate: '2026-05-08T14:42:28Z',
        successRate: 99.91,
      },
      {
        name: 'TCMB Kur',
        categoryKey: 'dashboard.category.fx',
        status: 'good',
        latencyMs: 1200,
        lastUpdate: '2026-05-08T12:05:00Z',
        successRate: 100,
      },
    ],
    newsStreamRows: [
      {
        name: 'Bloomberg',
        status: 'live',
        latencyLabel: '45s',
        lastNews: '2026-05-08T14:41:20Z',
        newsCount: 124,
      },
      {
        name: 'Reuters',
        status: 'live',
        latencyLabel: '1m 12s',
        lastNews: '2026-05-08T14:40:08Z',
        newsCount: 98,
      },
      {
        name: 'CNBC',
        status: 'degraded',
        latencyLabel: '2m 05s',
        lastNews: '2026-05-08T14:38:44Z',
        newsCount: 56,
      },
    ],
  }
}
