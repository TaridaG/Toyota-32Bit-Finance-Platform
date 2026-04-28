import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import './index.css'
import { appRouter } from './app/router'
import { ThemeProvider } from './shared/theme/ThemeProvider'
import './shared/i18n'
import { AppPreferencesProvider } from './shared/preferences/AppPreferencesContext'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppPreferencesProvider>
      <ThemeProvider>
        <RouterProvider router={appRouter} />
      </ThemeProvider>
    </AppPreferencesProvider>
  </StrictMode>,
)
