import { TickMarkType, type Time } from 'lightweight-charts'
import { timeToMs } from './timeUtils'

const DAY = 86_400
const HOUR = 3600

function clip(s: string, max = 16): string {
  const t = s.trim()
  return t.length <= max ? t : t.slice(0, max)
}

/** Dev log: görünür span bandı. */
export function axisBandForVisibleSpan(visibleSpanSec: number): string {
  const s = Math.max(1, visibleSpanSec)
  if (s <= DAY) return '<=1d'
  if (s <= 14 * DAY) return '<=14d'
  if (s <= 120 * DAY) return '<=120d'
  if (s <= 2 * 365 * DAY) return '<=2y'
  return '>2y'
}

/**
 * Görünür span (saniye) → eksen etiketi. Saniye yalnızca çok dar zoom’da.
 * 1Y seçiliyken burada ay/yıl bandı olmalı; saniye asla gösterilmez.
 */
export function formatTradingAxisTick(
  time: Time,
  tickMarkType: TickMarkType,
  locale: string,
  visibleSpanSec: number,
): string | null {
  const ms = timeToMs(time)
  if (!Number.isFinite(ms)) return null
  const d = new Date(ms)
  const span = Math.max(1, visibleSpanSec)

  if (span <= DAY) {
    if (tickMarkType === TickMarkType.Year || tickMarkType === TickMarkType.Month || tickMarkType === TickMarkType.DayOfMonth) {
      return null
    }
    if (span < 2 * HOUR) {
      return clip(
        new Intl.DateTimeFormat(locale, {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
          hour12: false,
        }).format(d),
        12,
      )
    }
    return clip(new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit', hour12: false }).format(d), 8)
  }

  if (span <= 14 * DAY) {
    if (tickMarkType === TickMarkType.Time || tickMarkType === TickMarkType.TimeWithSeconds) return null
    if (tickMarkType === TickMarkType.Year) return null
    return clip(new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short' }).format(d), 12)
  }

  if (span <= 120 * DAY) {
    if (tickMarkType === TickMarkType.Time || tickMarkType === TickMarkType.TimeWithSeconds) return null
    if (tickMarkType === TickMarkType.Year) return null
    if (tickMarkType === TickMarkType.Month) {
      return clip(new Intl.DateTimeFormat(locale, { month: 'short' }).format(d), 6)
    }
    return clip(new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short' }).format(d), 12)
  }

  if (span <= 2 * 365 * DAY) {
    if (tickMarkType === TickMarkType.Time || tickMarkType === TickMarkType.TimeWithSeconds) return null
    if (tickMarkType === TickMarkType.DayOfMonth) return null
    return clip(new Intl.DateTimeFormat(locale, { month: 'short', year: 'numeric' }).format(d), 14)
  }

  if (tickMarkType === TickMarkType.DayOfMonth || tickMarkType === TickMarkType.Time || tickMarkType === TickMarkType.TimeWithSeconds) {
    return null
  }
  if (tickMarkType === TickMarkType.Month && d.getMonth() !== 0) return null
  return clip(new Intl.DateTimeFormat(locale, { year: 'numeric' }).format(d), 8)
}

export function formatTradingCrosshairTime(visibleSpanSec: number, locale: string, t: Time): string {
  const ms = timeToMs(t)
  if (!Number.isFinite(ms)) return ''
  const d = new Date(ms)
  const span = Math.max(1, visibleSpanSec)

  if (span <= DAY) {
    if (span < 2 * HOUR) {
      return new Intl.DateTimeFormat(locale, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false,
      }).format(d)
    }
    return new Intl.DateTimeFormat(locale, {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(d)
  }
  if (span <= 14 * DAY) {
    return new Intl.DateTimeFormat(locale, { weekday: 'short', day: 'numeric', month: 'short' }).format(d)
  }
  if (span <= 120 * DAY) {
    return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short', year: 'numeric' }).format(d)
  }
  if (span <= 2 * 365 * DAY) {
    return new Intl.DateTimeFormat(locale, { month: 'short', year: 'numeric', day: 'numeric' }).format(d)
  }
  return new Intl.DateTimeFormat(locale, { year: 'numeric', month: 'short' }).format(d)
}
