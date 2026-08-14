import { Link } from 'react-router-dom'
import SectionCard from '../components/SectionCard'
import { useAuth } from '../context/AuthContext'

const endpoints = [
  { name: 'Auth', port: 8081 },
  { name: 'User', port: 8082 },
  { name: 'Job', port: 8083 },
  { name: 'Company', port: 8084 },
  { name: 'Application', port: 8085 },
  { name: 'Notification', port: 8086 },
]

const quickLinksByRole = {
  JOB_SEEKER: [
    { to: '/jobs', label: 'Browse jobs', hint: 'Search & save openings' },
    { to: '/applications', label: 'My applications', hint: 'Track status & resumes' },
    { to: '/profile', label: 'Update profile', hint: 'Skills, headline, avatar' },
  ],
  RECRUITER: [
    { to: '/jobs', label: 'Post a job', hint: 'Publish new openings' },
    { to: '/companies', label: 'Manage company', hint: 'Recruiters & details' },
    { to: '/applications', label: 'Review applicants', hint: 'Shortlist & hire' },
  ],
  ADMIN: [
    { to: '/monitoring', label: 'Monitoring', hint: 'Live service health' },
    { to: '/companies', label: 'Companies', hint: 'Oversee org records' },
    { to: '/profile', label: 'User ops', hint: 'Password & account admin' },
  ],
}

export default function DashboardPage() {
  const { user, role } = useAuth()
  const quickLinks = quickLinksByRole[role] || quickLinksByRole.JOB_SEEKER

  return (
    <div className="space-y-6">
      <SectionCard
        title="Operations Overview"
        subtitle="Private control surface aligned to your Job Portal microservice architecture."
      >
        <div className="grid gap-4 md:grid-cols-3">
          <div className="tile-brand">
            <p className="eyebrow text-indigo-200">Active Identity</p>
            <p className="mt-2 truncate text-lg font-black">{user?.email}</p>
            <p className="text-sm text-indigo-200">Role: {role}</p>
          </div>
          <div className="tile-amber">
            <p className="eyebrow text-amber-700">Gateway Endpoint</p>
            <p className="mt-2 text-sm font-bold text-amber-900">http://localhost:8080/api</p>
            <p className="text-xs text-amber-800">Centralized ingress for all service calls</p>
          </div>
          <div className="tile-cyan">
            <p className="eyebrow text-cyan-700">Managed Domains</p>
            <p className="text-gradient-brand mt-2 text-3xl font-black">6</p>
            <p className="text-xs text-cyan-900/80">Auth, User, Job, Company, Application, Notification</p>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="Quick Actions" subtitle="Shortcuts tailored to your current role.">
        <div className="grid gap-3 sm:grid-cols-3">
          {quickLinks.map((link) => (
            <Link key={link.to} to={link.to} className="item-card group flex flex-col justify-between">
              <div>
                <p className="font-bold text-slate-900 group-hover:text-indigo-600">{link.label}</p>
                <p className="mt-1 text-sm text-slate-500">{link.hint}</p>
              </div>
              <span className="mt-3 inline-flex items-center gap-1 text-sm font-semibold text-indigo-600">
                <span>Open</span>
                <span aria-hidden className="transition-transform group-hover:translate-x-0.5">
                  →
                </span>
              </span>
            </Link>
          ))}
        </div>
      </SectionCard>

      <SectionCard title="Service Inventory" subtitle="Health-check map for local and staging verification.">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {endpoints.map((item) => (
            <article key={item.name} className="item-card-compact">
              <div className="flex items-center justify-between">
                <p className="font-bold text-slate-900">{item.name} Service</p>
                <span className="badge badge-neutral">:{item.port}</span>
              </div>
              <p className="mt-1 text-sm text-slate-600">http://localhost:{item.port}</p>
              <p className="text-xs text-slate-500">/actuator/health</p>
            </article>
          ))}
        </div>
      </SectionCard>
    </div>
  )
}
