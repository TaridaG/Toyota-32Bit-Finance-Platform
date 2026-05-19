import { useCallback, useEffect, useRef, useState } from 'react'
import { apiClient } from '../../../shared/api/client'
import type { FxMidRow } from '../../../features/markets/lib/marketDisplayConversion'
import {
  buildFxRateMapFromHistories,
  buildFxRateMapFromMidRows,
  enrichFxRateMap,
  type FxRateMap,
} from '../../../features/markets/lib/fxTryHubConversion'

const FX_SYMBOLS = ['USDTRY', 'EURTRY', 'GBPTRY', 'JPYTRY', 'AEDTRY', 'EURUSD', 'GBPUSD', 'JPYUSD'] as const

/** Small lookback so TCMB holiday gaps still resolve "on or before" the purchase day. */
const FX_LOOKBACK_DAYS = 10

type FxHistoryPoint = { time: string; value: number | string }

type ParsedFxPoint = { day: string; value: number }

function toIsoDay(time: string): string | null {
  const ts = Date.parse(time)
  if (!Number.isFinite(ts)) return null
  return new Date(ts).toISOString().slice(0, 10)
}

function parseFxHistory(points: FxHistoryPoint[]): ParsedFxPoint[] {
  return points
    .map((p) => {
      const day = p.time ? toIsoDay(p.time) : null
      const raw = Number(p.value)
      if (!day || !Number.isFinite(raw)) return null
      return { day, value: raw }
    })
    .filter((p): p is ParsedFxPoint => p != null)
    .sort((a, b) => a.day.localeCompare(b.day))
}

function findRateOnOrBeforeDay(series: ParsedFxPoint[], isoDay: string): number | null {
  if (!isoDay || series.length === 0) return null
  let best: ParsedFxPoint | null = null
  for (const p of series) {
    if (p.day <= isoDay) best = p
    else break
  }
  return best?.value ?? null
}

function todayIsoDay(): string {
  return new Date().toISOString().slice(0, 10)
}

function addUtcDays(isoDay: string, days: number): string {
  const d = new Date(`${isoDay}T12:00:00Z`)
  d.setUTCDate(d.getUTCDate() + days)
  return d.toISOString().slice(0, 10)
}

function isRecentIsoDay(isoDay: string, slackDays = 1): boolean {
  const today = todayIsoDay()
  if (isoDay >= today) return true
  const diffDays = (Date.parse(today) - Date.parse(isoDay)) / 86_400_000
  return diffDays <= slackDays
}

async function fetchFxHistoryWindow(symbol: string, fromDay: string, toDay: string): Promise<ParsedFxPoint[]> {
  try {
    const response = await apiClient.get<FxHistoryPoint[]>('/api/market/fx/history', {
      params: { symbol, from: fromDay, to: toDay },
    })
    const rows = Array.isArray(response.data) ? response.data : []
    return parseFxHistory(rows)
  } catch {
    return []
  }
}

async function fetchLiveFxMidRows(): Promise<FxMidRow[]> {
  try {
    const response = await apiClient.get<FxMidRow[] | { data?: FxMidRow[] }>('/api/market/fx')
    const rows = Array.isArray(response.data)
      ? response.data
      : Array.isArray(response.data?.data)
        ? response.data.data
        : []
    return rows
  } catch {
    return []
  }
}

function buildFxRateMapForDay(
  bySymbol: Record<string, ParsedFxPoint[]>,
  isoDay: string,
  liveFallback: FxRateMap,
): FxRateMap {
  const raw: Record<string, number | null | undefined> = {}
  for (const symbol of FX_SYMBOLS) {
    raw[symbol] = findRateOnOrBeforeDay(bySymbol[symbol] ?? [], isoDay)
  }
  const historical = enrichFxRateMap(buildFxRateMapFromHistories(raw))
  const filled: FxRateMap = { ...historical }
  if (isRecentIsoDay(isoDay, 5)) {
    for (const sym of FX_SYMBOLS) {
      if (filled[sym] == null && liveFallback[sym] != null) {
        filled[sym] = liveFallback[sym]
      }
    }
  }
  return enrichFxRateMap(filled)
}

