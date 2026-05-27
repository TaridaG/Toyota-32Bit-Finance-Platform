import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

type AdminEnvelope<T> = { success: boolean; data?: T | null; error?: { message?: string } }

export type AdminUserListItem = {
  id: string
  username: string
  email: string
  emailVerified: boolean
  createdAt: string
  hasProfileAvatar: boolean
  portfolioCount: number
  frozen: boolean
}

export type AdminUserDirectoryPage = {
  content: AdminUserListItem[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  sort: string
}

export type AdminHoldingLine = {
  symbol: string
  instrumentName: string
  marketValue: number
  weightPercent: number
}

export type AdminPortfolioDetail = {
  portfolioId: number
  name: string
  createdAt: string
  baseCurrency: string
  holdings: AdminHoldingLine[]
}

export type AdminUserPortfolioTree = {
  portfolios: AdminPortfolioDetail[]
}

export type AdminUserDirectoryQuery = {
  page: number
  size: 10 | 20 | 50
  /** Sort column (server whitelist). */
  sortField: string
  /** asc | desc */
  sortDir: 'asc' | 'desc'
  registeredFrom?: string
  registeredToExclusive?: string
  emailVerified?: boolean
  portfolioCount?: number
  search?: string
}

function describeFailure(e: unknown): string {
  if (isAxiosError(e)) {
    const status = e.response?.status
    const body = e.response?.data as { error?: { message?: string } } | undefined
    const serverMsg = body?.error?.message
    if (status === 403) return serverMsg ?? 'Forbidden — ADMIN role required.'
    if (status === 401) return serverMsg ?? 'Unauthorized — sign in again.'
    if (serverMsg) return serverMsg
    if (status != null) return `HTTP ${status}`
  }
  if (e instanceof Error) return e.message
  return 'admin user directory unavailable'
}

const USER_DIRECTORY_BASE = '/api/v1/admin/metrics/user-directory'

export async function fetchAdminUserDirectory(q: AdminUserDirectoryQuery): Promise<AdminUserDirectoryPage> {
  const params = new URLSearchParams()
  params.set('page', String(q.page))
  params.set('size', String(q.size))
  params.set('sortField', q.sortField)
  params.set('sortDir', q.sortDir)
  if (q.registeredFrom) params.set('registeredFrom', q.registeredFrom)
  if (q.registeredToExclusive) params.set('registeredToExclusive', q.registeredToExclusive)
  if (q.emailVerified !== undefined) params.set('emailVerified', String(q.emailVerified))
  if (q.portfolioCount !== undefined) params.set('portfolioCount', String(q.portfolioCount))
  if (q.search?.trim()) params.set('search', q.search.trim())
  try {
    const { data } = await apiClient.get<AdminEnvelope<AdminUserDirectoryPage>>(
      `${USER_DIRECTORY_BASE}?${params.toString()}`,
    )
    if (!data.success || data.data == null) {
      throw new Error('invalid response')
    }
    return data.data
  } catch (e) {
    throw new Error(describeFailure(e))
  }
}

/** Admin-only avatar JPEG (requires Authorization; use blob URL in UI). */
export async function fetchAdminUserPortfolioTree(userId: string): Promise<AdminUserPortfolioTree> {
  try {
    const { data } = await apiClient.get<AdminEnvelope<AdminUserPortfolioTree>>(
      `${USER_DIRECTORY_BASE}/${userId}/portfolios`,
    )
    if (!data.success || data.data == null) {
      throw new Error('invalid response')
    }
    return normalizePortfolioTree(data.data)
  } catch (e) {
    throw new Error(describeFailure(e))
  }
}

function normalizePortfolioTree(raw: AdminUserPortfolioTree): AdminUserPortfolioTree {
  return {
    portfolios: raw.portfolios.map((p) => ({
      ...p,
      holdings: (p.holdings ?? []).map((h) => ({
        symbol: h.symbol,
        instrumentName: h.instrumentName ?? '',
        marketValue: Number(h.marketValue),
        weightPercent: Number(h.weightPercent),
      })),
    })),
  }
}

export async function fetchAdminUserAvatarBlob(userId: string): Promise<Blob | null> {
  try {
    const res = await apiClient.get<ArrayBuffer>(`${USER_DIRECTORY_BASE}/${userId}/avatar`, {
      responseType: 'arraybuffer',
      validateStatus: (status) => status === 200 || status === 404,
    })
    if (res.status === 404 || !res.data || res.data.byteLength === 0) return null
    return new Blob([res.data], { type: 'image/jpeg' })
  } catch {
    return null
  }
}
