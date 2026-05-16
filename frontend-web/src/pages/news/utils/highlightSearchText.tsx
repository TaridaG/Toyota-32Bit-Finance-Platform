import type { ReactNode } from 'react'

export function tokenizeSearchQuery(query: string): string[] {
  return query
    .trim()
    .split(/\s+/)
    .map((token) => token.trim())
    .filter((token) => token.length >= 2)
}

export function highlightSearchText(text: string, query: string): ReactNode {
  const tokens = tokenizeSearchQuery(query)
  if (tokens.length === 0) {
    return text
  }

  const pattern = new RegExp(`(${tokens.map(escapeRegExp).join('|')})`, 'gi')
  const parts = text.split(pattern)

  return parts.map((part, index) => {
    const isMatch = tokens.some((token) => part.toLowerCase() === token.toLowerCase())
    if (!isMatch) {
      return part
    }
    return (
      <mark key={`${part}-${index}`} className="fi-news-highlight">
        {part}
      </mark>
    )
  })
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function collectMatchedKeywords(text: string, query: string, relatedAssets: string[]): string[] {
  const haystack = text.toLowerCase()
  const matched = new Set<string>()

  for (const token of tokenizeSearchQuery(query)) {
    if (haystack.includes(token.toLowerCase())) {
      matched.add(token)
    }
  }

  const queryLower = query.trim().toLowerCase()
  for (const asset of relatedAssets) {
    if (!queryLower) {
      continue
    }
    if (asset.toLowerCase().includes(queryLower) || queryLower.includes(asset.toLowerCase())) {
      matched.add(asset)
    }
  }

  return Array.from(matched)
}
