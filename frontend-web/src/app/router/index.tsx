import { Navigate, createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../../shared/components/layout/AppLayout'
import { RootLayout } from '../../shared/components/layout/RootLayout'
import { DashboardPage } from '../../pages/dashboard/DashboardPage'
import { ExternalPortfolioPage } from '../../pages/external-portfolio/ExternalPortfolioPage'
import { SimulationPage } from '../../pages/simulation/SimulationPage'
import { MarketsPage } from '../../pages/markets/MarketsPage'
import { MyPortfolioPage } from '../../pages/my-portfolio/MyPortfolioPage'
import { NewsPage } from '../../pages/news/NewsPage'
import { AnalysisPage } from '../../pages/analysis/AnalysisPage'
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
        path: 'markets',
        element: <MarketsPage />,
      },
      {
        path: 'my-portfolio',
        element: (
          <RequireAuth>
            <MyPortfolioPage />
          </RequireAuth>
        ),
      },
      {
        path: 'news',
        element: <NewsPage />,
      },
      {
        path: 'analysis',
        element: <AnalysisPage />,
      },
      {
        path: 'app',
        element: (
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        ),
        children: [
          { index: true, element: <Navigate to="/app/markets" replace /> },
          { path: 'markets', element: <MarketsPage /> },
          { path: 'my-portfolio', element: <MyPortfolioPage /> },
          { path: 'news', element: <NewsPage /> },
          { path: 'analysis', element: <AnalysisPage /> },
          { path: 'dashboard', element: <DashboardPage /> },
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

