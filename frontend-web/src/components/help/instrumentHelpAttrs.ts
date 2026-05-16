/** DOM attributes for instrument rows targeted by admin info cards / help mode. */
export function instrumentHelpRowProps(symbol: string, name?: string) {
  const normalized = symbol.trim().toUpperCase()
  return {
    'data-help-instrument': normalized,
    ...(name?.trim() ? { 'data-help-instrument-name': name.trim() } : {}),
    className: 'instrument-help-row',
  } as const
}
