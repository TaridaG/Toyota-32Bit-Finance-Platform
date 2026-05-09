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
import { ProfileSettingsPage } from '../../pages/profile/ProfileSettingsPage'
import { LandingPage } from '../../pages/public/LandingPage'
import { LoginPage } from '../../pages/public/LoginPage'
import { RegisterPage } from '../../pages/public/RegisterPage'
import { PublicOnly, RequireAdmin, RequireAuth } from './RouteGuards'
import { AdminLayout } from '../../pages/admin/AdminLayout'
import { AdminOverviewPage } from '../../pages/admin/AdminOverviewPage'
import { AdminPlaceholderPage } from '../../pages/admin/AdminPlaceholderPage'
import { ADMIN_KPI_TOTAL_USERS_PATH, ADMIN_SECTION_ROUTES } from '../../features/admin/adminSectionRoutes'
import { AdminTotalUsersPage } from '../../pages/admin/AdminTotalUsersPage'

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
        path: 'admin',
        element: (
          <RequireAuth>
            <RequireAdmin>
              <AdminLayout />
            </RequireAdmin>
          </RequireAuth>
        ),
        children: [
          { index: true, element: <AdminOverviewPage /> },
          { path: ADMIN_KPI_TOTAL_USERS_PATH, element: <AdminTotalUsersPage /> },
          ...ADMIN_SECTION_ROUTES.filter((r) => r.path !== ADMIN_KPI_TOTAL_USERS_PATH).map((r) => ({
            path: r.path,
            element: <AdminPlaceholderPage titleKey={r.titleKey} leadKey={r.leadKey} />,
          })),
          { path: 'users', element: <Navigate to="/admin/kpi/total-users" replace /> },
          { path: 'portfolios', element: <Navigate to="/admin/kpi/active-portfolios" replace /> },
          { path: 'market-data', element: <Navigate to="/admin/kpi/market-streams" replace /> },
          { path: 'news', element: <Navigate to="/admin/kpi/news-sources" replace /> },
          { path: 'system', element: <Navigate to="/admin/kpi/system-status" replace /> },
        ],
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
          { path: 'profile', element: <ProfileSettingsPage /> },
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

