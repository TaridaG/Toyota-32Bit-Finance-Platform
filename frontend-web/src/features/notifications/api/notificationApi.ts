import { apiClient } from '../../../shared/api/client'

export type NotificationItem = {
  instrumentSymbol: string
  condition: string
  price: number
  triggeredAt: string
}

type NotificationEnvelope = {
  success?: boolean
  data?: NotificationItem[]
}

export async function fetchMyNotifications(): Promise<NotificationItem[]> {
  const { data } = await apiClient.get<NotificationEnvelope | NotificationItem[]>('/api/history/alarms')
  if (Array.isArray(data)) {
    return data
  }
  if (Array.isArray(data?.data)) {
    return data.data
  }
  return []
}
