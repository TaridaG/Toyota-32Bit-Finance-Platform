export type FloatingMarketPin = {
  id: string
  symbol: string
  label: string
  lat: number
  lng: number
  fallbackPrice: string
  fallbackChange: number
  icon: 'crypto' | 'index' | 'fx' | 'metal' | 'stock'
  apiSymbolHints?: string[]
}

export const FLOATING_MARKET_PINS: FloatingMarketPin[] = [
  {
    id: 'btc',
    symbol: 'BTC/USDT',
    label: 'BTC',
    lat: 37.77,
    lng: -122.42,
    fallbackPrice: '$67,421',
    fallbackChange: -0.79,
    icon: 'crypto',
    apiSymbolHints: ['BTCUSDT', 'BTC/USDT'],
  },
  {
    id: 'nasdaq',
    symbol: 'NASDAQ',
    label: 'NASDAQ',
    lat: 40.71,
    lng: -74.01,
    fallbackPrice: '18,620',
    fallbackChange: 0.72,
    icon: 'index',
    apiSymbolHints: ['NDX', 'NASDAQ'],
  },
  {
    id: 'usdtry',
    symbol: 'USD/TRY',
    label: 'USD/TRY',
    lat: 41.01,
    lng: 28.97,
    fallbackPrice: '32.48',
    fallbackChange: 0.12,
    icon: 'fx',
    apiSymbolHints: ['USDTRY', 'USD/TRY'],
  },
]

export const DISPLAY_PIN_IDS = ['btc', 'usdtry', 'nasdaq'] as const

export const NETWORK_CONNECTIONS: Array<[string, string]> = [
  ['nasdaq', 'usdtry'],
  ['btc', 'nasdaq'],
  ['usdtry', 'nasdaq'],
]

export const EARTH_NIGHT_TEXTURE_URL =
  'https://cdn.jsdelivr.net/npm/three-globe@2.45.0/example/img/earth-night.jpg'

export const EARTH_DAY_TEXTURE_URL =
  'https://cdn.jsdelivr.net/npm/three-globe@2.45.0/example/img/earth-blue-marble.jpg'

export const EARTH_BUMP_URL =
  'https://cdn.jsdelivr.net/npm/three-globe@2.45.0/example/img/earth-topology.png'
