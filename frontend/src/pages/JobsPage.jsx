import { useEffect, useState } from 'react'
import SectionCard from '../components/SectionCard'
import { ErrorMessage, SuccessMessage } from '../components/Message'
import { EmptyState, ListSkeleton } from '../components/StateBlocks'
import { useAuth } from '../context/AuthContext'
import { api, getErrorMessage } from '../lib/api'

const jobDefault = {
  title: '',
  description: '',
  companyId: '',
  location: '',
  jobType: 'FULL_TIME',
  minSalary: '',
  maxSalary: '',
  experienceRequired: '',
  skills: '',
}

function asJobList(payload) {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

export default function JobsPage() {
  const { role } = useAuth()
  const [jobs, setJobs] = useState([])
  const [filters, setFilters] = useState({
    keyword: '',
    location: '',
    jobType: '',
    minSalary: '',
    maxExperience: '',
  })
  const [form, setForm] = useState(jobDefault)
  const [updateJobId, setUpdateJobId] = useState('')
  const [jobIdLookup, setJobIdLookup] = useState('')
  const [companyIdLookup, setCompanyIdLookup] = useState('')
  const [statusJobId, setStatusJobId] = useState('')
  const [statusValue, setStatusValue] = useState('OPEN')
  const [deleteJobId, setDeleteJobId] = useState('')
  const [isLoadingJobs, setIsLoadingJobs] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function loadJobs() {
    setError('')
    setIsLoadingJobs(true)
    try {
      const response = await api.get('/api/jobs')
      setJobs(asJobList(response.data))
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingJobs(false)
    }
  }

  useEffect(() => {
    loadJobs()
  }, [])

  async function searchJobs(event) {
    event.preventDefault()
    setError('')
    setIsLoadingJobs(true)
    try {
      const response = await api.get('/api/jobs/search', {
        params: {
          keyword: filters.keyword || undefined,
          location: filters.location || undefined,
          jobType: filters.jobType || undefined,
          minSalary: filters.minSalary ? Number(filters.minSalary) : undefined,
          maxExperience: filters.maxExperience ? Number(filters.maxExperience) : undefined,
        },
      })
      setJobs(asJobList(response.data))
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingJobs(false)
    }
  }

  async function updateJob(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    try {
      await api.put(`/api/jobs/${updateJobId}`, {
        ...form,
        companyId: Number(form.companyId),
        minSalary: form.minSalary ? Number(form.minSalary) : 0,
        maxSalary: form.maxSalary ? Number(form.maxSalary) : 0,
        experienceRequired: form.experienceRequired ? Number(form.experienceRequired) : 0,
        skills: form.skills
          .split(',')
          .map((x) => x.trim())
          .filter(Boolean),
      })
      setSuccess('Job updated successfully')
      setUpdateJobId('')
      loadJobs()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function createJob(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    try {
      await api.post('/api/jobs', {
        ...form,
        companyId: Number(form.companyId),
        minSalary: form.minSalary ? Number(form.minSalary) : undefined,
        maxSalary: form.maxSalary ? Number(form.maxSalary) : undefined,
        experienceRequired: form.experienceRequired ? Number(form.experienceRequired) : 0,
        skills: form.skills
          .split(',')
          .map((x) => x.trim())
          .filter(Boolean),
      })
      setSuccess('Job posted successfully')
      setForm(jobDefault)
      loadJobs()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function getJobById(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setIsLoadingJobs(true)
    try {
      const response = await api.get(`/api/jobs/${jobIdLookup}`)
      setJobs(response.data ? [response.data] : [])
      setSuccess('Loaded job by ID')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingJobs(false)
    }
  }

  async function getJobsByCompany(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setIsLoadingJobs(true)
    try {
      const response = await api.get(`/api/jobs/company/${companyIdLookup}`)
      setJobs(asJobList(response.data))
      setSuccess('Loaded jobs by company')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingJobs(false)
    }
  }

  async function updateJobStatus(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      await api.patch(`/api/jobs/${statusJobId}/status`, null, { params: { status: statusValue } })
      setSuccess('Job status updated')
      loadJobs()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function deleteJob(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      await api.delete(`/api/jobs/${deleteJobId}`)
      setSuccess('Job deleted')
      setDeleteJobId('')
      loadJobs()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  const canCreate = role === 'RECRUITER' || role === 'ADMIN'

  return (
    <div className="space-y-6">
      <SectionCard title="Jobs Module" subtitle="Browse and search live jobs from job_service.">
        <ErrorMessage text={error} />
        <SuccessMessage text={success} />

        <form className="mt-4 grid gap-3 md:grid-cols-6" onSubmit={searchJobs}>
          <input
            className="rounded-xl border border-slate-300 px-3 py-2"
            placeholder="Keyword"
            value={filters.keyword}
            onChange={(event) => setFilters((prev) => ({ ...prev, keyword: event.target.value }))}
          />
          <input
            className="rounded-xl border border-slate-300 px-3 py-2"
            placeholder="Location"
            value={filters.location}
            onChange={(event) => setFilters((prev) => ({ ...prev, location: event.target.value }))}
          />
          <select
            className="rounded-xl border border-slate-300 px-3 py-2"
            value={filters.jobType}
            onChange={(event) => setFilters((prev) => ({ ...prev, jobType: event.target.value }))}
          >
            <option value="">Any Type</option>
            <option value="FULL_TIME">FULL_TIME</option>
            <option value="PART_TIME">PART_TIME</option>
            <option value="CONTRACT">CONTRACT</option>
          </select>
          <input
            className="rounded-xl border border-slate-300 px-3 py-2"
            placeholder="Min salary"
            value={filters.minSalary}
            onChange={(event) => setFilters((prev) => ({ ...prev, minSalary: event.target.value }))}
          />
          <input
            className="rounded-xl border border-slate-300 px-3 py-2"
            placeholder="Max experience"
            value={filters.maxExperience}
            onChange={(event) =>
              setFilters((prev) => ({ ...prev, maxExperience: event.target.value }))
            }
          />
          <button
            type="submit"
            className="rounded-xl bg-cyan-600 px-4 py-2 font-bold text-white hover:bg-cyan-700"
          >
            Search
          </button>
        </form>

        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <form className="flex gap-2" onSubmit={getJobById}>
            <input
              className="flex-1 rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Get job by ID"
              value={jobIdLookup}
              onChange={(event) => setJobIdLookup(event.target.value)}
            />
            <button type="submit" className="rounded-xl bg-slate-900 px-3 py-2 text-sm font-bold text-white">
              Get
            </button>
          </form>
          <form className="flex gap-2" onSubmit={getJobsByCompany}>
            <input
              className="flex-1 rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Get jobs by company ID"
              value={companyIdLookup}
              onChange={(event) => setCompanyIdLookup(event.target.value)}
            />
            <button type="submit" className="rounded-xl bg-slate-900 px-3 py-2 text-sm font-bold text-white">
              Load
            </button>
          </form>
        </div>

        {isLoadingJobs ? <ListSkeleton rows={4} /> : null}

        {!isLoadingJobs && jobs.length === 0 ? (
          <EmptyState
            title="No jobs to display"
            message="Try broader search filters or create a new job posting."
          />
        ) : null}

        {!isLoadingJobs && jobs.length > 0 ? (
          <div className="mt-4 grid gap-3">
            {jobs.map((job) => (
              <article key={job.id} className="rounded-2xl border border-slate-200 p-4">
                <p className="text-lg font-black text-slate-900">{job.title}</p>
                <p className="text-sm text-slate-600">{job.location} • {job.jobType}</p>
                <p className="mt-2 text-sm text-slate-700">{job.description}</p>
                <p className="mt-2 text-xs uppercase tracking-wider text-slate-500">
                  Status: {job.status}
                </p>
              </article>
            ))}
          </div>
        ) : null}
      </SectionCard>

      {canCreate ? (
        <SectionCard title="Post Job" subtitle="Recruiter/Admin action: create new listings.">
          <form className="grid gap-3 md:grid-cols-2" onSubmit={createJob}>
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Title"
              required
              value={form.title}
              onChange={(event) => setForm((prev) => ({ ...prev, title: event.target.value }))}
            />
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Company ID"
              required
              value={form.companyId}
              onChange={(event) => setForm((prev) => ({ ...prev, companyId: event.target.value }))}
            />
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Location"
              required
              value={form.location}
              onChange={(event) => setForm((prev) => ({ ...prev, location: event.target.value }))}
            />
            <select
              className="rounded-xl border border-slate-300 px-3 py-2"
              value={form.jobType}
              onChange={(event) => setForm((prev) => ({ ...prev, jobType: event.target.value }))}
            >
              <option value="FULL_TIME">FULL_TIME</option>
              <option value="PART_TIME">PART_TIME</option>
              <option value="CONTRACT">CONTRACT</option>
            </select>
            <textarea
              className="md:col-span-2 rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Description"
              rows={4}
              required
              value={form.description}
              onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
            />
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Min salary"
              value={form.minSalary}
              onChange={(event) => setForm((prev) => ({ ...prev, minSalary: event.target.value }))}
            />
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Max salary"
              value={form.maxSalary}
              onChange={(event) => setForm((prev) => ({ ...prev, maxSalary: event.target.value }))}
            />
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Experience required"
              value={form.experienceRequired}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, experienceRequired: event.target.value }))
              }
            />
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Skills (comma-separated)"
              value={form.skills}
              onChange={(event) => setForm((prev) => ({ ...prev, skills: event.target.value }))}
            />
            <button
              type="submit"
              className="md:col-span-2 rounded-xl bg-slate-900 px-4 py-2 font-bold text-white"
            >
              Publish Job
            </button>
          </form>

          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <form className="grid gap-3 rounded-2xl border border-slate-200 p-3" onSubmit={updateJob}>
              <p className="text-sm font-semibold text-slate-700">PUT /api/jobs/{'{id}'}</p>
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Job ID to update"
                value={updateJobId}
                onChange={(event) => setUpdateJobId(event.target.value)}
              />
              <button type="submit" className="rounded-xl bg-slate-900 px-4 py-2 font-bold text-white">
                Update Job Using Form Data
              </button>
            </form>

            <form className="grid gap-3 rounded-2xl border border-slate-200 p-3" onSubmit={updateJobStatus}>
              <p className="text-sm font-semibold text-slate-700">PATCH /api/jobs/{'{id}'}/status</p>
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Job ID"
                value={statusJobId}
                onChange={(event) => setStatusJobId(event.target.value)}
              />
              <select
                className="rounded-xl border border-slate-300 px-3 py-2"
                value={statusValue}
                onChange={(event) => setStatusValue(event.target.value)}
              >
                <option value="OPEN">OPEN</option>
                <option value="CLOSED">CLOSED</option>
                <option value="DRAFT">DRAFT</option>
              </select>
              <button type="submit" className="rounded-xl bg-amber-500 px-4 py-2 font-bold text-amber-950">
                Update Status
              </button>
            </form>

            <form className="grid gap-3 rounded-2xl border border-slate-200 p-3 md:col-span-2" onSubmit={deleteJob}>
              <p className="text-sm font-semibold text-slate-700">DELETE /api/jobs/{'{id}'}</p>
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Job ID"
                value={deleteJobId}
                onChange={(event) => setDeleteJobId(event.target.value)}
              />
              <button type="submit" className="rounded-xl bg-rose-500 px-4 py-2 font-bold text-white">
                Delete Job
              </button>
            </form>
          </div>
        </SectionCard>
      ) : null}
    </div>
  )
}
