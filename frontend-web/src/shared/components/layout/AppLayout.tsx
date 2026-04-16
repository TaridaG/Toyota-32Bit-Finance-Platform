import { NavLink, Outlet } from 'react-router-dom'

const navItems = [
  { to: '/', label: 'Ana Sayfa', end: true },
  { to: '/portfolio', label: 'Portföy' },
  { to: '/simulation', label: 'Simülasyon' },
]

export function AppLayout() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <h2 className="sidebar-title">Finans Uygulaması</h2>
        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `sidebar-link${isActive ? ' sidebar-link-active' : ''}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="content-shell">
        <header className="topbar">
          <h1>Finans Platformu</h1>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

