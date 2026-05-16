import type { UTCTimestamp } from 'lightweight-charts'
import { apiClient } from '../../../shared/api/client'
import { normalizeDrawColor } from '../../../pages/analysis/chart/drawing/drawColors'
import type { AssetType, DrawingItem, DrawingMarker } from '../../../pages/analysis/types'

export type SavedDrawingPayload =
  | { type: 'point'; color?: string; anchor: { time: number; price: number } }
  | { type: 'hline'; color?: string; price: number }
  | { type: 'vline'; color?: string; time: number }
  | {
      type: 'trendline'
      color?: string
      a: { time: number; price: number }
      b: { time: number; price: number }
    }
  | { type: 'ray'; color?: string; a: { time: number; price: number }; b: { time: number; price: number } }
  | { type: 'rect'; color?: string; a: { time: number; price: number }; b: { time: number; price: number } }
  | { type: 'fib'; color?: string; a: { time: number; price: number }; b: { time: number; price: number } }

export type ChartDrawingSaveSummary = {
  id: number
  name: string
  assetKey: string
  assetSymbol: string
  assetType: string | null
  createdAt: string
  drawingTypes: string[]
  drawingMarkers?: DrawingMarker[]
  minAnchorTime: number | null
  maxAnchorTime: number | null
  minPrice: number | null
  maxPrice: number | null
}

export type ChartDrawingSaveDetail = ChartDrawingSaveSummary & {
  drawings: SavedDrawingPayload[]
}

type ApiEnvelope<T> = {
  data: T
}

export function stripDrawingIds(items: DrawingItem[]): SavedDrawingPayload[] {
  return items.map((item) => {
    const { id: _id, ...rest } = item
    return rest as SavedDrawingPayload
  })
}

export function resolveDrawingMarkers(summary: ChartDrawingSaveSummary): DrawingMarker[] {
  if (summary.drawingMarkers && summary.drawingMarkers.length > 0) {
    return summary.drawingMarkers.map((marker) => ({
      type: marker.type,
      color: normalizeDrawColor(marker.color),
    }))
  }
  return summary.drawingTypes.map((type) => ({
    type: type as DrawingMarker['type'],
    color: normalizeDrawColor(undefined),
  }))
}

export function hydrateSavedDrawings(payloads: SavedDrawingPayload[]): DrawingItem[] {
  return payloads.map((payload) => {
    const id = crypto.randomUUID()
    const color = normalizeDrawColor(payload.color)
    switch (payload.type) {
      case 'point':
        return {
          id,
          type: 'point',
          color,
          anchor: { time: payload.anchor.time as UTCTimestamp, price: payload.anchor.price },
        }
      case 'hline':
        return { id, type: 'hline', color, price: payload.price }
      case 'vline':
        return { id, type: 'vline', color, time: payload.time as UTCTimestamp }
      case 'trendline':
        return {
          id,
          type: 'trendline',
          color,
          a: { time: payload.a.time as UTCTimestamp, price: payload.a.price },
          b: { time: payload.b.time as UTCTimestamp, price: payload.b.price },
        }
      case 'ray':
        return {
          id,
          type: 'ray',
          color,
          a: { time: payload.a.time as UTCTimestamp, price: payload.a.price },
          b: { time: payload.b.time as UTCTimestamp, price: payload.b.price },
        }
      case 'rect':
        return {
          id,
          type: 'rect',
          color,
          a: { time: payload.a.time as UTCTimestamp, price: payload.a.price },
          b: { time: payload.b.time as UTCTimestamp, price: payload.b.price },
        }
      case 'fib':
        return {
          id,
          type: 'fib',
          color,
          a: { time: payload.a.time as UTCTimestamp, price: payload.a.price },
          b: { time: payload.b.time as UTCTimestamp, price: payload.b.price },
        }
      default:
        throw new Error('Unknown drawing type')
    }
  })
}

export async function createChartDrawingSave(input: {
  assetKey: string
  assetSymbol: string
  assetType: AssetType
  name: string
  drawings: DrawingItem[]
}): Promise<ChartDrawingSaveDetail> {
  const { data } = await apiClient.post<ApiEnvelope<ChartDrawingSaveDetail>>('/api/chart-drawings', {
    assetKey: input.assetKey,
    assetSymbol: input.assetSymbol,
    assetType: input.assetType,
    name: input.name.trim(),
    drawings: stripDrawingIds(input.drawings),
  })
  return data.data
}

export async function fetchChartDrawingSaves(assetKey: string): Promise<ChartDrawingSaveSummary[]> {
  const { data } = await apiClient.get<ApiEnvelope<ChartDrawingSaveSummary[]>>('/api/chart-drawings', {
    params: { assetKey },
  })
  return data.data
}

export async function fetchChartDrawingSave(id: number): Promise<ChartDrawingSaveDetail> {
  const { data } = await apiClient.get<ApiEnvelope<ChartDrawingSaveDetail>>(`/api/chart-drawings/${id}`)
  return data.data
}

export async function deleteChartDrawingSave(id: number): Promise<void> {
  await apiClient.delete(`/api/chart-drawings/${id}`)
}
