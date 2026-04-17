import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { isAuthenticated } from '../../shared/auth/session'

type GuardProps = {
  children: ReactNode
}

export function PublicOnly({ children }: GuardProps) {
  return isAuthenticated() ? <Navigate to="/app" replace /> : <>{children}</>
}

export function RequireAuth({ children }: GuardProps) {
  return isAuthenticated() ? <>{children}</> : <Navigate to="/" replace />
}

