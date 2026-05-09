export type AdminKpiAccent = 'blue' | 'green' | 'cyan' | 'purple' | 'amber'

export type AdminDashboardKpi =
  | {
      variant: 'trend'
      id: string
      labelKey: string
      value: string
      deltaPctKey: string
      trend: 'up' | 'down'
      sparkline: number[]
      accent: AdminKpiAccent
    }
  | {
      variant: 'streams'
      id: string
      labelKey: string
      total: number
      active: number
      passive: number
      sparkline: number[]
      accent: AdminKpiAccent
    }
  | {
      variant: 'news'
      id: string
      labelKey: string
      total: number
      active: number
      errors: number
      sparkline: number[]
      accent: AdminKpiAccent
    }
  | {
      variant: 'system'
      id: string
      labelKey: string
      statusKey: string
      detailKey: string
      /** true = lit segment (healthy) */
      healthSegments: boolean[]
    }
  | {
      variant: 'latency'
      id: string
      labelKey: string
      valueSec: string
      deltaShortKey: string
      trend: 'up' | 'down'
      targetSec: number
      sparkline: number[]
      accent: AdminKpiAccent
    }

export type AdminDataFlowPoint = {
  label: string
  success: number
  error: number
  warn: number
}

export type AdminLatencyBand = {
  bandKey: 'live' | 'good' | 'medium' | 'bad'
  count: number
  percent: number
  color: string
}

export type AdminStreamTableRow = {
  name: string
  categoryKey: string
  status: 'live' | 'good' | 'medium' | 'bad'
  latencyMs: number
  lastUpdate: string
  successRate: number
}

export type AdminNewsStreamRow = {
  name: string
  status: 'live' | 'degraded' | 'error'
  latencyLabel: string
  lastNews: string
  newsCount: number
}

export type AdminOverviewMock = {
  generatedAt: string
  dataFlowSummary: { successPct: number; errorPct: number; warnPct: number }
  dashboardKpis: AdminDashboardKpi[]
  dataFlowSeries: AdminDataFlowPoint[]
  latencyBands: AdminLatencyBand[]
  latencyAverageSec: number
  streamRows: AdminStreamTableRow[]
  newsStreamRows: AdminNewsStreamRow[]
}
