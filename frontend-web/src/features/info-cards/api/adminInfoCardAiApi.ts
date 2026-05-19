import { apiClient } from '../../../shared/api/client'
import type {
  CompleteInfoCardAiRequest,
  InfoCardAiContent,
  TranslateInfoCardAiRequest,
} from '../../../types/infoCardAi'

type ApiEnvelope<T> = {
  success: boolean
  data: T
  error?: { code?: string; message?: string }
}

const AI_REQUEST_TIMEOUT_MS = 90_000

function unwrap<T>(payload: ApiEnvelope<T>): T {
  if (!payload?.success) {
    const code = payload?.error?.code
    const message = payload?.error?.message ?? 'AI request failed'
    const err = new Error(message) as Error & { code?: string }
    if (code) {
      err.code = code
    }
    throw err
  }
  return payload.data
}

function enrichAxiosError(error: unknown): unknown {
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as {
      response?: { data?: ApiEnvelope<unknown> }
      message?: string
    }
    const apiError = axiosError.response?.data?.error
    if (apiError?.code) {
      const enriched = new Error(apiError.message ?? axiosError.message ?? 'AI request failed') as Error & {
        code?: string
      }
      enriched.code = apiError.code
      return enriched
    }
  }
  return error
}

export async function completeInfoCardWithAi(payload: CompleteInfoCardAiRequest): Promise<InfoCardAiContent> {
  try {
    const { data } = await apiClient.post<ApiEnvelope<InfoCardAiContent>>(
      '/api/admin/info-cards/ai/complete',
      payload,
      { timeout: AI_REQUEST_TIMEOUT_MS },
    )
    return unwrap(data)
  } catch (error) {
    throw enrichAxiosError(error)
  }
}

export async function translateInfoCardWithAi(payload: TranslateInfoCardAiRequest): Promise<InfoCardAiContent> {
  try {
    const { data } = await apiClient.post<ApiEnvelope<InfoCardAiContent>>(
      '/api/admin/info-cards/ai/translate',
      payload,
      { timeout: AI_REQUEST_TIMEOUT_MS },
    )
    return unwrap(data)
  } catch (error) {
    throw enrichAxiosError(error)
  }
}
