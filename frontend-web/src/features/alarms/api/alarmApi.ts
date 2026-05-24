import { apiClient } from '../../../shared/api/client'

export type AlarmCondition =
  | 'GREATER_THAN'
  | 'LESS_THAN'
  | 'EQUAL'
  | 'PERCENT_CHANGE_UP'
  | 'PERCENT_CHANGE_DOWN'

export type AlarmItem = {
  id: number
  instrumentSymbol: string
  condition: AlarmCondition
  threshold: number
  active: boolean
  createdAt: string
}

type ApiEnvelope<T> = {
  success: boolean
  data?: T
  error?: { message?: string }
}

function assertSuccessData<T>(body: ApiEnvelope<T>): T {
  if (!body.success) {
    throw new Error(body.error?.message ?? 'Request failed')
  }
  return body.data as T
}

function assertSuccessOnly(body: ApiEnvelope<unknown>): void {
  if (!body.success) {
    throw new Error(body.error?.message ?? 'Request failed')
  }
}

export async function fetchActiveAlarms(): Promise<AlarmItem[]> {
  const { data } = await apiClient.get<ApiEnvelope<AlarmItem[]>>('/api/alarms')
  return assertSuccessData(data)
}

export type CreateAlarmPayload = {
  instrumentId: number
  condition: AlarmCondition
  threshold: number
}

export async function createAlarm(payload: CreateAlarmPayload): Promise<void> {
  const { data } = await apiClient.post<ApiEnvelope<unknown>>('/api/alarms', payload)
  assertSuccessOnly(data)
}

export async function deactivateAlarm(alarmId: number): Promise<void> {
  const { data } = await apiClient.delete<ApiEnvelope<unknown>>(`/api/alarms/${alarmId}`)
  assertSuccessOnly(data)
}

/** Header preview and alarms page listen for this after list changes. */
export function notifyAlarmsChanged(): void {
  window.dispatchEvent(new CustomEvent('finance-alarms-changed'))
}
