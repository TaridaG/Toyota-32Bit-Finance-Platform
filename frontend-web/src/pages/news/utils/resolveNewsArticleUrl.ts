const PUBLISHER_BASE_BY_SOURCE: Record<string, string> = {
  bigpara: 'https://bigpara.hurriyet.com.tr',
  cointelegraph: 'https://cointelegraph.com',
}

export function resolveNewsArticleUrl(articleUrl: string, sourceName?: string): string {
  const trimmed = articleUrl?.trim() ?? ''
  if (!trimmed) {
    return ''
  }
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed
  }
  if (trimmed.startsWith('//')) {
    return `https:${trimmed}`
  }

  const sourceKey = (sourceName ?? '').trim().toLowerCase()
  const base =
    Object.entries(PUBLISHER_BASE_BY_SOURCE).find(([key]) => sourceKey.includes(key))?.[1] ?? ''

  if (!base) {
    return trimmed
  }

  const path = trimmed.startsWith('/') ? trimmed : `/${trimmed}`
  return `${base.replace(/\/+$/, '')}${path}`
}
