import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'

export type HelpCardAnchor = {
  cardId: string
  anchor: DOMRect
}

type LiteracyHelpModeContextValue = {
  active: boolean
  toggle: () => void
  deactivate: () => void
  activate: () => void
  openCardState: HelpCardAnchor | null
  openCard: (cardId: string, anchor: DOMRect) => void
  closeCard: () => void
}

const LiteracyHelpModeContext = createContext<LiteracyHelpModeContextValue | null>(null)

export function LiteracyHelpModeProvider({ children }: { children: ReactNode }) {
  const [active, setActive] = useState(false)
  const [openCardState, setOpenCardState] = useState<HelpCardAnchor | null>(null)

  const deactivate = useCallback(() => {
    setActive(false)
    setOpenCardState(null)
  }, [])

  const activate = useCallback(() => {
    setActive(true)
    setOpenCardState(null)
  }, [])

  const toggle = useCallback(() => {
    setActive((prev) => {
      if (prev) {
        setOpenCardState(null)
        return false
      }
      setOpenCardState(null)
      return true
    })
  }, [])

  const openCard = useCallback((cardId: string, anchor: DOMRect) => {
    setOpenCardState({ cardId, anchor })
  }, [])

  const closeCard = useCallback(() => {
    setOpenCardState(null)
  }, [])

  const value = useMemo(
    () => ({
      active,
      toggle,
      deactivate,
      activate,
      openCardState,
      openCard,
      closeCard,
    }),
    [active, toggle, deactivate, activate, openCardState, openCard, closeCard],
  )

  return <LiteracyHelpModeContext.Provider value={value}>{children}</LiteracyHelpModeContext.Provider>
}

export function useLiteracyHelpMode() {
  const ctx = useContext(LiteracyHelpModeContext)
  if (!ctx) {
    throw new Error('useLiteracyHelpMode must be used within LiteracyHelpModeProvider')
  }
  return ctx
}
