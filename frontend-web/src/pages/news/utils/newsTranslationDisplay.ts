type TranslatableNewsFields = {
  title: string
  summary?: string
  titleOriginal?: string
  summaryOriginal?: string | null
  translated?: boolean
}

export function shouldShowNewsTranslationToggle(item: TranslatableNewsFields): boolean {
  if (item.translated) {
    return true
  }
  const originalTitle = item.titleOriginal?.trim()
  if (!originalTitle) {
    return false
  }
  return normalizeComparable(originalTitle) !== normalizeComparable(item.title)
}

export function resolveNewsDisplayText(
  showOriginal: boolean,
  translated: { title: string; summary: string },
  original: { title: string; summary: string } | null,
): { title: string; summary: string } {
  if (!showOriginal || !original) {
    return translated
  }
  return {
    title: original.title || translated.title,
    summary: original.summary || translated.summary,
  }
}

function normalizeComparable(value: string): string {
  return value.trim().replace(/\s+/g, ' ').toLowerCase()
}
