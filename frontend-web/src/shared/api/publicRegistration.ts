import { apiClient } from './client'

export type PublicRegisterBody = {
  email: string
  username: string
  password: string
  verificationCode: string
}

type RegisterEnvelope = {
  success: boolean
  data?: {
    userId: string
    username: string
    email: string
  }
  error?: {
    code?: string
    message?: string
  }
}

export async function registerPortalUser(body: PublicRegisterBody): Promise<void> {
  const { data } = await apiClient.post<RegisterEnvelope>('/api/public/register', body)
  if (!data.success) {
    const msg = data.error?.message ?? 'Registration failed'
    throw new Error(msg)
  }
}

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

type UsernameAvailabilityEnvelope = {
  success: boolean
  data?: {
    normalizedUsername: string
    available: boolean
    suggestions: string[]
  }
  error?: {
    code?: string
    message?: string
  }
}

export async function sendRegistrationVerificationCode(email: string): Promise<{ expiresInSeconds: number; resendInSeconds: number }> {
  const { data } = await apiClient.post<SendCodeEnvelope>('/api/public/register/send-code', { email })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Verification code send failed')
  }
  return {
    expiresInSeconds: data.data.expiresInSeconds,
    resendInSeconds: data.data.resendInSeconds,
  }
}

export async function checkUsernameAvailability(username: string): Promise<{
  normalizedUsername: string
  available: boolean
  suggestions: string[]
}> {
  const { data } = await apiClient.get<UsernameAvailabilityEnvelope>('/api/public/register/username-availability', {
    params: { username },
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Username availability check failed')
  }
  return {
    normalizedUsername: data.data.normalizedUsername,
    available: data.data.available,
    suggestions: Array.isArray(data.data.suggestions) ? data.data.suggestions : [],
  }
}
