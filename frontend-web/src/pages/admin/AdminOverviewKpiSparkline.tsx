import type { AdminKpiAccent } from '../../features/admin/types'

export function KpiSparkline({ values, accent }: { values: number[]; accent: AdminKpiAccent }) {
  if (values.length < 2) return null
  const min = Math.min(...values)
  const max = Math.max(...values)
  const padX = 1.5
  const padY = 3
  const range = Math.max(1e-6, max - min)
  const h = 24
  const w = 100
  const pts = values
    .map((v, i) => {
      const x = padX + (i / (values.length - 1)) * (w - 2 * padX)
      const yn = (v - min) / range
      const y = h - padY - yn * (h - 2 * padY)
      return `${x},${y}`
    })
    .join(' ')
  return (
    <svg className="fi-admin-dash-kpi-spark" viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none" aria-hidden>
      <polyline
        className={`fi-admin-dash-kpi-spark-line fi-admin-dash-kpi-spark-line--${accent}`}
        fill="none"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        points={pts}
      />
    </svg>
  )
}
