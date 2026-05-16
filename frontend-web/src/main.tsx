import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import './index.css'
import { appRouter } from './app/router'
import { ThemeProvider } from './shared/theme/ThemeProvider'
import './shared/i18n'
import { AppPreferencesProvider } from './shared/preferences/AppPreferencesContext'
import { LiteracyHelpModeProvider } from './features/literacy-help/LiteracyHelpModeContext'
import { InfoCardsProvider } from './features/info-cards/InfoCardsProvider'
import { AdminInfoCardPickProvider } from './features/admin-info-card-pick/AdminInfoCardPickContext'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppPreferencesProvider>
      <ThemeProvider>
        <InfoCardsProvider>
          <LiteracyHelpModeProvider>
            <AdminInfoCardPickProvider>
              <RouterProvider router={appRouter} />
            </AdminInfoCardPickProvider>
          </LiteracyHelpModeProvider>
        </InfoCardsProvider>
      </ThemeProvider>
    </AppPreferencesProvider>
  </StrictMode>,
)
