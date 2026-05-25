import { Navigate, createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../../shared/components/layout/AppLayout'
import { RootLayout } from '../../shared/components/layout/RootLayout'
import { DashboardPage } from '../../pages/dashboard/DashboardPage'
import { ExternalPortfolioPage } from '../../pages/external-portfolio/ExternalPortfolioPage'
import { MarketsPage } from '../../pages/markets/MarketsPage'
import { MyPortfolioPage } from '../../pages/my-portfolio/MyPortfolioPage'
import { NewsPage } from '../../pages/news/NewsPage'
import { AnalysisPage } from '../../pages/analysis/AnalysisPage'
import { FaizVadeliPage } from '../../pages/faiz-vadeli/FaizVadeliPage'
import { BankRatesPage } from '../../pages/bank-rates/BankRatesPage'
import { FinansalOkuryazarlikPage } from '../../pages/finansal-okuryazarlik/FinansalOkuryazarlikPage'
import { BilgiKartlariPage } from '../../pages/bilgi-kartlari/BilgiKartlariPage'
import { ProfileSettingsPage } from '../../pages/profile/ProfileSettingsPage'
import { AlarmsPage } from '../../pages/alarms/AlarmsPage'
import { NotificationsPage } from '../../pages/notifications/NotificationsPage'
import { LandingPage } from '../../pages/public/LandingPage'
import { LoginPage } from '../../pages/public/LoginPage'
import { RegisterPage } from '../../pages/public/RegisterPage'
import {
  PUBLIC_BANK_RATES_ROUTE,
  PUBLIC_FINANCIAL_LITERACY_ROUTE,
  PUBLIC_MARKETS_ROUTE,
} from '../routes/publicCatalogRoutes'
import { PublicOnly, RequireAdmin, RequireAuth } from './RouteGuards'
import { AdminLayout } from '../../pages/admin/AdminLayout'
import { AdminOverviewPage } from '../../pages/admin/AdminOverviewPage'
import { ADMIN_KPI_TOTAL_USERS_PATH } from '../../features/admin/adminSectionRoutes'
import { AdminAvgLatencyPage } from '../../pages/admin/AdminAvgLatencyPage'
import { AdminTotalNewsPage } from '../../pages/admin/AdminTotalNewsPage'
import { AdminTotalPortfoliosPage } from '../../pages/admin/AdminTotalPortfoliosPage'
import { AdminMarketAssetsPage } from '../../pages/admin/AdminMarketAssetsPage'
import { AdminBlockedEmailsPage } from '../../pages/admin/AdminBlockedEmailsPage'
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
        path: 'finansal-okuryazarlik',
        element: <FinansalOkuryazarlikPage />,
      },
      {
        path: 'bank-rates',
        element: <BankRatesPage />,
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
          { path: 'kpi/active-portfolios', element: <AdminTotalPortfoliosPage /> },
          { path: 'kpi/market-streams', element: <AdminMarketAssetsPage /> },
          { path: 'kpi/news-sources', element: <AdminTotalNewsPage /> },
          { path: 'kpi/avg-latency', element: <AdminAvgLatencyPage /> },
          { path: 'users', element: <Navigate to="/admin/kpi/total-users" replace /> },
          { path: 'portfolios', element: <Navigate to="/admin/kpi/active-portfolios" replace /> },
          { path: 'market-data', element: <Navigate to="/admin/kpi/market-streams" replace /> },
          { path: 'news', element: <Navigate to="/admin/kpi/news-sources" replace /> },
          { path: 'system', element: <Navigate to="/admin" replace /> },
          { path: 'kpi/system-status', element: <Navigate to="/admin" replace /> },
          { path: 'blocked-emails', element: <AdminBlockedEmailsPage /> },
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
          { index: true, element: <Navigate to={PUBLIC_MARKETS_ROUTE} replace /> },
          { path: 'turkiye-ekonomisi', element: <Navigate to={PUBLIC_MARKETS_ROUTE} replace /> },
          {
            path: 'finansal-okuryazarlik',
            element: <Navigate to={PUBLIC_FINANCIAL_LITERACY_ROUTE} replace />,
          },
          { path: 'bank-rates', element: <Navigate to={PUBLIC_BANK_RATES_ROUTE} replace /> },
          { path: 'markets', element: <Navigate to={PUBLIC_MARKETS_ROUTE} replace /> },
          { path: 'faiz-vadeli', element: <FaizVadeliPage /> },
          { path: 'my-portfolio', element: <MyPortfolioPage /> },
          { path: 'news', element: <NewsPage /> },
          { path: 'my-news', element: <Navigate to="/app/my-portfolio?section=news" replace /> },
          { path: 'analysis', element: <AnalysisPage /> },
          { path: 'dashboard', element: <DashboardPage /> },
          { path: 'portfolio', element: <ExternalPortfolioPage /> },
          { path: 'simulation', element: <Navigate to="/app/analysis" replace /> },
          { path: 'profile', element: <ProfileSettingsPage /> },
          { path: 'notifications', element: <NotificationsPage /> },
          { path: 'alarms', element: <AlarmsPage /> },
          {
            path: 'bilgi-kartlari',
            element: (
              <RequireAdmin>
                <BilgiKartlariPage />
              </RequireAdmin>
            ),
          },
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
    element: <Navigate to="/app/analysis" replace />,
  },
])

