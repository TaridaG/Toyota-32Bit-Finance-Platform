import { useMemo, useState } from 'react'

export type AdminDailyComboRow = {
  date: string
  barValue: number
  rollingAverage7d: number
  deltaVsPreviousDay: number
}

export type AdminDailyComboChartLabels = {
  empty: string
  aria: string
  tipPrimary: string
  tipAvg: string
  tipDelta: string
}

type Props = {
  rows: AdminDailyComboRow[]
  locale: string
  labels: AdminDailyComboChartLabels
}

function utcTodayIsoDate(): string {
  return new Date().toISOString().slice(0, 10)
}

export function AdminDailyComboChart({ rows, locale, labels }: Props) {
  const [tip, setTip] = useState<{
    x: number
    y: number
    d: AdminDailyComboRow
  } | null>(null)
  const today = utcTodayIsoDate()

  const n = rows.length
  const dense = n > 14
  const W = 960
  const H = dense ? 168 : 152
  const padL = 36
  const padR = 10
  const padT = 8
  const padB = dense ? 30 : 22

  const { maxY, bars, linePts } = useMemo(() => {
    if (rows.length === 0) {
      return { maxY: 1, bars: [] as { x: number; h: number; w: number; d: AdminDailyComboRow }[], linePts: '' }
    }
    const innerW = W - padL - padR
    const innerH = H - padT - padB
    const maxRaw = Math.max(1, ...rows.map((r) => Math.max(r.barValue, r.rollingAverage7d)))
    const maxYVal = maxRaw * 1.04
    const bw = innerW / n
    const barW = Math.max(2, bw * 0.55)
    const barsLocal = rows.map((d, i) => {
      const cx = padL + i * bw + bw / 2
      const h = (d.barValue / maxYVal) * innerH
      return { x: cx - barW / 2, h: Math.max(0, h), w: barW, d }
    })
    const pts = rows
      .map((d, i) => {
        const cx = padL + i * bw + bw / 2
        const y = padT + innerH - (d.rollingAverage7d / maxYVal) * innerH
        return `${cx},${y}`
      })
      .join(' ')
    return { maxY: maxYVal, bars: barsLocal, linePts: pts }
  }, [rows, H, padB, n, W, padL, padT])

  const labelStep = Math.max(1, Math.ceil(n / 14))

  if (rows.length === 0) {
    return (
      <div className="fi-admin-ua-chart-empty" role="status">
        <p>{labels.empty}</p>
      </div>
    )
  }

  return (
    <div className="fi-admin-ua-chart-wrap">
      <svg
        className="fi-admin-ua-chart-svg"
        viewBox={`0 0 ${W} ${H}`}
        preserveAspectRatio="xMidYMid meet"
        role="img"
        aria-label={labels.aria}
        onMouseLeave={() => setTip(null)}
      >
        <line
          x1={padL}
          y1={H - padB}
          x2={W - 10}
          y2={H - padB}
          className="fi-admin-ua-chart-axis"
        />
        {bars.map((b) => {
          const barTop = H - padB - b.h
          const isToday = b.d.date === today
          return (
            <rect
              key={b.d.date}
              x={b.x}
              y={barTop}
              width={b.w}
              height={Math.max(0, b.h)}
              rx={3}
              className={`fi-admin-ua-chart-bar${isToday ? ' fi-admin-ua-chart-bar--today' : ''}`}
              onMouseEnter={(e) => {
                const rect = (e.target as SVGRectElement).closest('svg')?.getBoundingClientRect()
                if (!rect) return
                setTip({ x: e.clientX - rect.left, y: e.clientY - rect.top, d: b.d })
              }}
              onMouseMove={(e) => {
                const rect = (e.target as SVGRectElement).closest('svg')?.getBoundingClientRect()
                if (!rect) return
                setTip({ x: e.clientX - rect.left, y: e.clientY - rect.top, d: b.d })
              }}
            />
          )
        })}
        {rows.length >= 2 && linePts ? (
          <polyline
            fill="none"
            className="fi-admin-ua-chart-avg-line"
            strokeWidth={2}
            strokeDasharray="5 4"
            points={linePts}
          />
        ) : null}
        {rows.map((d, i) => {
          const showTick = i === 0 || i === n - 1 || i % labelStep === 0
          if (!showTick) return null
          const bw = (W - padL - 10) / n
          const cx = padL + i * bw + bw / 2
          const short = new Date(d.date + 'T12:00:00Z').toLocaleDateString(locale, {
            month: dense ? 'numeric' : 'short',
            day: 'numeric',
            timeZone: 'UTC',
          })
          return (
            <text
              key={`${d.date}-lbl`}
              x={cx}
              y={H - 8}
              textAnchor="middle"
              className={`fi-admin-ua-chart-xlabel${dense ? ' fi-admin-ua-chart-xlabel--dense' : ''}`}
            >
              {short}
            </text>
          )
        })}
        <text x={4} y={padT + 8} className="fi-admin-ua-chart-ylabel">
          {Math.round(maxY)}
        </text>
      </svg>
      {tip ? (
        <div
          className="fi-admin-ua-chart-tip"
          style={{ left: Math.min(Math.max(tip.x + 12, 8), 280), top: Math.max(tip.y - 72, 4) }}
        >
          <div className="fi-admin-ua-chart-tip-date">
            {new Date(tip.d.date + 'T12:00:00Z').toLocaleDateString(locale, {
              weekday: 'short',
              year: 'numeric',
              month: 'short',
              day: 'numeric',
              timeZone: 'UTC',
            })}
          </div>
          <div>
            {labels.tipPrimary}: <strong>{tip.d.barValue}</strong>
          </div>
          <div>
            {labels.tipAvg}: <strong>{tip.d.rollingAverage7d.toFixed(2)}</strong>
          </div>
          <div>
            {labels.tipDelta}:{' '}
            <strong>
              {tip.d.deltaVsPreviousDay >= 0 ? '+' : ''}
              {tip.d.deltaVsPreviousDay}
            </strong>
          </div>
        </div>
      ) : null}
    </div>
  )
}
