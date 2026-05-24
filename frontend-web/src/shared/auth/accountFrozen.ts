import { isAxiosError } from 'axios'
import { clearAuthSession } from './session'

export const ACCOUNT_FROZEN_ERROR_CODE = 'ACCOUNT_FROZEN'
export const ACCOUNT_REMOVED_ERROR_CODE = 'ACCOUNT_REMOVED'

export function isAccountFrozenApiError(error: unknown): boolean {
  if (!isAxiosError(error)) {
    return false
  }
  if (error.response?.status !== 403) {
    return false
  }
  const body = error.response.data as { error?: { code?: string; message?: string } } | undefined
  const code = body?.error?.code
  if (code === ACCOUNT_FROZEN_ERROR_CODE) {
    return true
  }
  const msg = body?.error?.message ?? ''
  return /donduruldu|suspended|frozen|gesperrt/i.test(msg)
}

export function isAccountRemovedApiError(error: unknown): boolean {
  if (!isAxiosError(error)) {
    return false
  }
  if (error.response?.status !== 403) {
    return false
  }
  const body = error.response.data as { error?: { code?: string; message?: string } } | undefined
  const code = body?.error?.code
  if (code === ACCOUNT_REMOVED_ERROR_CODE) {
    return true
  }
  const msg = body?.error?.message ?? ''
  return /kaldırıldı|removed|deleted|gelöscht|entfernt/i.test(msg)
}

export function logoutFrozenAccount(): void {
  clearAuthSession()
  if (!window.location.pathname.includes('/login')) {
    window.location.assign('/login?frozen=1')
  }
}

export function logoutRemovedAccount(): void {
  clearAuthSession()
  if (!window.location.pathname.includes('/login')) {
    window.location.assign('/login?removed=1')
  }
}

export function logoutTerminatedPortalAccount(error: unknown): boolean {
  if (isAccountFrozenApiError(error)) {
    logoutFrozenAccount()
    return true
  }
  if (isAccountRemovedApiError(error)) {
    logoutRemovedAccount()
    return true
  }
  return false
}
