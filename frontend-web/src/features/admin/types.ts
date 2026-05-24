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

export type AdminOverviewMock = {
  dashboardKpis: AdminDashboardKpi[]
}
