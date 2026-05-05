import { apiClient } from './client'

export type PortalLoginTokens = {
  accessToken: string
  expiresIn: number
  tokenType: string
  refreshToken?: string | null
  refreshExpiresIn?: number | null
}

type LoginEnvelope = {
  success: boolean
  data?: PortalLoginTokens
  error?: {
    code?: string
    message?: string
  }
}

export async function loginWithPortalPassword(username: string, password: string): Promise<PortalLoginTokens> {
  const { data } = await apiClient.post<LoginEnvelope>('/api/public/login', { username, password })
  if (!data.success || !data.data?.accessToken) {
    const msg = data.error?.message ?? 'Login failed'
    throw new Error(msg)
  }
  return data.data
}
