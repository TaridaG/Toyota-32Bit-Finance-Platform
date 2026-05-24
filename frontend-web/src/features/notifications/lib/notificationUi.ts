import type { AlarmCondition } from '../../alarms/api/alarmApi'
import { formatAlarmCondition } from '../../alarms/lib/alarmUi'
import type { PortalNotification } from '../api/notificationApi'

/** Legacy inbox rows saved before locale-aware defaults (match backend English fallbacks). */
const LEGACY_SYSTEM_INBOX_BODIES = new Set([
  'Your account access has been restored.',
  'Your Finance Portal account has been suspended. Contact support if you have questions.',
  'Your Finance Portal account has been permanently removed by an administrator.',
])

function isLegacySystemInboxBody(body: string): boolean {
  const trimmed = body.trim()
  if (LEGACY_SYSTEM_INBOX_BODIES.has(trimmed)) {
    return true
  }
  return trimmed.startsWith('Your Finance Portal account has been permanently removed by an administrator.')
}

export function formatNotificationType(
  type: string,
  t: (key: string) => string,
): string {
  switch (type) {
    case 'ALARM':
      return t('type.alarm')
    case 'ADMIN_MESSAGE':
      return t('type.adminMessage')
    case 'ACCOUNT_FROZEN':
      return t('type.accountFrozen')
    case 'ACCOUNT_UNFROZEN':
      return t('type.accountUnfrozen')
    default:
      return type
  }
}

export function formatNotificationDetail(
  item: PortalNotification,
  tNotify: (key: string, opts?: Record<string, unknown>) => string,
  tAlarms: (key: string, opts?: Record<string, unknown>) => string,
): string {
  if (item.type === 'ADMIN_MESSAGE') {
    if (item.body?.trim()) {
      return item.body.trim()
    }
    return formatNotificationType(item.type, tNotify)
  }
  if (item.type === 'ACCOUNT_FROZEN') {
    const body = item.body?.trim()
    if (body && !isLegacySystemInboxBody(body)) {
      return body
    }
    return tNotify('defaultBody.accountFrozen')
  }
  if (item.type === 'ACCOUNT_UNFROZEN') {
    const body = item.body?.trim()
    if (body && !isLegacySystemInboxBody(body)) {
      return body
    }
    return tNotify('defaultBody.accountUnfrozen')
  }
  const threshold = item.threshold ?? 0
  const condition = (item.condition ?? 'ABOVE') as AlarmCondition
  const conditionLabel = formatAlarmCondition(condition, threshold, tAlarms)
  const price =
    item.price != null && Number.isFinite(item.price) ? String(item.price) : '—'
  return tNotify('detailLine', {
    condition: conditionLabel,
    price,
  })
}
