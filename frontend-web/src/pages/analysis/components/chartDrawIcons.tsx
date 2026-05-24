import type { ReactElement } from 'react'
import type { DrawTool } from '../types'

export function DrawIconNone() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M6 3l2.5 12.5L11 12l4 8 2-1-4.5-8.5L17 9 6 3z"
        fill="currentColor"
        stroke="currentColor"
        strokeWidth="0.5"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export function DrawIconTrend() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M4 18L10 10l4 4 6-8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function DrawIconRay() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M4 17 L14 9" strokeLinecap="round" />
      <path d="M14 9 L20 5" strokeLinecap="round" />
      <circle cx="4" cy="17" r="1.5" fill="currentColor" stroke="none" />
    </svg>
  )
}

export function DrawIconHline() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M4 12h16" strokeLinecap="round" />
      <path d="M8 7h8M8 17h8" strokeOpacity="0.35" strokeLinecap="round" />
    </svg>
  )
}

export function DrawIconVline() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M12 4v16" strokeLinecap="round" />
      <path d="M7 8v8M17 8v8" strokeOpacity="0.35" strokeLinecap="round" />
    </svg>
  )
}

export function DrawIconRect() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <rect x="5" y="7" width="14" height="10" rx="1.5" />
    </svg>
  )
}

export function DrawIconFib() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
      <path d="M5 6h14M5 10h14M5 14h14M5 18h14" strokeLinecap="round" strokeOpacity="0.55" />
      <path d="M7 6v12" strokeLinecap="round" />
      <text x="9" y="9" fill="currentColor" stroke="none" fontSize="6" fontWeight="700">
        61.8
      </text>
    </svg>
  )
}

export function DrawIconPoint() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <circle cx="12" cy="11" r="3" />
      <path d="M12 14v5M9 21h6" strokeLinecap="round" />
    </svg>
  )
}

export function DrawIconTrash() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M4 7h16" strokeLinecap="round" />
      <path d="M9 7V5h6v2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M8 7l1 12h6l1-12" strokeLinejoin="round" />
    </svg>
  )
}

export function DrawIconPlus() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M12 5v14M5 12h14" strokeLinecap="round" />
    </svg>
  )
}

export function IconChartMeasure() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M5 19V5" strokeLinecap="round" strokeDasharray="3 2" />
      <path d="M19 19V9" strokeLinecap="round" strokeDasharray="3 2" />
      <path d="M5 12h14" strokeLinecap="round" strokeDasharray="4 3" />
      <path d="M5 12l3-2.5M5 12l3 2.5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M19 9l-3-2.5M19 9l-3 2.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function DrawIconHistory() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M12 8v4l3 2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M3 12a9 9 0 1 0 3-6.7" strokeLinecap="round" />
      <path d="M3 4v5h5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export const CHART_DRAW_TOOLS: { id: DrawTool; Icon: () => ReactElement }[] = [
  { id: 'none', Icon: DrawIconNone },
  { id: 'trendline', Icon: DrawIconTrend },
  { id: 'ray', Icon: DrawIconRay },
  { id: 'hline', Icon: DrawIconHline },
  { id: 'vline', Icon: DrawIconVline },
  { id: 'rect', Icon: DrawIconRect },
  { id: 'fib', Icon: DrawIconFib },
  { id: 'point', Icon: DrawIconPoint },
]
