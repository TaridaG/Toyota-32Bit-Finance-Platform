import type { AdminKpiAccent } from '../../features/admin/types'

function seriesToPoints(values: number[], w: number, h: number, padX: number, padY: number): string {
  if (values.length < 2) return ''
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = Math.max(1e-6, max - min)
  return values
    .map((v, i) => {
      const x = padX + (i / (values.length - 1)) * (w - 2 * padX)
      const yn = (v - min) / range
      const y = h - padY - yn * (h - 2 * padY)
      return `${x},${y}`
    })
    .join(' ')
}

export function KpiSparkline({
  values,
  accent,
  secondaryValues,
}: {
  values: number[]
  accent: AdminKpiAccent
  /** Same length as {@link values}; drawn muted (e.g. deletions vs signups). */
  secondaryValues?: number[]
}) {
  if (values.length < 2) return null
  const padX = 1.5
  const padY = 3
  const h = 24
  const w = 100
  const pts = seriesToPoints(values, w, h, padX, padY)
  const sec =
    secondaryValues != null &&
    secondaryValues.length === values.length &&
    secondaryValues.length >= 2
      ? seriesToPoints(secondaryValues, w, h, padX, padY)
      : null
  return (
    <svg className="fi-admin-dash-kpi-spark" viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none" aria-hidden>
      {sec ? (
        <polyline
          className="fi-admin-dash-kpi-spark-line fi-admin-dash-kpi-spark-line--secondary"
          fill="none"
          strokeWidth="1.75"
          strokeLinecap="round"
          strokeLinejoin="round"
          points={sec}
        />
      ) : null}
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
