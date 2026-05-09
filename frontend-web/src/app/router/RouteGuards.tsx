import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { isAdminUser, isAuthenticated } from '../../shared/auth/session'

type GuardProps = {
  children: ReactNode
}

export function PublicOnly({ children }: GuardProps) {
  return isAuthenticated() ? <Navigate to="/app" replace /> : <>{children}</>
}

export function RequireAuth({ children }: GuardProps) {
  return isAuthenticated() ? <>{children}</> : <Navigate to="/" replace />
}

export function RequireAdmin({ children }: GuardProps) {
  if (!isAuthenticated()) {
    return <Navigate to="/" replace />
  }
  if (!isAdminUser()) {
    return <Navigate to="/app" replace />
  }
  return <>{children}</>
}

