import axios from 'axios'
import { isAuthenticated } from '../auth/session'

const DEFAULT_API_BASE_URL = 'http://localhost:8080'
const LANGUAGE_STORAGE_KEY = 'finance.locale'
const CURRENCY_STORAGE_KEY = 'finance.currency'

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

const normalizedBaseUrl =
  configuredBaseUrl && configuredBaseUrl.length > 0
    ? configuredBaseUrl.replace(/\/+$/, '')
    : DEFAULT_API_BASE_URL

export const apiClient = axios.create({
  baseURL: normalizedBaseUrl,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const language = window.localStorage.getItem(LANGUAGE_STORAGE_KEY)?.trim()
  const currency = window.localStorage.getItem(CURRENCY_STORAGE_KEY)?.trim()
  if (language) {
    config.headers['X-Language'] = language
  }
  if (currency) {
    config.headers['X-Currency'] = currency
  }

  if (isAuthenticated()) {
    const token = window.localStorage.getItem('finance.authToken')
    if (token && token.trim().length > 0) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

