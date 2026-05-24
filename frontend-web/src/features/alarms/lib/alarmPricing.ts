import type { AlarmCondition } from '../api/alarmApi'

export function parseDecimalInput(raw: string): number | null {
  const normalized = raw.trim().replace(',', '.')
  if (!normalized) {
    return null
  }
  const value = Number(normalized)
  return Number.isFinite(value) ? value : null
}

export function formatPriceInput(value: number, maxFractionDigits = 6): string {
  if (!Number.isFinite(value)) {
    return ''
  }
  return value.toLocaleString(undefined, {
    maximumFractionDigits: maxFractionDigits,
    useGrouping: false,
  })
}

export function formatPercentInput(value: number): string {
  if (!Number.isFinite(value)) {
    return ''
  }
  const rounded = Math.round(value * 100) / 100
  return rounded.toLocaleString(undefined, {
    maximumFractionDigits: 2,
    useGrouping: false,
  })
}

export function percentFromPrices(current: number, target: number): number | null {
  if (!Number.isFinite(current) || current <= 0 || !Number.isFinite(target)) {
    return null
  }
  return ((target - current) / current) * 100
}

export function targetFromPercent(current: number, percent: number): number | null {
  if (!Number.isFinite(current) || current <= 0 || !Number.isFinite(percent)) {
    return null
  }
  return current * (1 + percent / 100)
}

export function resolvePriceAlarmCondition(current: number, target: number): 'GREATER_THAN' | 'LESS_THAN' {
  if (!Number.isFinite(target) || !Number.isFinite(current)) {
    return 'GREATER_THAN'
  }
  return target >= current ? 'GREATER_THAN' : 'LESS_THAN'
}

export function isPercentAlarmCondition(condition: AlarmCondition): boolean {
  return condition === 'PERCENT_CHANGE_UP' || condition === 'PERCENT_CHANGE_DOWN'
}

export function displayAlarmTargetPrice(
  condition: AlarmCondition,
  threshold: number,
  currentPrice: number | null | undefined,
): number | null {
  if (isPercentAlarmCondition(condition)) {
    if (currentPrice == null || !Number.isFinite(currentPrice) || currentPrice <= 0) {
      return null
    }
    const pct = condition === 'PERCENT_CHANGE_UP' ? threshold : -threshold
    return targetFromPercent(currentPrice, pct)
  }
  return Number.isFinite(threshold) ? threshold : null
}
