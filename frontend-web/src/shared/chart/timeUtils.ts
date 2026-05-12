import type { Time } from 'lightweight-charts'

export function timeToMs(t: Time): number {
  if (typeof t === 'number') return t * 1000
  if (typeof t === 'string') {
    const ms = Date.parse(t)
    return Number.isFinite(ms) ? ms : NaN
  }
  const o = t as { year: number; month: number; day: number }
  if (o && typeof o.year === 'number' && typeof o.month === 'number' && typeof o.day === 'number') {
    return Date.UTC(o.year, o.month - 1, o.day)
  }
  return NaN
}

export function timeToUnixSec(t: Time): number {
  return Math.floor(timeToMs(t) / 1000)
}