async function fetchFxRateMapForDay(isoDay: string, liveFallback: FxRateMap): Promise<FxRateMap> {
  if (!isoDay) return enrichFxRateMap({ ...liveFallback })

  const today = todayIsoDay()
  const to = isoDay > today ? today : isoDay
  const from = addUtcDays(to, -FX_LOOKBACK_DAYS)

  const historyResults = await Promise.all(
    FX_SYMBOLS.map((sym) => fetchFxHistoryWindow(sym, from, to)),
  )

  const bySymbol: Record<string, ParsedFxPoint[]> = {}
  FX_SYMBOLS.forEach((sym, i) => {
    bySymbol[sym] = historyResults[i] ?? []
  })

  return buildFxRateMapForDay(bySymbol, isoDay, liveFallback)
}

type UseFxTryHubHistoryResult = {
  loading: boolean
  error: boolean
  rateMapAtDay: (isoDay: string) => FxRateMap
}

/**
 * Lazy FX for investment simulation: loads live mids once, then fetches history only for
 * the purchase day and exit day (±{@link FX_LOOKBACK_DAYS}), not the full chart range.
 */
export function useFxTryHubHistory(
  enabled: boolean,
  entryDay: string,
  exitDay: string | null,
): UseFxTryHubHistoryResult {
  const [liveFallback, setLiveFallback] = useState<FxRateMap>({})
  const [cache, setCache] = useState<Record<string, FxRateMap>>({})
  const [pendingDays, setPendingDays] = useState<Set<string>>(() => new Set())
  const [error, setError] = useState(false)
  const liveRef = useRef<FxRateMap>({})
  const cacheRef = useRef<Record<string, FxRateMap>>({})

  useEffect(() => {
    liveRef.current = liveFallback
  }, [liveFallback])

  useEffect(() => {
    cacheRef.current = cache
  }, [cache])

  useEffect(() => {
    if (!enabled) {
      setLiveFallback({})
      setCache({})
      setPendingDays(new Set())
      setError(false)
      liveRef.current = {}
      cacheRef.current = {}
      return
    }

    let cancelled = false
    void (async () => {
      const rows = await fetchLiveFxMidRows()
      if (cancelled) return
      const liveMap = buildFxRateMapFromMidRows(rows)
      setLiveFallback(liveMap)
      liveRef.current = liveMap
      if (Object.keys(liveMap).length === 0) {
        setError(true)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [enabled])

  const loadDay = useCallback(async (isoDay: string) => {
    if (!isoDay || cacheRef.current[isoDay]) return

    setPendingDays((prev) => new Set(prev).add(isoDay))

    try {
      const map = await fetchFxRateMapForDay(isoDay, liveRef.current)
      setCache((prev) => {
        const next = { ...prev, [isoDay]: map }
        cacheRef.current = next
        return next
      })
    } catch {
      setError(true)
    } finally {
      setPendingDays((prev) => {
        const next = new Set(prev)
        next.delete(isoDay)
        return next
      })
    }
  }, [])

  useEffect(() => {
    if (!enabled) return
    const days = new Set<string>()
    if (entryDay) days.add(entryDay)
    if (exitDay) days.add(exitDay)
    for (const day of days) {
      void loadDay(day)
    }
  }, [enabled, entryDay, exitDay, loadDay])

  const rateMapAtDay = useCallback(
    (isoDay: string): FxRateMap => {
      if (!isoDay) return enrichFxRateMap({ ...liveFallback })
      if (cache[isoDay]) return cache[isoDay]
      if (isRecentIsoDay(isoDay, 5)) {
        return enrichFxRateMap({ ...liveFallback })
      }
      return {}
    },
    [cache, liveFallback],
  )

  const loading =
    pendingDays.has(entryDay) || (exitDay != null && pendingDays.has(exitDay))

  return { loading, error, rateMapAtDay }
}
