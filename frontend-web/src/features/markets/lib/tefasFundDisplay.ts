const FUND_PREFIX = 'FUND_'

export function isTefasCanonicalFundSymbol(symbol: string): boolean {
  const s = symbol.trim().toUpperCase()
  return s.startsWith(FUND_PREFIX) && s.length > FUND_PREFIX.length
}

export function tefasFundCodeFromSymbol(symbol: string): string | null {
  if (!isTefasCanonicalFundSymbol(symbol)) return null
  return symbol.trim().toUpperCase().slice(FUND_PREFIX.length)
}

/** User-facing TEFAS fund code (e.g. AFA) instead of canonical symbol (FUND_AFA). */
export function formatInstrumentDisplaySymbol(symbol: string): string {
  const code = tefasFundCodeFromSymbol(symbol)
  return code ?? symbol.trim().toUpperCase()
}

function escapeRegex(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/** Strip redundant "TEFAS CODE —" prefix and demo suffix from catalog names. */
export function formatTefasFundDisplayName(name: string, symbol: string): string {
  const trimmed = name.trim()
  const code = tefasFundCodeFromSymbol(symbol)
  if (!code) return trimmed || symbol.trim().toUpperCase()
  if (!trimmed) return code

  const stripped = trimmed
    .replace(new RegExp(`^TEFAS\\s+${escapeRegex(code)}\\s*[—–-]\\s*`, 'i'), '')
    .replace(/\s*\(örnek\)\s*$/i, '')
    .replace(/\s*\(ornek\)\s*$/i, '')
    .trim()

  const text = stripped || trimmed
  return text.charAt(0).toLocaleUpperCase('tr-TR') + text.slice(1)
}

export type InstrumentDisplayLabel = {
  symbol: string
  name: string
  isTefasFund: boolean
}

export function resolveInstrumentDisplayLabel(symbol: string, name: string): InstrumentDisplayLabel {
  if (!isTefasCanonicalFundSymbol(symbol)) {
    const sym = symbol.trim().toUpperCase()
    return {
      symbol: sym,
      name: name.trim() || sym,
      isTefasFund: false,
    }
  }
  return {
    symbol: formatInstrumentDisplaySymbol(symbol),
    name: formatTefasFundDisplayName(name, symbol),
    isTefasFund: true,
  }
}

/** Single-line label for selects: "AFA — Para piyasası". */
export function formatInstrumentOptionLabel(symbol: string, name: string): string {
  const label = resolveInstrumentDisplayLabel(symbol, name)
  if (!label.name || label.name === label.symbol) return label.symbol
  return `${label.symbol} — ${label.name}`
}
