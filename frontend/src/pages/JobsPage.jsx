import { useEffect, useState } from 'react'
import SectionCard from '../components/SectionCard'
import { ErrorMessage, SuccessMessage } from '../components/Message'
import { EmptyState, ListSkeleton } from '../components/StateBlocks'
import { useConfirm } from '../components/ConfirmDialogProvider'
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

const PAGE_SIZE = 10

function asJobList(payload) {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

function asPageMeta(payload) {
  if (!payload || Array.isArray(payload) || !Array.isArray(payload.content)) return null
  return {
    number: payload.number ?? 0,
    totalPages: payload.totalPages ?? 1,
    totalElements: payload.totalElements ?? payload.content.length,
    first: payload.first ?? payload.number === 0,
    last: payload.last ?? payload.number >= (payload.totalPages ?? 1) - 1,
  }
}

export default function JobsPage() {
  const { role } = useAuth()
  const confirm = useConfirm()
  const isJobSeeker = role === 'JOB_SEEKER'
  const [jobs, setJobs] = useState([])
  const [pageMeta, setPageMeta] = useState(null)
  const [activeView, setActiveView] = useState('all')
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
  const [savedJobIds, setSavedJobIds] = useState(new Set())
  const [showSavedOnly, setShowSavedOnly] = useState(false)

  async function loadSavedJobIds() {
    if (!isJobSeeker) return
    try {
      const response = await api.get('/api/jobs/saved/ids')
      setSavedJobIds(new Set(response.data || []))
    } catch {
      // Non-critical: bookmark state just won't be pre-filled.
    }
  }

  async function loadJobs(targetPage = 0) {
    setError('')
    setIsLoadingJobs(true)
    try {
      const response = await api.get('/api/jobs', { params: { page: targetPage, size: PAGE_SIZE } })
      setJobs(asJobList(response.data))
      setPageMeta(asPageMeta(response.data))
      setShowSavedOnly(false)
      setActiveView('all')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingJobs(false)
    }
  }

  async function loadSavedJobs() {
    setError('')
    setIsLoadingJobs(true)
    try {
      const response = await api.get('/api/jobs/saved')
      setJobs(asJobList(response.data))
      setPageMeta(null)
      setShowSavedOnly(true)
      setActiveView('saved')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingJobs(false)
    }
  }

  async function toggleSaveJob(jobId) {
    setError('')
    const alreadySaved = savedJobIds.has(jobId)
    try {
      if (alreadySaved) {
        await api.delete(`/api/jobs/${jobId}/save`)
        setSavedJobIds((prev) => {
          const next = new Set(prev)
          next.delete(jobId)
          return next
        })
        if (showSavedOnly) setJobs((prev) => prev.filter((job) => job.id !== jobId))
      } else {
        await api.post(`/api/jobs/${jobId}/save`)
        setSavedJobIds((prev) => new Set(prev).add(jobId))
      }
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  useEffect(() => {
    loadJobs()
    loadSavedJobIds()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function fetchSearchResults(targetPage = 0) {
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
          page: targetPage,
          size: PAGE_SIZE,
        },
      })
      setJobs(asJobList(response.data))
      setPageMeta(asPageMeta(response.data))
      setShowSavedOnly(false)
      setActiveView('search')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingJobs(false)
    }
  }

  function searchJobs(event) {
    event.preventDefault()
    fetchSearchResults(0)
  }

  function goToPage(targetPage) {
    if (activeView === 'search') {
      fetchSearchResults(targetPage)
    } else {
      loadJobs(targetPage)
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
      setPageMeta(null)
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
      setPageMeta(null)
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
    const confirmed = await confirm({
      title: `Delete job #${deleteJobId}?`,
      message: 'This permanently removes the job listing. This cannot be undone.',
      confirmLabel: 'Delete job',
    })
    if (!confirmed) return
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
            className="input"
            placeholder="Keyword"
            value={filters.keyword}
            onChange={(event) => setFilters((prev) => ({ ...prev, keyword: event.target.value }))}
          />
          <input
            className="input"
            placeholder="Location"
            value={filters.location}
            onChange={(event) => setFilters((prev) => ({ ...prev, location: event.target.value }))}
          />
          <select
            className="input"
            value={filters.jobType}
            onChange={(event) => setFilters((prev) => ({ ...prev, jobType: event.target.value }))}
          >
            <option value="">Any Type</option>
            <option value="FULL_TIME">FULL_TIME</option>
            <option value="PART_TIME">PART_TIME</option>
            <option value="CONTRACT">CONTRACT</option>
          </select>
          <input
            className="input"
            placeholder="Min salary"
            value={filters.minSalary}
            onChange={(event) => setFilters((prev) => ({ ...prev, minSalary: event.target.value }))}
          />
          <input
            className="input"
            placeholder="Max experience"
            value={filters.maxExperience}
            onChange={(event) =>
              setFilters((prev) => ({ ...prev, maxExperience: event.target.value }))
            }
          />
          <button
            type="submit"
            className="btn btn-accent"
          >
            Search
          </button>
        </form>

        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <form className="flex gap-2" onSubmit={getJobById}>
            <input
              className="input flex-1"
              placeholder="Get job by ID"
              value={jobIdLookup}
              onChange={(event) => setJobIdLookup(event.target.value)}
            />
            <button type="submit" className="btn btn-dark btn-sm">
              Get
            </button>
          </form>
          <form className="flex gap-2" onSubmit={getJobsByCompany}>
            <input
              className="input flex-1"
              placeholder="Get jobs by company ID"
              value={companyIdLookup}
              onChange={(event) => setCompanyIdLookup(event.target.value)}
            />
            <button type="submit" className="btn btn-dark btn-sm">
              Load
            </button>
          </form>
        </div>

        {isJobSeeker ? (
          <div className="mt-3 flex gap-2">
            <button
              type="button"
              onClick={() => loadJobs(0)}
              className={`btn btn-sm ${!showSavedOnly ? 'btn-dark' : 'btn-secondary'}`}
            >
              All Jobs
            </button>
            <button
              type="button"
              onClick={loadSavedJobs}
              className={`btn btn-sm ${showSavedOnly ? 'btn-dark' : 'btn-secondary'}`}
            >
              Saved Jobs
            </button>
          </div>
        ) : null}

        {isLoadingJobs ? <ListSkeleton rows={4} /> : null}

        {!isLoadingJobs && jobs.length === 0 ? (
          <EmptyState
            title="No jobs to display"
            message={
              showSavedOnly
                ? 'You haven’t saved any jobs yet.'
                : 'Try broader search filters or create a new job posting.'
            }
          />
        ) : null}

        {!isLoadingJobs && jobs.length > 0 ? (
          <div className="mt-4 grid gap-3">
            {jobs.map((job) => (
              <article key={job.id} className="item-card">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-lg font-black text-slate-900">{job.title}</p>
                    {job.source && job.source !== 'RECRUITER' ? (
                      <span className="badge badge-info">{job.source}</span>
                    ) : null}
                  </div>
                  {isJobSeeker ? (
                    <button
                      type="button"
                      onClick={() => toggleSaveJob(job.id)}
                      aria-label={savedJobIds.has(job.id) ? 'Unsave job' : 'Save job'}
                      className={`btn btn-sm shrink-0 ${
                        savedJobIds.has(job.id) ? 'btn-warning' : 'btn-secondary'
                      }`}
                    >
                      {savedJobIds.has(job.id) ? '★ Saved' : '☆ Save'}
                    </button>
                  ) : null}
                </div>
                {job.companyName ? (
                  <p className="mt-1 text-sm font-semibold text-slate-700">{job.companyName}</p>
                ) : null}
                <p className="text-sm text-slate-600">{job.location} • {job.jobType}</p>
                <p className="mt-2 text-sm text-slate-700">{job.description}</p>
                <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
                  <p className="text-xs uppercase tracking-wider text-slate-500">
                    Status: {job.status}
                  </p>
                  {job.externalUrl ? (
                    <a
                      href={job.externalUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-xs font-semibold text-indigo-600 hover:underline"
                    >
                      View original listing →
                    </a>
                  ) : null}
                </div>
              </article>
            ))}
          </div>
        ) : null}

        {!isLoadingJobs && pageMeta && pageMeta.totalPages > 1 ? (
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
            <p className="text-xs text-slate-500">
              Page {pageMeta.number + 1} of {pageMeta.totalPages} &middot; {pageMeta.totalElements} job
              {pageMeta.totalElements === 1 ? '' : 's'} total
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={pageMeta.first}
                onClick={() => goToPage(pageMeta.number - 1)}
                className="btn btn-secondary btn-sm"
              >
                &larr; Previous
              </button>
              <button
                type="button"
                disabled={pageMeta.last}
                onClick={() => goToPage(pageMeta.number + 1)}
                className="btn btn-secondary btn-sm"
              >
                Next &rarr;
              </button>
            </div>
          </div>
        ) : null}
      </SectionCard>

      {canCreate ? (
        <SectionCard title="Post Job" subtitle="Recruiter/Admin action: create new listings.">
          <form className="grid gap-3 md:grid-cols-2" onSubmit={createJob}>
            <input
              className="input"
              placeholder="Title"
              required
              value={form.title}
              onChange={(event) => setForm((prev) => ({ ...prev, title: event.target.value }))}
            />
            <input
              className="input"
              placeholder="Company ID"
              required
              value={form.companyId}
              onChange={(event) => setForm((prev) => ({ ...prev, companyId: event.target.value }))}
            />
            <input
              className="input"
              placeholder="Location"
              required
              value={form.location}
              onChange={(event) => setForm((prev) => ({ ...prev, location: event.target.value }))}
            />
            <select
              className="input"
              value={form.jobType}
              onChange={(event) => setForm((prev) => ({ ...prev, jobType: event.target.value }))}
            >
              <option value="FULL_TIME">FULL_TIME</option>
              <option value="PART_TIME">PART_TIME</option>
              <option value="CONTRACT">CONTRACT</option>
            </select>
            <textarea
              className="input md:col-span-2"
              placeholder="Description"
              rows={4}
              required
              value={form.description}
              onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
            />
            <input
              className="input"
              placeholder="Min salary"
              value={form.minSalary}
              onChange={(event) => setForm((prev) => ({ ...prev, minSalary: event.target.value }))}
            />
            <input
              className="input"
              placeholder="Max salary"
              value={form.maxSalary}
              onChange={(event) => setForm((prev) => ({ ...prev, maxSalary: event.target.value }))}
            />
            <input
              className="input"
              placeholder="Experience required"
              value={form.experienceRequired}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, experienceRequired: event.target.value }))
              }
            />
            <input
              className="input"
              placeholder="Skills (comma-separated)"
              value={form.skills}
              onChange={(event) => setForm((prev) => ({ ...prev, skills: event.target.value }))}
            />
            <button
              type="submit"
              className="btn btn-dark md:col-span-2"
            >
              Publish Job
            </button>
          </form>

          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <form className="item-card grid gap-3" onSubmit={updateJob}>
              <p className="text-sm font-semibold text-slate-700">PUT /api/jobs/{'{id}'}</p>
              <input
                className="input"
                placeholder="Job ID to update"
                value={updateJobId}
                onChange={(event) => setUpdateJobId(event.target.value)}
              />
              <button type="submit" className="btn btn-dark">
                Update Job Using Form Data
              </button>
            </form>

            <form className="item-card grid gap-3" onSubmit={updateJobStatus}>
              <p className="text-sm font-semibold text-slate-700">PATCH /api/jobs/{'{id}'}/status</p>
              <input
                className="input"
                placeholder="Job ID"
                value={statusJobId}
                onChange={(event) => setStatusJobId(event.target.value)}
              />
              <select
                className="input"
                value={statusValue}
                onChange={(event) => setStatusValue(event.target.value)}
              >
                <option value="OPEN">OPEN</option>
                <option value="CLOSED">CLOSED</option>
                <option value="DRAFT">DRAFT</option>
              </select>
              <button type="submit" className="btn btn-warning">
                Update Status
              </button>
            </form>

            <form className="item-card grid gap-3 md:col-span-2" onSubmit={deleteJob}>
              <p className="text-sm font-semibold text-slate-700">DELETE /api/jobs/{'{id}'}</p>
              <input
                className="input"
                placeholder="Job ID"
                value={deleteJobId}
                onChange={(event) => setDeleteJobId(event.target.value)}
              />
              <button type="submit" className="btn btn-danger">
                Delete Job
              </button>
            </form>
          </div>
        </SectionCard>
      ) : null}
    </div>
  )
}
