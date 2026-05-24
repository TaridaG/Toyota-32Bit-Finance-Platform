/** Same rules as portal registration and finance-api username validation. */
export const USERNAME_MIN_LENGTH = 3
export const USERNAME_MAX_LENGTH = 36

/** Letters, digits, dot, underscore, hyphen — no HTML pattern attribute (avoids /v regexp bugs). */
export const USERNAME_CHARS_RE = /^[a-zA-Z0-9._-]+$/

export const USERNAME_AVAILABILITY_DEBOUNCE_MS = 600

export function normalizeUsernameInput(raw: string): string {
  return raw.trim().toLowerCase()
}

export function isUsernameFormatValid(raw: string): boolean {
  const trimmed = normalizeUsernameInput(raw)
  if (trimmed.length < USERNAME_MIN_LENGTH || trimmed.length > USERNAME_MAX_LENGTH) {
    return false
  }
  return USERNAME_CHARS_RE.test(trimmed)
}
