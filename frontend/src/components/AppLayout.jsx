import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const navLinks = [
  { to: '/', label: 'Dashboard' },
  { to: '/jobs', label: 'Jobs' },
  { to: '/companies', label: 'Companies' },
  { to: '/applications', label: 'Applications' },
  { to: '/notifications', label: 'Notifications' },
  { to: '/profile', label: 'Profile' },
  { to: '/monitoring', label: 'Monitoring', adminOnly: true },
]

export default function AppLayout({ children }) {
  const { isAuthenticated, user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const isAuthScreen = location.pathname === '/auth'

  const visibleLinks = isAuthenticated
    ? navLinks.filter((link) => !link.adminOnly || user?.role === 'ADMIN')
    : []

  async function handleLogout() {
    await logout()
    navigate('/auth')
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 antialiased">
      <header className="relative overflow-hidden border-b border-slate-200/80 bg-white/95 backdrop-blur">
        <div className="absolute inset-x-0 top-0 h-24 bg-gradient-to-r from-cyan-200/35 via-amber-100/55 to-emerald-200/35" />
        <div className="relative mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-3.5 md:py-4">
          <div className="space-y-1">
            <Link to={isAuthenticated ? '/' : '/auth'} className="nav-title text-xl font-black tracking-tight text-slate-900 md:text-[1.35rem]">
              Job Portal Control Deck
            </Link>
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
              Internal Operations Portal
            </p>
          </div>

          <nav className="flex flex-wrap items-center gap-2 md:gap-2.5">
            {visibleLinks.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `rounded-full border px-3 py-1.5 text-sm font-semibold transition-colors ${
                    isActive
                      ? 'border-slate-900 bg-slate-900 text-white shadow-sm'
                      : 'border-slate-200 bg-slate-100/85 text-slate-700 hover:bg-slate-200'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            {isAuthenticated ? (
              <>
                <div className="hidden text-right sm:block">
                  <p className="text-xs font-semibold text-slate-500">{user?.email}</p>
                  <p className="text-xs text-slate-400">Authenticated session</p>
                </div>
                <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-bold uppercase text-emerald-700">
                  {user?.role || 'User'}
                </span>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="rounded-full bg-rose-500 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-600"
                >
                  Logout
                </button>
              </>
            ) : (
              !isAuthScreen && (
                <Link
                  to="/auth"
                  className="rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700"
                >
                  Login
                </Link>
              )
            )}
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl px-4 py-8">{children}</main>
    </div>
  )
}
