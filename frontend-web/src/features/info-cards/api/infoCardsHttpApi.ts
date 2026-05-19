import { apiClient } from '../../../shared/api/client'
import type { InfoCard, InfoCardInput, InfoCardsDashboard, PortalPageKey } from '../../../types/infoCards'

type ApiEnvelope<T> = {
  success: boolean
  data: T
}

export type InfoCardsPageResponse = {
  content: InfoCard[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

function unwrap<T>(payload: ApiEnvelope<T>): T {
  if (!payload?.success) {
    throw new Error('Info cards API request failed')
  }
  return payload.data
}

function mapCard(raw: InfoCard): InfoCard {
  return {
    ...raw,
    targetElementIds: raw.targetElementIds ?? [],
    targetInstrumentSymbols: raw.targetInstrumentSymbols ?? [],
    relatedTerms: raw.relatedTerms ?? [],
    targetTerms: raw.targetTerms ?? [],
    pages: raw.pages ?? [],
    translations: raw.translations ?? undefined,
  }
}

export async function fetchPortalInfoCards(pageKey?: PortalPageKey, includeAdminOnly = false): Promise<InfoCard[]> {
  const { data } = await apiClient.get<ApiEnvelope<InfoCard[]>>('/api/portal/info-cards', {
    params: { page: pageKey, includeAdminOnly },
  })
  return unwrap(data).map(mapCard)
}

export async function lookupPortalInfoCard(
  pageKey: PortalPageKey,
  options: { term?: string; elementId?: string; instrumentSymbol?: string },
): Promise<InfoCard | undefined> {
  const { data } = await apiClient.get<ApiEnvelope<InfoCard | null>>('/api/portal/info-cards/lookup', {
    params: {
      page: pageKey,
      term: options.term,
      elementId: options.elementId,
      instrumentSymbol: options.instrumentSymbol,
    },
  })
  const card = unwrap(data)
  return card ? mapCard(card) : undefined
}

export async function fetchInfoCardBySlug(slug: string): Promise<InfoCard | undefined> {
  const { data } = await apiClient.get<ApiEnvelope<InfoCard | null>>(`/api/portal/info-cards/slug/${encodeURIComponent(slug)}`)
  const card = unwrap(data)
  return card ? mapCard(card) : undefined
}

export async function fetchAdminInfoCardsPage(params: {
  page: number
  size: number
  portalPage?: PortalPageKey
  query?: string
  status?: 'ALL' | 'ACTIVE' | 'PASSIVE'
}): Promise<InfoCardsPageResponse> {
  const { data } = await apiClient.get<ApiEnvelope<InfoCardsPageResponse>>('/api/admin/info-cards', {
    params: {
      page: params.page,
      size: params.size,
      portalPage: params.portalPage,
      query: params.query?.trim() || undefined,
      status: params.status ?? 'ACTIVE',
    },
  })
  const page = unwrap(data)
  return { ...page, content: page.content.map(mapCard) }
}

export async function fetchAdminInfoCardsDashboard(): Promise<InfoCardsDashboard> {
  const { data } = await apiClient.get<ApiEnvelope<InfoCardsDashboard>>('/api/admin/info-cards/dashboard')
  return unwrap(data)
}

export async function createAdminInfoCard(input: InfoCardInput): Promise<InfoCard> {
  const { data } = await apiClient.post<ApiEnvelope<InfoCard>>('/api/admin/info-cards', input)
  return mapCard(unwrap(data))
}

export async function updateAdminInfoCard(id: string, input: InfoCardInput): Promise<InfoCard> {
  const { data } = await apiClient.put<ApiEnvelope<InfoCard>>(`/api/admin/info-cards/${id}`, input)
  return mapCard(unwrap(data))
}

export async function toggleAdminInfoCardStatus(id: string): Promise<InfoCard> {
  const { data } = await apiClient.patch<ApiEnvelope<InfoCard>>(`/api/admin/info-cards/${id}/status`)
  return mapCard(unwrap(data))
}

export async function deleteAdminInfoCard(id: string): Promise<void> {
  await apiClient.delete(`/api/admin/info-cards/${id}`)
}

