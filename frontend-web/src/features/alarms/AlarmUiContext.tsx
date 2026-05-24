import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

export type AlarmCreateTarget = {
  instrumentId: number
  symbol: string
  name?: string | null
  /** Optional spot from markets row while live quote loads */
  currentPrice?: number | null
}

type AlarmUiContextValue = {
  createTarget: AlarmCreateTarget | null
  openCreateAlarm: (target: AlarmCreateTarget) => void
  closeCreateAlarm: () => void
  refreshKey: number
  bumpAlarmsRefresh: () => void
}

const AlarmUiContext = createContext<AlarmUiContextValue | null>(null)

export function AlarmUiProvider({ children }: { children: ReactNode }) {
  const [createTarget, setCreateTarget] = useState<AlarmCreateTarget | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  const openCreateAlarm = useCallback((target: AlarmCreateTarget) => {
    setCreateTarget(target)
  }, [])

  const closeCreateAlarm = useCallback(() => {
    setCreateTarget(null)
  }, [])

  const bumpAlarmsRefresh = useCallback(() => {
    setRefreshKey((k) => k + 1)
  }, [])

  const value = useMemo(
    () => ({
      createTarget,
      openCreateAlarm,
      closeCreateAlarm,
      refreshKey,
      bumpAlarmsRefresh,
    }),
    [createTarget, openCreateAlarm, closeCreateAlarm, refreshKey, bumpAlarmsRefresh],
  )

  return <AlarmUiContext.Provider value={value}>{children}</AlarmUiContext.Provider>
}

export function useAlarmUi() {
  const ctx = useContext(AlarmUiContext)
  if (!ctx) {
    throw new Error('useAlarmUi must be used within AlarmUiProvider')
  }
  return ctx
}
