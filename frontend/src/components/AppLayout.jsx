import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { api } from '../lib/api'

const navLinks = [
  { to: '/', label: 'Dashboard' },
  { to: '/jobs', label: 'Jobs' },
  { to: '/companies', label: 'Companies' },
  { to: '/applications', label: 'Applications' },
  { to: '/notifications', label: 'Notifications' },
  { to: '/monitoring', label: 'Monitoring', adminOnly: true },
]

function initialsFromEmail(email) {
  if (!email) return '?'
  const name = email.split('@')[0]
  return name.slice(0, 2).toUpperCase()
}

function getInitialTheme() {
  if (typeof document !== 'undefined') {
    const current = document.documentElement.dataset.theme
    if (current === 'light' || current === 'dark') return current
  }
  if (typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark'
  }
  return 'light'
}

function LogoMark() {
  return (
    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-700 text-sm font-black text-white shadow-[0_10px_24px_-10px_rgba(76,79,224,0.7)] transition-all duration-300 group-hover:rotate-[-6deg] group-hover:shadow-[0_10px_28px_-8px_rgba(76,79,224,0.9)]">
      JP
    </span>
  )
}

function SunIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-4 w-4">
      <circle cx="12" cy="12" r="4.2" stroke="currentColor" strokeWidth="1.8" />
      <path
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        d="M12 2.5v2.2M12 19.3v2.2M4.9 4.9l1.55 1.55M17.55 17.55l1.55 1.55M2.5 12h2.2M19.3 12h2.2M4.9 19.1l1.55-1.55M17.55 6.45l1.55-1.55"
      />
    </svg>
  )
}

function MoonIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-4 w-4">
      <path
        d="M20.5 14.7A8.4 8.4 0 1 1 9.3 3.5a6.8 6.8 0 0 0 11.2 11.2Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function UserIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-4 w-4">
      <circle cx="12" cy="8" r="3.4" stroke="currentColor" strokeWidth="1.8" />
      <path d="M4.5 20c1.4-3.6 4.4-5.5 7.5-5.5s6.1 1.9 7.5 5.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function GearIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-4 w-4">
      <circle cx="12" cy="12" r="2.8" stroke="currentColor" strokeWidth="1.8" />
      <path
        d="M12 3.5v2M12 18.5v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M3.5 12h2M18.5 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </svg>
  )
}

function LogoutIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-4 w-4">
      <path
        d="M9 4H6a1.5 1.5 0 0 0-1.5 1.5v13A1.5 1.5 0 0 0 6 20h3M16 16.5 20.5 12 16 7.5M20 12H9"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function BellIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-4 w-4">
      <path
        d="M6 9.5a6 6 0 1 1 12 0c0 4 1.5 5.5 1.5 5.5h-15S6 13.5 6 9.5Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M10.3 18.5a1.8 1.8 0 0 0 3.4 0" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function NotificationBell({ className = '' }) {
  const { data: unreadCount } = useQuery({
    queryKey: ['notifications', 'unreadCount'],
    queryFn: async () => {
      const response = await api.get('/api/notifications/unread/count')
      return response.data.count
    },
    refetchInterval: 30000,
    refetchOnWindowFocus: true,
  })

  const hasUnread = typeof unreadCount === 'number' && unreadCount > 0

  return (
    <Link
      to="/notifications"
      aria-label={hasUnread ? `${unreadCount} unread notifications` : 'Notifications'}
      className={`theme-toggle relative ${className}`}
    >
      <BellIcon />
      {hasUnread ? (
        <span className="absolute -right-1 -top-1 h-4 min-w-4 rounded-full">
          <span className="badge-pulse" />
          <span className="relative flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        </span>
      ) : null}
    </Link>
  )
}

function ThemeToggle({ theme, onToggle, className = '' }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to night mode'}
      title={theme === 'dark' ? 'Switch to light mode' : 'Switch to night mode'}
      className={`theme-toggle ${className}`}
    >
      {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
    </button>
  )
}

function ProfileMenu({ user, onLogout, className = '' }) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef(null)

  useEffect(() => {
    function handlePointer(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) setOpen(false)
    }
    function handleKey(event) {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', handlePointer)
    document.addEventListener('keydown', handleKey)
    return () => {
      document.removeEventListener('mousedown', handlePointer)
      document.removeEventListener('keydown', handleKey)
    }
  }, [])

  return (
    <div ref={rootRef} className={`relative ${className}`}>
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="menu"
        aria-label="Open account menu"
        aria-expanded={open}
        className="flex items-center justify-center rounded-full transition-transform hover:scale-105"
      >
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-black text-indigo-700 ring-2 ring-transparent transition-shadow hover:ring-indigo-200">
          {initialsFromEmail(user?.email)}
        </span>
      </button>

      {open ? (
        <div
          role="menu"
          className="dropdown-panel dropdown-in absolute right-0 top-[calc(100%+0.65rem)] w-64 p-2"
        >
          <div className="px-3 py-2.5">
            <p className="truncate text-sm font-semibold text-slate-900">{user?.email}</p>
            <span className="badge badge-info mt-1.5">{user?.role || 'User'}</span>
          </div>
          <div className="my-1 border-t border-slate-100" />
          <Link to="/profile" onClick={() => setOpen(false)} className="menu-item">
            <UserIcon />
            View profile
          </Link>
          <Link to="/profile#settings" onClick={() => setOpen(false)} className="menu-item">
            <GearIcon />
            Account settings
          </Link>
          <div className="my-1 border-t border-slate-100" />
          <button
            type="button"
            onClick={() => {
              setOpen(false)
              onLogout()
            }}
            className="menu-item menu-item-danger"
          >
            <LogoutIcon />
            Logout
          </button>
        </div>
      ) : null}
    </div>
  )
}

