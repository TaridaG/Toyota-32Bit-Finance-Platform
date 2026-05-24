/** Guest-accessible catalog pages (no login). */
export const PUBLIC_FINANCIAL_LITERACY_ROUTE = '/finansal-okuryazarlik'
export const PUBLIC_BANK_RATES_ROUTE = '/bank-rates'
export const PUBLIC_MARKETS_ROUTE = '/markets'

export const PUBLIC_CATALOG_NAV = [
  { to: PUBLIC_MARKETS_ROUTE, labelKey: 'header.navPublic.markets' },
  { to: PUBLIC_BANK_RATES_ROUTE, labelKey: 'header.navPublic.bankRates' },
  { to: PUBLIC_FINANCIAL_LITERACY_ROUTE, labelKey: 'header.navPublic.finansalOkuryazarlik' },
] as const
