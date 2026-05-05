import { apiClient } from './client'

export type PublicRegisterBody = {
  email: string
  username: string
  password: string
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
