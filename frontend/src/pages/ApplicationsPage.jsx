import { useState } from 'react'
import SectionCard from '../components/SectionCard'
import { ErrorMessage, SuccessMessage } from '../components/Message'
import { EmptyState, ListSkeleton } from '../components/StateBlocks'
import { useAuth } from '../context/AuthContext'
import { api, getErrorMessage } from '../lib/api'

export default function ApplicationsPage() {
  const { role } = useAuth()
  const [myApplications, setMyApplications] = useState([])
  const [jobId, setJobId] = useState('')
  const [updateId, setUpdateId] = useState('')
  const [status, setStatus] = useState('SHORTLISTED')
  const [applicationIdLookup, setApplicationIdLookup] = useState('')
  const [applicationsByJobId, setApplicationsByJobId] = useState('')
  const [withdrawId, setWithdrawId] = useState('')
  const [isLoadingApplications, setIsLoadingApplications] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const isJobSeeker = role === 'JOB_SEEKER'
  const canUpdate = role === 'RECRUITER' || role === 'ADMIN'

  async function loadMine() {
    setError('')
    setIsLoadingApplications(true)
    try {
      const response = await api.get('/api/applications/my')
      setMyApplications(response.data)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingApplications(false)
    }
  }

  async function applyToJob(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      await api.post('/api/applications', { jobId: Number(jobId) })
      setSuccess('Application submitted')
      setJobId('')
      loadMine()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function updateStatus(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      await api.patch(`/api/applications/${updateId}/status`, null, {
        params: { status },
      })
      setSuccess('Application status updated')
      setUpdateId('')
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function getApplicationById(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setIsLoadingApplications(true)
    try {
      const response = await api.get(`/api/applications/${applicationIdLookup}`)
      setMyApplications(response.data ? [response.data] : [])
      setSuccess('Loaded application by ID')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingApplications(false)
    }
  }

  async function getApplicationsByJob(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setIsLoadingApplications(true)
    try {
      const response = await api.get('/api/applications', {
        params: { jobId: Number(applicationsByJobId) },
      })
      setMyApplications(response.data)
      setSuccess('Loaded applications by job')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingApplications(false)
    }
  }

  async function withdrawApplication(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      await api.patch(`/api/applications/${withdrawId}/withdraw`)
      setSuccess('Application withdrawn')
      setWithdrawId('')
      loadMine()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  return (
    <div className="space-y-6">
      <SectionCard
        title="Applications Module"
        subtitle="Submit applications as JOB_SEEKER or update statuses as RECRUITER/ADMIN."
        right={
          isJobSeeker ? (
          <button
            type="button"
            onClick={loadMine}
            className="rounded-xl bg-cyan-600 px-3 py-2 text-sm font-bold text-white hover:bg-cyan-700"
          >
            Load My Applications
          </button>
          ) : null
        }
      >
        <ErrorMessage text={error} />
        <SuccessMessage text={success} />

        {isLoadingApplications ? <ListSkeleton rows={3} /> : null}

        {!isLoadingApplications && myApplications.length === 0 ? (
          <EmptyState
            title="No applications loaded"
            message="Use one of the load actions to fetch applications."
          />
        ) : null}

        {!isLoadingApplications && myApplications.length > 0 ? (
          <div className="mt-4 grid gap-3">
            {myApplications.map((app) => (
              <article key={app.id} className="rounded-2xl border border-slate-200 p-3">
                <p className="font-bold text-slate-900">
                  Application #{app.id} • Job #{app.jobId}
                </p>
                <p className="text-sm text-slate-600">Status: {app.status}</p>
              </article>
            ))}
          </div>
        ) : null}

        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <form className="flex gap-2" onSubmit={getApplicationById}>
            <input
              className="flex-1 rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Get application by ID"
              value={applicationIdLookup}
              onChange={(event) => setApplicationIdLookup(event.target.value)}
            />
            <button type="submit" className="rounded-xl bg-slate-900 px-3 py-2 text-sm font-bold text-white">
              Load
            </button>
          </form>

          {canUpdate ? (
            <form className="flex gap-2" onSubmit={getApplicationsByJob}>
              <input
                className="flex-1 rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Get applications by Job ID"
                value={applicationsByJobId}
                onChange={(event) => setApplicationsByJobId(event.target.value)}
              />
              <button type="submit" className="rounded-xl bg-slate-900 px-3 py-2 text-sm font-bold text-white">
                Load
              </button>
            </form>
          ) : null}
        </div>
      </SectionCard>

      {isJobSeeker ? (
        <SectionCard title="Apply for Job" subtitle="POST /api/applications">
          <form className="grid gap-3" onSubmit={applyToJob}>
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Job ID"
              required
              value={jobId}
              onChange={(event) => setJobId(event.target.value)}
            />
            <button
              type="submit"
              className="rounded-xl bg-slate-900 px-4 py-2 font-bold text-white"
            >
              Submit Application
            </button>
          </form>

          <form className="mt-3 flex gap-2" onSubmit={withdrawApplication}>
            <input
              className="flex-1 rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Withdraw application ID"
              value={withdrawId}
              onChange={(event) => setWithdrawId(event.target.value)}
            />
            <button type="submit" className="rounded-xl bg-rose-500 px-4 py-2 font-bold text-white">
              Withdraw
            </button>
          </form>
        </SectionCard>
      ) : null}

      {canUpdate ? (
        <SectionCard title="Update Status" subtitle="PATCH /api/applications/{id}/status">
          <form className="grid gap-3 md:grid-cols-3" onSubmit={updateStatus}>
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Application ID"
              required
              value={updateId}
              onChange={(event) => setUpdateId(event.target.value)}
            />
            <select
              className="rounded-xl border border-slate-300 px-3 py-2"
              value={status}
              onChange={(event) => setStatus(event.target.value)}
            >
              <option value="SHORTLISTED">SHORTLISTED</option>
              <option value="REJECTED">REJECTED</option>
              <option value="HIRED">HIRED</option>
            </select>
            <button
              type="submit"
              className="rounded-xl bg-amber-500 px-4 py-2 font-bold text-amber-950"
            >
              Update
            </button>
          </form>
        </SectionCard>
      ) : null}
    </div>
  )
}
