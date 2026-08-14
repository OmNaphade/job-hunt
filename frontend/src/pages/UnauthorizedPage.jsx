import { Link } from 'react-router-dom'
import SectionCard from '../components/SectionCard'

export default function UnauthorizedPage({ requiredRole, currentRole, reason }) {
  return (
    <div className="mx-auto max-w-3xl">
      <SectionCard
        title="Access Restricted"
        subtitle="Your account is authenticated, but this area requires additional permissions."
      >
        <div className="space-y-4">
          <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
            <p className="text-sm font-semibold text-amber-800">Reason</p>
            <p className="mt-1 text-sm text-amber-900">{reason || 'You do not have access to this route.'}</p>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="tile">
              <p className="text-xs uppercase tracking-widest text-slate-500">Required Role</p>
              <p className="mt-1 text-sm font-bold text-slate-900">{requiredRole || 'N/A'}</p>
            </div>
            <div className="tile">
              <p className="text-xs uppercase tracking-widest text-slate-500">Your Role</p>
              <p className="mt-1 text-sm font-bold text-slate-900">{currentRole || 'UNKNOWN'}</p>
            </div>
          </div>

          <div className="flex flex-wrap gap-2">
            <Link
              to="/"
              className="btn btn-dark"
            >
              Back to Dashboard
            </Link>
            <Link
              to="/auth"
              className="btn btn-secondary"
            >
              Switch Account
            </Link>
          </div>
        </div>
      </SectionCard>
    </div>
  )
}
