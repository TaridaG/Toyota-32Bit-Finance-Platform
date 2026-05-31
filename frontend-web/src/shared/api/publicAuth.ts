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
  data?: {
    status?: string
    accessToken?: string
    expiresIn?: number
    tokenType?: string
    refreshToken?: string | null
    refreshExpiresIn?: number | null
    mfaChallengeId?: string
  }
  error?: {
    code?: string
    message?: string
  }
}

export type PortalLoginResult =
  | { kind: 'complete'; tokens: PortalLoginTokens }
  | { kind: 'mfaRequired'; challengeId: string }

function mapTokens(data: NonNullable<LoginEnvelope['data']>): PortalLoginTokens {
  if (!data.accessToken) {
    throw new Error('Login failed')
  }
  return {
    accessToken: data.accessToken,
    expiresIn: data.expiresIn ?? 300,
    tokenType: data.tokenType ?? 'Bearer',
    refreshToken: data.refreshToken,
    refreshExpiresIn: data.refreshExpiresIn,
  }
}

function parseLoginEnvelope(data: LoginEnvelope): PortalLoginResult {
  if (!data.success || !data.data) {
    const msg = data.error?.message ?? 'Login failed'
    throw new Error(msg)
  }
  if (data.data.status === 'MFA_REQUIRED' || data.data.mfaChallengeId) {
    const challengeId = data.data.mfaChallengeId
    if (!challengeId) {
      throw new Error('Login failed')
    }
    return { kind: 'mfaRequired', challengeId }
  }
  return { kind: 'complete', tokens: mapTokens(data.data) }
}

export async function loginWithPortalPassword(username: string, password: string): Promise<PortalLoginResult> {
  const { data } = await apiClient.post<LoginEnvelope>('/api/v1/public/login', { username, password })
  return parseLoginEnvelope(data)
}

export async function verifyPortalLoginMfa(
  challengeId: string,
  code: string,
  trustDevice: boolean,
): Promise<PortalLoginTokens> {
  const { data } = await apiClient.post<LoginEnvelope>('/api/v1/public/login/mfa', {
    challengeId,
    code,
    trustDevice,
  })
  if (!data.success || !data.data?.accessToken) {
    const msg = data.error?.message ?? 'Verification failed'
    throw new Error(msg)
  }
  return mapTokens(data.data)
}
