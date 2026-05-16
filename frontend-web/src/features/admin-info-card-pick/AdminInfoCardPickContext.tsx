import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import type { PortalPageKey } from '../../types/infoCards'

export type InfoCardPickPrefill = {
  pageKey: PortalPageKey
  targetTerms: string[]
  targetElementIds?: string[]
  targetInstrumentSymbols?: string[]
  title: string
  pickLabel: string
}

type AdminInfoCardPickContextValue = {
  pickModeActive: boolean
  canPickOnRoute: boolean
  setCanPickOnRoute: (allowed: boolean) => void
  activatePickMode: () => void
  deactivatePickMode: () => void
  togglePickMode: () => void
  editorOpen: boolean
  pickPrefill: InfoCardPickPrefill | null
  openEditorFromPick: (prefill: InfoCardPickPrefill) => void
  closeEditor: () => void
}

const AdminInfoCardPickContext = createContext<AdminInfoCardPickContextValue | null>(null)

export function AdminInfoCardPickProvider({ children }: { children: ReactNode }) {
  const [pickModeActive, setPickModeActive] = useState(false)
  const [canPickOnRoute, setCanPickOnRoute] = useState(true)
  const [editorOpen, setEditorOpen] = useState(false)
  const [pickPrefill, setPickPrefill] = useState<InfoCardPickPrefill | null>(null)

  const deactivatePickMode = useCallback(() => {
    setPickModeActive(false)
    window.getSelection()?.removeAllRanges()
  }, [])

  const activatePickMode = useCallback(() => {
    setEditorOpen(false)
    setPickPrefill(null)
    setPickModeActive(true)
    window.getSelection()?.removeAllRanges()
  }, [])

  const togglePickMode = useCallback(() => {
    setPickModeActive((prev) => {
      if (prev) {
        window.getSelection()?.removeAllRanges()
        return false
      }
      setEditorOpen(false)
      setPickPrefill(null)
      return true
    })
  }, [])

  const openEditorFromPick = useCallback((prefill: InfoCardPickPrefill) => {
    setPickModeActive(false)
    setPickPrefill(prefill)
    setEditorOpen(true)
    window.getSelection()?.removeAllRanges()
  }, [])

  const closeEditor = useCallback(() => {
    setEditorOpen(false)
    setPickPrefill(null)
  }, [])

  const value = useMemo(
    () => ({
      pickModeActive,
      canPickOnRoute,
      setCanPickOnRoute,
      activatePickMode,
      deactivatePickMode,
      togglePickMode,
      editorOpen,
      pickPrefill,
      openEditorFromPick,
      closeEditor,
    }),
    [
      pickModeActive,
      canPickOnRoute,
      activatePickMode,
      deactivatePickMode,
      togglePickMode,
      editorOpen,
      pickPrefill,
      openEditorFromPick,
      closeEditor,
    ],
  )

  return <AdminInfoCardPickContext.Provider value={value}>{children}</AdminInfoCardPickContext.Provider>
}

export function useAdminInfoCardPick() {
  const ctx = useContext(AdminInfoCardPickContext)
  if (!ctx) {
    throw new Error('useAdminInfoCardPick must be used within AdminInfoCardPickProvider')
  }
  return ctx
}
