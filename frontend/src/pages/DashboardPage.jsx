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

export default function DashboardPage() {
  const { user, role } = useAuth()

  return (
    <div className="space-y-6">
      <SectionCard
        title="Operations Overview"
        subtitle="Private control surface aligned to your Job Portal microservice architecture."
      >
        <div className="grid gap-4 md:grid-cols-3">
          <div className="rounded-2xl bg-slate-900 p-4 text-white">
            <p className="text-xs uppercase tracking-widest text-slate-300">Active Identity</p>
            <p className="mt-2 text-lg font-black">{user?.email}</p>
            <p className="text-sm text-slate-300">Role: {role}</p>
          </div>
          <div className="rounded-2xl bg-amber-100 p-4">
            <p className="text-xs uppercase tracking-widest text-amber-700">Gateway Endpoint</p>
            <p className="mt-2 text-sm font-bold text-amber-900">http://localhost:8080/api</p>
            <p className="text-xs text-amber-800">Centralized ingress for all service calls</p>
          </div>
          <div className="rounded-2xl bg-cyan-100 p-4">
            <p className="text-xs uppercase tracking-widest text-cyan-700">Managed Domains</p>
            <p className="mt-2 text-2xl font-black text-cyan-900">6</p>
            <p className="text-xs text-cyan-900/80">Auth, User, Job, Company, Application, Notification</p>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="Service Inventory" subtitle="Health-check map for local and staging verification.">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {endpoints.map((item) => (
            <article key={item.name} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
              <p className="font-bold text-slate-900">{item.name} Service</p>
              <p className="text-sm text-slate-600">http://localhost:{item.port}</p>
              <p className="text-xs text-slate-500">/actuator/health</p>
            </article>
          ))}
        </div>
      </SectionCard>
    </div>
  )
}
