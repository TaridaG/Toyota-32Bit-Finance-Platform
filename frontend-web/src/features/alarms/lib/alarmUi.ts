import type { AlarmCondition } from '../api/alarmApi'
import { displayAlarmTargetPrice, formatPriceInput, isPercentAlarmCondition } from './alarmPricing'

export type AlarmFormKind = 'PRICE_ABOVE' | 'PRICE_BELOW' | 'PERCENT_UP' | 'PERCENT_DOWN'

export function alarmKindToCondition(kind: AlarmFormKind): AlarmCondition {
  switch (kind) {
    case 'PRICE_ABOVE':
      return 'GREATER_THAN'
    case 'PRICE_BELOW':
      return 'LESS_THAN'
    case 'PERCENT_UP':
      return 'PERCENT_CHANGE_UP'
    case 'PERCENT_DOWN':
      return 'PERCENT_CHANGE_DOWN'
    default:
      return 'GREATER_THAN'
  }
}

export function formatAlarmCondition(
  condition: AlarmCondition,
  threshold: number,
  t: (key: string, opts?: Record<string, unknown>) => string,
): string {
  const value = Number.isFinite(threshold) ? String(threshold) : '—'
  switch (condition) {
    case 'GREATER_THAN':
      return t('alarms.condition.priceAbove', { value })
    case 'LESS_THAN':
      return t('alarms.condition.priceBelow', { value })
    case 'PERCENT_CHANGE_UP':
      return t('alarms.condition.percentUp', { value })
    case 'PERCENT_CHANGE_DOWN':
      return t('alarms.condition.percentDown', { value })
    case 'EQUAL':
      return t('alarms.condition.equal', { value })
    default:
      return `${condition} ${value}`
  }
}

export function formatAlarmPricePair(
  condition: AlarmCondition,
  threshold: number,
  currentPrice: number | null | undefined,
  t: (key: string, opts?: Record<string, unknown>) => string,
): { current: string; target: string } {
  const current =
    currentPrice != null && Number.isFinite(currentPrice) && currentPrice > 0
      ? formatPriceInput(currentPrice)
      : '—'
  const targetValue = displayAlarmTargetPrice(condition, threshold, currentPrice)
  const target =
    targetValue != null
      ? formatPriceInput(targetValue)
      : isPercentAlarmCondition(condition)
        ? t('alarms.list.percentOnly', { value: threshold })
        : formatPriceInput(threshold)
  return { current, target }
}
