import { create } from 'zustand'
import {
  getPortfolioAllocation,
  getPortfolioSummary,
  getPortfolios,
} from '../../features/portfolio/api/portfolioApi'
import type {
  Portfolio,
  PortfolioAllocation,
  PortfolioSummary,
} from '../../shared/types/portfolio'

type PortfolioStore = {
  portfolios: Portfolio[]
  summary: PortfolioSummary | null
  allocation: PortfolioAllocation[]
  loading: boolean
  error: string | null
  fetchPortfolios: () => Promise<void>
  fetchSummary: (id: number) => Promise<void>
  fetchAllocation: (id: number) => Promise<void>
}

export const usePortfolioStore = create<PortfolioStore>((set) => ({
  portfolios: [],
  summary: null,
  allocation: [],
  loading: false,
  error: null,

  fetchPortfolios: async () => {
    set({ loading: true, error: null })
    try {
      const portfolios = await getPortfolios()
      set({ portfolios })
    } catch {
      set({ error: 'Portföyler alınırken bir hata oluştu.' })
    } finally {
      set({ loading: false })
    }
  },

  fetchSummary: async (id: number) => {
    set({ loading: true, error: null })
    try {
      const summary = await getPortfolioSummary(id)
      set({ summary })
    } catch {
      set({ error: 'Portföy özeti alınırken bir hata oluştu.' })
    } finally {
      set({ loading: false })
    }
  },

  fetchAllocation: async (id: number) => {
    set({ loading: true, error: null })
    try {
      const allocation = await getPortfolioAllocation(id)
      set({ allocation })
    } catch {
      set({ error: 'Portföy dağılımı alınırken bir hata oluştu.' })
    } finally {
      set({ loading: false })
    }
  },
}))

