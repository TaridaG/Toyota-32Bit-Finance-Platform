export type PortalProfile = {
  email: string
  username: string
  phone: string | null
  notifySecurityAlerts: boolean
  notifyProductUpdates: boolean
  /** ISO-8601 instant from the server; null if no profile photo is stored. */
  avatarUpdatedAt: string | null
}

export type ApiEnvelope<T> = {
  success: boolean
  data?: T
  error?: { code?: string; message?: string }
}
