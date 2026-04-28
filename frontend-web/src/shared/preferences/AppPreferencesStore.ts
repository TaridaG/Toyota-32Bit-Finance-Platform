import { createContext } from 'react'
import type { AppPreferencesContextValue } from './preferences'

export const AppPreferencesContext = createContext<AppPreferencesContextValue | null>(null)
