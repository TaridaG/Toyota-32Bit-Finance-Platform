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
    suggestions?: string[]
  }
}

type EmailAvailabilityEnvelope = {
  success: boolean
  data?: {
    normalizedEmail: string
    available: boolean
    blocked: boolean
    suggestions: string[]
  }
  error?: {
    code?: string
    message?: string
  }
}

export async function registerPortalUser(body: PublicRegisterBody): Promise<void> {
  const { data } = await apiClient.post<RegisterEnvelope>('/api/v1/public/register', body)
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
    suggestions?: string[]
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
    suggestions?: string[]
  }
}

export async function sendRegistrationVerificationCode(
  email: string,
  locale?: string,
): Promise<{ expiresInSeconds: number; resendInSeconds: number }> {
  const { data } = await apiClient.post<SendCodeEnvelope>('/api/v1/public/register/send-code', {
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

export async function checkEmailAvailability(email: string): Promise<{
  normalizedEmail: string
  available: boolean
  blocked: boolean
  suggestions: string[]
}> {
  const { data } = await apiClient.get<EmailAvailabilityEnvelope>('/api/v1/public/register/email-availability', {
    params: { email },
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Email availability check failed')
  }
  return {
    normalizedEmail: data.data.normalizedEmail,
    available: data.data.available,
    blocked: data.data.blocked,
    suggestions: Array.isArray(data.data.suggestions) ? data.data.suggestions : [],
  }
}

export async function checkUsernameAvailability(username: string): Promise<{
  normalizedUsername: string
  available: boolean
  suggestions: string[]
}> {
  const { data } = await apiClient.get<UsernameAvailabilityEnvelope>('/api/v1/public/register/username-availability', {
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
