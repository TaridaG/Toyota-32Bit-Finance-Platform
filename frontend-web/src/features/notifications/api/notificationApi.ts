import { apiClient } from '../../../shared/api/client'

export type PortalNotification = {
  id: number
  type: string
  title?: string | null
  body?: string | null
  instrumentSymbol?: string | null
  condition?: string | null
  threshold: number | null
  price?: number | null
  triggeredAt: string
  read: boolean
}

export type NotificationPage = {
  content: PortalNotification[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  unreadCount: number
}

type ApiEnvelope<T> = {
  success?: boolean
  data?: T
}

function unwrap<T>(payload: ApiEnvelope<T> | T): T | null {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    const envelope = payload as ApiEnvelope<T>
    return envelope.data ?? null
  }
  return (payload as T) ?? null
}

export async function fetchNotificationsPage(
  page = 0,
  size = 20,
): Promise<NotificationPage> {
  const { data } = await apiClient.get<ApiEnvelope<NotificationPage>>('/api/notifications', {
    params: { page, size },
  })
  const body = unwrap(data)
  return {
    content: body?.content ?? [],
    page: body?.page ?? page,
    size: body?.size ?? size,
    totalElements: body?.totalElements ?? 0,
    totalPages: body?.totalPages ?? 0,
    unreadCount: body?.unreadCount ?? 0,
  }
}

export async function fetchNotificationPreview(limit = 8): Promise<NotificationPage> {
  return fetchNotificationsPage(0, limit)
}

export async function markNotificationRead(id: number): Promise<PortalNotification | null> {
  const { data } = await apiClient.patch<ApiEnvelope<PortalNotification>>(
    `/api/notifications/${id}/read`,
  )
  return unwrap(data)
}

export async function markAllNotificationsRead(): Promise<void> {
  await apiClient.patch('/api/notifications/read-all')
}

export async function deleteNotification(id: number): Promise<void> {
  await apiClient.delete(`/api/notifications/${id}`)
}

/** Header bell preview listens for this after inbox changes. */
export function notifyNotificationsChanged(): void {
  window.dispatchEvent(new CustomEvent('finance-notifications-changed'))
}
