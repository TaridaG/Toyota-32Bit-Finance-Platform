import type { MarketsRowDragPayload } from './marketsRowDrag'

export const MARKETS_PORTFOLIO_SIM_PANEL_ID = 'markets-portfolio-simulation'

type AddHandler = (payload: MarketsRowDragPayload) => void

let addHandler: AddHandler | null = null

export function registerMarketsPortfolioSimAdd(handler: AddHandler): () => void {
  addHandler = handler
  return () => {
    if (addHandler === handler) {
      addHandler = null
    }
  }
}

export function addRowToMarketsPortfolioSimulation(payload: MarketsRowDragPayload): void {
  addHandler?.(payload)
  document.getElementById(MARKETS_PORTFOLIO_SIM_PANEL_ID)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
