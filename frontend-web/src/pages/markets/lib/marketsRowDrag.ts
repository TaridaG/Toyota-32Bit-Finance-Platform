export const MARKETS_ROW_DRAG_MIME = 'application/x-finance-market-row'

export type MarketsRowDragPayload = {
  symbol: string
  name: string
  category: string | null
}

export function serializeMarketsRowDrag(payload: MarketsRowDragPayload): string {
  return JSON.stringify(payload)
}

export function parseMarketsRowDrag(data: string): MarketsRowDragPayload | null {
  if (!data) {
    return null
  }
  try {
    const parsed = JSON.parse(data) as MarketsRowDragPayload
    if (!parsed?.symbol?.trim()) {
      return null
    }
    return {
      symbol: parsed.symbol.trim().toUpperCase(),
      name: parsed.name?.trim() || parsed.symbol,
      category: parsed.category ?? null,
    }
  } catch {
    return null
  }
}
