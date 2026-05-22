import { fetchTahvilHistory, type MarketHistoryPoint } from './tahvilMarketApi'

/** Yahoo metal futures (GC=F, …) — symbol must keep "=" for MDS history lookup. */
export async function fetchMetalFuturesHistory(symbol: string): Promise<MarketHistoryPoint[]> {
  return fetchTahvilHistory(symbol.trim())
}