export default function AppLayout({ children }) {
  const { isAuthenticated, user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const isAuthScreen = location.pathname === '/auth'
  const [menuOpen, setMenuOpen] = useState(false)
  const [theme, setTheme] = useState(getInitialTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    window.localStorage.setItem('jp-theme', theme)
  }, [theme])

  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (location.hash) {
      const target = document.getElementById(location.hash.slice(1))
      if (target) {
        target.scrollIntoView({ behavior: 'smooth', block: 'start' })
        return
      }
    }
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  }, [location.pathname, location.hash])

  function toggleTheme() {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'))
  }

  const visibleLinks = isAuthenticated
    ? navLinks.filter((link) => !link.adminOnly || user?.role === 'ADMIN')
    : []

  async function handleLogout() {
    await logout()
    setMenuOpen(false)
    navigate('/auth')
  }

  return (
    <div className="flex min-h-screen flex-col text-slate-900 antialiased">
      <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/85 backdrop-blur-md">
        <div className="mx-auto flex w-full max-w-[1680px] items-center gap-2 px-4 py-3 sm:px-6 sm:gap-4 md:py-3.5 lg:px-10">
          <div className="flex min-w-0 items-center gap-3 lg:gap-5">
            <Link
              to={isAuthenticated ? '/' : '/auth'}
              className="nav-title group flex shrink-0 items-center gap-3"
              onClick={() => setMenuOpen(false)}
            >
              <LogoMark />
              <span className="hidden leading-tight sm:block">
                <span className="block text-[1.05rem] font-black tracking-tight text-slate-900">
                  Job Portal
                </span>
                <span className="block text-[0.65rem] font-bold uppercase tracking-[0.2em] text-indigo-500">
                  Control Deck
                </span>
              </span>
            </Link>

            {visibleLinks.length > 0 ? (
              <span aria-hidden className="hidden h-6 w-px shrink-0 bg-slate-200 lg:block" />
            ) : null}

            <nav className="hidden flex-wrap items-center gap-1.5 lg:flex">
              {visibleLinks.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `rounded-full px-3.5 py-1.5 text-sm font-semibold whitespace-nowrap transition-colors ${
                      isActive
                        ? 'bg-slate-900 text-white shadow-sm'
                        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>

          <div className="ml-auto flex shrink-0 items-center gap-2">
            <ThemeToggle theme={theme} onToggle={toggleTheme} className="hidden sm:inline-flex" />
            {isAuthenticated ? (
              <>
                <NotificationBell className="hidden sm:inline-flex" />
                <ProfileMenu user={user} onLogout={handleLogout} className="hidden sm:block" />
                <button
                  type="button"
                  onClick={() => setMenuOpen((prev) => !prev)}
                  aria-label="Toggle navigation menu"
                  aria-expanded={menuOpen}
                  className="btn btn-secondary btn-sm !px-2.5 lg:hidden"
                >
                  <span className="flex flex-col gap-[3px]">
                    <span className="block h-[2px] w-4 rounded-full bg-slate-700" />
                    <span className="block h-[2px] w-4 rounded-full bg-slate-700" />
                    <span className="block h-[2px] w-4 rounded-full bg-slate-700" />
                  </span>
                </button>
              </>
            ) : (
              !isAuthScreen && (
                <Link to="/auth" className="btn btn-primary btn-sm">
                  Login
                </Link>
              )
            )}
          </div>
        </div>

        {isAuthenticated && menuOpen ? (
          <div className="border-t border-slate-200/80 bg-white/95 px-4 py-3 lg:hidden">
            <div className="mb-3 flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span className="flex h-9 w-9 items-center justify-center rounded-full bg-indigo-100 text-xs font-black text-indigo-700">
                  {initialsFromEmail(user?.email)}
                </span>
                <div className="leading-tight">
                  <p className="max-w-[14rem] truncate text-xs font-semibold text-slate-700">{user?.email}</p>
                  <span className="badge badge-info">{user?.role || 'User'}</span>
                </div>
              </div>
              <ThemeToggle theme={theme} onToggle={toggleTheme} className="sm:hidden" />
            </div>
            <nav className="flex flex-col gap-1">
              {visibleLinks.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={() => setMenuOpen(false)}
                  className={({ isActive }) =>
                    `rounded-xl px-3.5 py-2 text-sm font-semibold transition-colors ${
                      isActive ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
              <Link
                to="/profile#settings"
                onClick={() => setMenuOpen(false)}
                className="rounded-xl px-3.5 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100"
              >
                Account settings
              </Link>
            </nav>
            <button type="button" onClick={handleLogout} className="btn btn-danger btn-sm mt-3 w-full">
              Logout
            </button>
          </div>
        ) : null}
      </header>

      <main className="mx-auto w-full max-w-[1680px] flex-1 px-4 py-6 sm:px-6 sm:py-8 lg:px-10">{children}</main>

      <footer className="border-t border-slate-200/70 px-4 py-5 text-center text-xs text-slate-400">
        Job Portal Control Deck &middot; internal operations tooling
      </footer>
    </div>
  )
}
