import { Link, useLocation } from 'react-router-dom'
import SectionCard from '../components/SectionCard'
import { useAuth } from '../context/AuthContext'

export default function NotFoundPage() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  return (
    <div className="mx-auto max-w-3xl">
      <SectionCard title="Page not found" subtitle="There's nothing here.">
        <div className="space-y-4">
          <div className="tile">
            <p className="eyebrow">Requested path</p>
            <p className="mt-1 break-all text-sm font-bold text-slate-900">{location.pathname}</p>
          </div>

          <p className="text-sm text-slate-600">
            The page you were looking for doesn&rsquo;t exist, moved, or the link is out of date.
          </p>

          <div className="flex flex-wrap gap-2">
            <Link to={isAuthenticated ? '/' : '/auth'} className="btn btn-primary">
              {isAuthenticated ? 'Back to Dashboard' : 'Go to Login'}
            </Link>
          </div>
        </div>
      </SectionCard>
    </div>
  )
}
