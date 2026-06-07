import { apiClient } from './client'

type SendCodeEnvelope = {
  success: boolean
  data?: {
    expiresInSeconds: number
    resendInSeconds: number
  }
  error?: {
    code?: string
    message?: string
  }
}

type ResetEnvelope = {
  success: boolean
  error?: {
    code?: string
    message?: string
  }
}

export async function sendPublicPasswordResetCode(
  email: string,
  locale?: string,
): Promise<{ expiresInSeconds: number; resendInSeconds: number }> {
  const { data } = await apiClient.post<SendCodeEnvelope>('/api/v1/public/password/send-reset-code', {
    email,
    locale: locale?.trim() || undefined,
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Verification code send failed')
  }
  return {
    expiresInSeconds: data.data.expiresInSeconds,
    resendInSeconds: data.data.resendInSeconds,
  }
}

export async function resetPublicPassword(
  email: string,
  verificationCode: string,
  newPassword: string,
): Promise<void> {
  const { data } = await apiClient.post<ResetEnvelope>('/api/v1/public/password/reset', {
    email,
    verificationCode,
    newPassword,
  })
  if (!data.success) {
    throw new Error(data.error?.message ?? 'Password reset failed')
  }
}
