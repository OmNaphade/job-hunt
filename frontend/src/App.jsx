import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import { useAuth } from './context/AuthContext'
import ApplicationsPage from './pages/ApplicationsPage'
import AuthPage from './pages/AuthPage'
import CompaniesPage from './pages/CompaniesPage'
import DashboardPage from './pages/DashboardPage'
import JobsPage from './pages/JobsPage'
import MonitoringPage from './pages/MonitoringPage'
import NotFoundPage from './pages/NotFoundPage'
import NotificationsPage from './pages/NotificationsPage'
import ProfilePage from './pages/ProfilePage'
import UnauthorizedPage from './pages/UnauthorizedPage'

function RequireAuth({ children }) {
  const { isAuthenticated, loading } = useAuth()

  if (loading) {
    return <p className="px-4 py-16 text-center text-slate-500">Loading session...</p>
  }

  if (!isAuthenticated) {
    return <Navigate to="/auth" replace />
  }

  return children
}

function PublicOnly({ children }) {
  const { isAuthenticated, loading } = useAuth()

  if (loading) {
    return <p className="px-4 py-16 text-center text-slate-500">Checking access...</p>
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return children
}

function RequireRole({ children, roles, reason }) {
  const { isAuthenticated, loading, role } = useAuth()

  if (loading) {
    return <p className="px-4 py-16 text-center text-slate-500">Authorizing access...</p>
  }

  if (!isAuthenticated) {
    return <Navigate to="/auth" replace />
  }

  if (!roles.includes(role)) {
    return (
      <UnauthorizedPage
        requiredRole={roles.join(' or ')}
        currentRole={role}
        reason={reason || 'You are authenticated but not authorized for this route.'}
      />
    )
  }

  return children
}

function App() {
  return (
    <AppLayout>
      <Routes>
        <Route
          path="/"
          element={
            <RequireAuth>
              <DashboardPage />
            </RequireAuth>
          }
        />
        <Route
          path="/auth"
          element={
            <PublicOnly>
              <AuthPage />
            </PublicOnly>
          }
        />
        <Route
          path="/jobs"
          element={
            <RequireAuth>
              <JobsPage />
            </RequireAuth>
          }
        />
        <Route
          path="/companies"
          element={
            <RequireAuth>
              <CompaniesPage />
            </RequireAuth>
          }
        />
        <Route
          path="/applications"
          element={
            <RequireAuth>
              <ApplicationsPage />
            </RequireAuth>
          }
        />
        <Route
          path="/notifications"
          element={
            <RequireAuth>
              <NotificationsPage />
            </RequireAuth>
          }
        />
        <Route
          path="/profile"
          element={
            <RequireAuth>
              <ProfilePage />
            </RequireAuth>
          }
        />
        <Route
          path="/monitoring"
          element={
            <RequireRole
              roles={['ADMIN']}
              reason="Monitoring dashboard is restricted to admin operators for security and control."
            >
              <MonitoringPage />
            </RequireRole>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AppLayout>
  )
}

export default App
