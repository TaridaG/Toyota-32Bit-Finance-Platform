import { isAxiosError } from 'axios'

export const EMAIL_BLOCKED_CODE = 'EMAIL_BLOCKED'
export const EMAIL_IN_USE_CODE = 'EMAIL_IN_USE'

export type RegistrationEmailApiError = {
  message: string
  code?: string
  suggestions: string[]
}

export function parseRegistrationEmailApiError(error: unknown): RegistrationEmailApiError | null {
  if (!isAxiosError(error)) {
    return null
  }
  const body = error.response?.data as {
    error?: { code?: string; message?: string; suggestions?: string[] }
  } | undefined
  const code = body?.error?.code
  const message = body?.error?.message
  if (!message) {
    return null
  }
  if (code === EMAIL_BLOCKED_CODE || code === EMAIL_IN_USE_CODE) {
    return {
      message,
      code,
      suggestions: Array.isArray(body?.error?.suggestions) ? body!.error!.suggestions! : [],
    }
  }
  if (error.response?.status === 403 && /engellen|blocked|gesperrt/i.test(message)) {
    return { message, code: EMAIL_BLOCKED_CODE, suggestions: [] }
  }
  if (error.response?.status === 409 && /kullanılıyor|already|registered|vergeben/i.test(message)) {
    return {
      message,
      code: EMAIL_IN_USE_CODE,
      suggestions: Array.isArray(body?.error?.suggestions) ? body!.error!.suggestions! : [],
    }
  }
  return null
}
