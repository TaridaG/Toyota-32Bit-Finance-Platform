import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

export type AdminLatencySnapshot = {
  id: number
  averageLatencySec: number
  sampleCount: number
  measuredAt: string
  hasSamples: boolean
}

export type LatencyRunRow = {
  id: number
  averageLatencySec: number
  sampleCount: number
  measuredAt: string
  hasSamples: boolean
}

export type LatencyRunsPage = {
  content: LatencyRunRow[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export type LatencyProbeSampleItem = {
  path: string
  durationMs: number
}

type MetricsEnvelope<T> = {
  success: boolean
  data: T | null
  error?: { code?: string; message?: string }
}

function describeFailure(e: unknown, fallback: string): string {
  if (isAxiosError(e)) {
    const status = e.response?.status
    const body = e.response?.data as MetricsEnvelope<unknown> | undefined
    const serverMsg = body?.error?.message
    if (serverMsg) return serverMsg
    if (status != null) return `HTTP ${status}`
  }
  if (e instanceof Error) return e.message
  return fallback
}

export async function fetchAdminLatencySnapshot(): Promise<AdminLatencySnapshot | null> {
  try {
    const { data } = await apiClient.get<MetricsEnvelope<AdminLatencySnapshot>>('/api/v1/admin/metrics/latency-snapshot')
    if (!data.success) {
      throw new Error(data.error?.message ?? 'latency-snapshot failed')
    }
    return data.data
  } catch (e) {
    throw new Error(describeFailure(e, 'latency-snapshot unavailable'))
  }
}

export async function fetchAdminLatencyRunsPage(page: number, size: number): Promise<LatencyRunsPage> {
  try {
    const { data } = await apiClient.get<MetricsEnvelope<LatencyRunsPage>>('/api/v1/admin/metrics/latency-runs', {
      params: { page, size },
    })
    if (!data.success || data.data == null) {
      throw new Error(data.error?.message ?? 'latency-runs failed')
    }
    return data.data
  } catch (e) {
    throw new Error(describeFailure(e, 'latency-runs unavailable'))
  }
}

export async function fetchLatencyRunSamples(runId: number): Promise<LatencyProbeSampleItem[]> {
  try {
    const { data } = await apiClient.get<MetricsEnvelope<LatencyProbeSampleItem[]>>(
      `/api/v1/admin/metrics/latency-runs/${runId}/samples`,
    )
    if (!data.success || data.data == null) {
      throw new Error(data.error?.message ?? 'latency samples failed')
    }
    return data.data
  } catch (e) {
    throw new Error(describeFailure(e, 'latency samples unavailable'))
  }
}

export async function fetchAdminLatencyProbeTargets(): Promise<string[]> {
  try {
    const { data } = await apiClient.get<MetricsEnvelope<{ paths: string[] }>>(
      '/api/v1/admin/metrics/latency-probe-targets',
    )
    if (!data.success || !data.data?.paths?.length) {
      throw new Error(data.error?.message ?? 'latency-probe-targets failed')
    }
    return data.data.paths
  } catch (e) {
    throw new Error(describeFailure(e, 'latency-probe-targets unavailable'))
  }
}

export type LatencySample = { path: string; durationMs: number }

export async function saveAdminLatencySnapshot(samples: LatencySample[]): Promise<AdminLatencySnapshot> {
  try {
    const { data } = await apiClient.post<MetricsEnvelope<AdminLatencySnapshot>>(
      '/api/v1/admin/metrics/latency-snapshot',
      { samples },
    )
    if (!data.success || data.data == null) {
      throw new Error(data.error?.message ?? 'latency-snapshot save failed')
    }
    return data.data
  } catch (e) {
    throw new Error(describeFailure(e, 'latency-snapshot save unavailable'))
  }
}
