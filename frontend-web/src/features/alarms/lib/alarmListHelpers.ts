import type { AlarmItem } from '../api/alarmApi'

export function sortAlarmsNewestFirst(items: AlarmItem[]): AlarmItem[] {
  return [...items].sort((a, b) => Date.parse(b.createdAt ?? '') - Date.parse(a.createdAt ?? ''))
}

export const ALARM_HEADER_PREVIEW = 3

export function formatExtraAlarmCount(total: number): string | null {
  const extra = total - ALARM_HEADER_PREVIEW
  if (extra <= 0) {
    return null
  }
  return extra > 99 ? '99+' : String(extra)
}
