import { Navigate, createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../../shared/components/layout/AppLayout'
import { RootLayout } from '../../shared/components/layout/RootLayout'
import { DashboardPage } from '../../pages/dashboard/DashboardPage'
import { ExternalPortfolioPage } from '../../pages/external-portfolio/ExternalPortfolioPage'
import { SimulationPage } from '../../pages/simulation/SimulationPage'
import { LandingPage } from '../../pages/public/LandingPage'
import { LoginPage } from '../../pages/public/LoginPage'
import { RegisterPage } from '../../pages/public/RegisterPage'
import { PublicOnly, RequireAuth } from './RouteGuards'

export const appRouter = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      {
        index: true,
        element: (
          <PublicOnly>
            <LandingPage />
          </PublicOnly>
        ),
      },
      {
        path: 'login',
        element: (
          <PublicOnly>
            <LoginPage />
          </PublicOnly>
        ),
      },
      {
        path: 'register',
        element: (
          <PublicOnly>
            <RegisterPage />
          </PublicOnly>
        ),
      },
      {
        path: 'app',
        element: (
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        ),
        children: [
          { index: true, element: <DashboardPage /> },
          { path: 'portfolio', element: <ExternalPortfolioPage /> },
          { path: 'simulation', element: <SimulationPage /> },
        ],
      },
    ],
  },
  {
    path: '/portfolio',
    element: <Navigate to="/app/portfolio" replace />,
  },
  {
    path: '/simulation',
    element: <Navigate to="/app/simulation" replace />,
  },
])

