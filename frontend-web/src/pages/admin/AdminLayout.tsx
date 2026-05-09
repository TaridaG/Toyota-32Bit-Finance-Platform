import { NavLink, Outlet } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ADMIN_SIDEBAR_LINKS } from '../../features/admin/adminSectionRoutes'

export function AdminLayout() {
  const { t } = useTranslation('admin')

  return (
    <div className="fi-admin-shell">
      <aside className="fi-admin-sidebar" aria-label={t('sidebarAria')}>
        <div className="fi-admin-sidebar-head">
          <span className="fi-admin-sidebar-kicker">{t('kicker')}</span>
          <strong className="fi-admin-sidebar-title">{t('title')}</strong>
        </div>
        <nav className="fi-admin-nav">
          {ADMIN_SIDEBAR_LINKS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `fi-admin-nav-link${isActive ? ' fi-admin-nav-link-active' : ''}`}
            >
              {t(item.labelKey)}
            </NavLink>
          ))}
        </nav>
        <p className="fi-admin-sidebar-foot">{t('mockBanner')}</p>
      </aside>
      <div className="fi-admin-main">
        <Outlet />
      </div>
    </div>
  )
}
