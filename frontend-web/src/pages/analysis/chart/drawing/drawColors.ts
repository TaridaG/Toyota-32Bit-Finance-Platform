import type { DrawTool } from '../../types'

export const DEFAULT_DRAW_COLOR = '#f59e0b'

export type DrawableTool = Exclude<DrawTool, 'none'>

export const DRAW_COLOR_PALETTE: readonly string[] = [
  '#f59e0b',
  '#38bdf8',
  '#22c55e',
  '#ef4444',
  '#a855f7',
  '#ec4899',
  '#14b8a6',
  '#f97316',
  '#e2e8f0',
  '#facc15',
]

export const DEFAULT_DRAW_COLORS: Record<DrawableTool, string> = {
  trendline: '#f59e0b',
  ray: '#38bdf8',
  hline: '#22c55e',
  vline: '#a855f7',
  rect: '#ec4899',
  fib: '#14b8a6',
  point: '#f97316',
}

export function normalizeDrawColor(color: string | undefined | null): string {
  if (typeof color === 'string' && /^#[0-9a-fA-F]{6}$/.test(color.trim())) {
    return color.trim()
  }
  return DEFAULT_DRAW_COLOR
}

export function withAlpha(hex: string, alpha: number): string {
  const normalized = normalizeDrawColor(hex).slice(1)
  const r = Number.parseInt(normalized.slice(0, 2), 16)
  const g = Number.parseInt(normalized.slice(2, 4), 16)
  const b = Number.parseInt(normalized.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

export function createDefaultDrawColorsByTool(): Record<DrawableTool, string> {
  return { ...DEFAULT_DRAW_COLORS }
}

function clamp01(value: number): number {
  return Math.min(1, Math.max(0, value))
}

export function hexFromHsl(hue: number, saturation = 100, lightness = 50): string {
  const h = ((hue % 360) + 360) % 360
  const s = clamp01(saturation / 100)
  const l = clamp01(lightness / 100)
  const c = (1 - Math.abs(2 * l - 1)) * s
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
  const m = l - c / 2
  let r = 0
  let g = 0
  let b = 0
  if (h < 60) {
    r = c
    g = x
  } else if (h < 120) {
    r = x
    g = c
  } else if (h < 180) {
    g = c
    b = x
  } else if (h < 240) {
    g = x
    b = c
  } else if (h < 300) {
    r = x
    b = c
  } else {
    r = c
    b = x
  }
  const toHex = (channel: number) =>
    Math.round((channel + m) * 255)
      .toString(16)
      .padStart(2, '0')
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`
}

export function hueFromHex(hex: string): number {
  const normalized = normalizeDrawColor(hex).slice(1)
  const r = Number.parseInt(normalized.slice(0, 2), 16) / 255
  const g = Number.parseInt(normalized.slice(2, 4), 16) / 255
  const b = Number.parseInt(normalized.slice(4, 6), 16) / 255
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const delta = max - min
  if (delta === 0) return 0
  let hue = 0
  if (max === r) {
    hue = ((g - b) / delta) % 6
  } else if (max === g) {
    hue = (b - r) / delta + 2
  } else {
    hue = (r - g) / delta + 4
  }
  return clamp01(hue / 6) * 360
}

export function hexFromSliderPosition(position: number, saturation = 100, lightness = 50): string {
  return hexFromHsl(position * 360, saturation, lightness)
}

export function sliderPositionFromHex(hex: string): number {
  return clamp01(hueFromHex(hex) / 360)
}
