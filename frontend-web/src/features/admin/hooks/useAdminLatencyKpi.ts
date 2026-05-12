import { useCallback, useEffect, useRef, useState } from 'react'
import {
  fetchAdminLatencyProbeTargets,
  fetchAdminLatencySnapshot,
  saveAdminLatencySnapshot,
  type AdminLatencySnapshot,
} from '../api/adminLatencyMetrics'
import { apiClient } from '../../../shared/api/client'
import { isAdminUser } from '../../../shared/auth/session'

export type AdminLatencyKpiPhase = 'idle' | 'loading_snapshot' | 'probing' | 'done_ok' | 'error'

export type AdminLatencyKpi = {
  enabled: boolean
  phase: AdminLatencyKpiPhase
  /** Persisted or running average (seconds); null → use mock KPI string */
  displaySec: number | null
  probeCurrent: number
  probeTotal: number
  runningAvgSec: number | null
  errorMessage: string | null
  runProbe: () => Promise<void>
  reloadSnapshot: () => Promise<void>
}

function averageMs(samples: { durationMs: number }[]): number {
  if (samples.length === 0) return 0
  return samples.reduce((a, s) => a + s.durationMs, 0) / samples.length
}

export function useAdminLatencyKpi(): AdminLatencyKpi {
  const enabled = isAdminUser()
  const [phase, setPhase] = useState<AdminLatencyKpiPhase>(enabled ? 'loading_snapshot' : 'idle')
  const [displaySec, setDisplaySec] = useState<number | null>(null)
  const [probeCurrent, setProbeCurrent] = useState(0)
  const [probeTotal, setProbeTotal] = useState(0)
  const [runningAvgSec, setRunningAvgSec] = useState<number | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const doneTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const probeInFlight = useRef(false)

  const clearDoneTimer = () => {
    if (doneTimer.current != null) {
      clearTimeout(doneTimer.current)
      doneTimer.current = null
    }
  }

  useEffect(() => () => clearDoneTimer(), [])

  const applySnapshot = useCallback((snap: AdminLatencySnapshot | null) => {
    if (snap) {
      setDisplaySec(snap.averageLatencySec)
    }
  }, [])

  const reloadSnapshot = useCallback(async () => {
    if (!enabled) return
    setPhase('loading_snapshot')
    setErrorMessage(null)
    try {
      const snap = await fetchAdminLatencySnapshot()
      applySnapshot(snap)
      setPhase('idle')
    } catch (e) {
      setErrorMessage(e instanceof Error ? e.message : 'load failed')
      setPhase('error')
    }
  }, [applySnapshot, enabled])

  useEffect(() => {
    if (!enabled) {
      setPhase('idle')
      return
    }
    void reloadSnapshot()
  }, [enabled, reloadSnapshot])

  const runProbe = useCallback(async () => {
    if (!enabled || probeInFlight.current) return
    probeInFlight.current = true
    clearDoneTimer()
    setErrorMessage(null)
    setPhase('probing')
    setProbeCurrent(0)
    setProbeTotal(0)
    setRunningAvgSec(null)
    const collected: { path: string; durationMs: number }[] = []
    try {
      const paths = await fetchAdminLatencyProbeTargets()
      setProbeTotal(paths.length)
      for (let i = 0; i < paths.length; i++) {
        const path = paths[i]!
        const t0 = performance.now()
        await apiClientGetForProbe(path)
        const durationMs = performance.now() - t0
        collected.push({ path, durationMs })
        setProbeCurrent(i + 1)
        setRunningAvgSec(averageMs(collected) / 1000)
      }
      const saved = await saveAdminLatencySnapshot(collected)
      setDisplaySec(saved.averageLatencySec)
      setPhase('done_ok')
      doneTimer.current = setTimeout(() => {
        setPhase('idle')
        doneTimer.current = null
      }, 2200)
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'probe failed'
      setErrorMessage(msg)
      setPhase('error')
    } finally {
      probeInFlight.current = false
    }
  }, [enabled])

  return {
    enabled,
    phase,
    displaySec,
    probeCurrent,
    probeTotal,
    runningAvgSec,
    errorMessage,
    runProbe,
    reloadSnapshot,
  }
}

async function apiClientGetForProbe(path: string): Promise<void> {
  await apiClient.get(path)
}
