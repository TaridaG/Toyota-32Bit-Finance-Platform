import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { infoCardsApi } from '../../services/infoCardsApi'
import { fetchAdminInfoCardsDashboard } from './api/infoCardsHttpApi'
import type { InfoCard, InfoCardInput, InfoCardsDashboard } from '../../types/infoCards'

type InfoCardsContextValue = {
  cards: InfoCard[]
  dashboard: InfoCardsDashboard | null
  loading: boolean
  error: string | null
  refresh: () => Promise<void>
  createCard: (input: InfoCardInput) => Promise<InfoCard>
  updateCard: (id: string, patch: Partial<InfoCardInput>) => Promise<InfoCard>
  deleteCard: (id: string) => Promise<boolean>
  toggleStatus: (id: string) => Promise<InfoCard | null>
}

const InfoCardsContext = createContext<InfoCardsContextValue | null>(null)

export function InfoCardsProvider({ children }: { children: ReactNode }) {
  const [cards, setCards] = useState<InfoCard[]>([])
  const [dashboard, setDashboard] = useState<InfoCardsDashboard | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [portalCards, dash] = await Promise.all([
        infoCardsApi.loadPortalCards(true),
        fetchAdminInfoCardsDashboard().catch(() => null),
      ])
      setCards(portalCards)
      setDashboard(dash ?? infoCardsApi.getDashboard())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load info cards')
      setCards(infoCardsApi.getAll())
      setDashboard(infoCardsApi.getDashboard())
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const value = useMemo<InfoCardsContextValue>(
    () => ({
      cards,
      dashboard: dashboard ?? infoCardsApi.getDashboard(),
      loading,
      error,
      refresh,
      createCard: async (input) => {
        const card = await infoCardsApi.create(input)
        await refresh()
        return card
      },
      updateCard: async (id, patch) => {
        const card = await infoCardsApi.update(id, patch)
        await refresh()
        return card
      },
      deleteCard: async (id) => {
        const ok = await infoCardsApi.delete(id)
        await refresh()
        return ok
      },
      toggleStatus: async (id) => {
        const card = await infoCardsApi.toggleStatus(id)
        await refresh()
        return card
      },
    }),
    [cards, dashboard, loading, error, refresh],
  )

  return <InfoCardsContext.Provider value={value}>{children}</InfoCardsContext.Provider>
}

export function useInfoCards() {
  const ctx = useContext(InfoCardsContext)
  if (!ctx) {
    throw new Error('useInfoCards must be used within InfoCardsProvider')
  }
  return ctx
}
