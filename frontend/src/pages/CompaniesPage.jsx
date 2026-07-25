import { useEffect, useState } from 'react'
import SectionCard from '../components/SectionCard'
import { ErrorMessage, SuccessMessage } from '../components/Message'
import { EmptyState, ListSkeleton } from '../components/StateBlocks'
import { useAuth } from '../context/AuthContext'
import { api, getErrorMessage } from '../lib/api'

const defaults = {
  name: '',
  description: '',
  website: '',
  location: '',
}

export default function CompaniesPage() {
  const { role } = useAuth()
  const [companies, setCompanies] = useState([])
  const [form, setForm] = useState(defaults)
  const [companyIdLookup, setCompanyIdLookup] = useState('')
  const [updateId, setUpdateId] = useState('')
  const [deleteId, setDeleteId] = useState('')
  const [recruiterCompanyId, setRecruiterCompanyId] = useState('')
  const [recruiterUserId, setRecruiterUserId] = useState('')
  const [recruiterDesignation, setRecruiterDesignation] = useState('')
  const [recruiters, setRecruiters] = useState([])
  const [removeRecruiterCompanyId, setRemoveRecruiterCompanyId] = useState('')
  const [removeRecruiterId, setRemoveRecruiterId] = useState('')
  const [isLoadingCompanies, setIsLoadingCompanies] = useState(true)
  const [isLoadingRecruiters, setIsLoadingRecruiters] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function loadCompanies() {
    setError('')
    setIsLoadingCompanies(true)
    try {
      const response = await api.get('/api/companies')
      setCompanies(response.data)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingCompanies(false)
    }
  }

  useEffect(() => {
    loadCompanies()
  }, [])

  async function createCompany(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    try {
      await api.post('/api/companies', form)
      setSuccess('Company created')
      setForm(defaults)
      loadCompanies()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function getCompanyById(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setIsLoadingCompanies(true)
    try {
      const response = await api.get(`/api/companies/${companyIdLookup}`)
      setCompanies(response.data ? [response.data] : [])
      setSuccess('Loaded company by ID')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingCompanies(false)
    }
  }

  async function updateCompany(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      await api.put(`/api/companies/${updateId}`, form)
      setSuccess('Company updated')
      loadCompanies()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function deleteCompany(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      await api.delete(`/api/companies/${deleteId}`)
      setSuccess('Company deleted')
      setDeleteId('')
      loadCompanies()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function addRecruiter(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      await api.post(`/api/companies/${recruiterCompanyId}/recruiters`, {
        userId: Number(recruiterUserId),
        designation: recruiterDesignation,
      })
      setSuccess('Recruiter added')
      setRecruiterUserId('')
      setRecruiterDesignation('')
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function loadRecruiters(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setIsLoadingRecruiters(true)
    try {
      const response = await api.get(`/api/companies/${recruiterCompanyId}/recruiters`)
      setRecruiters(response.data)
      setSuccess('Loaded recruiters')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingRecruiters(false)
    }
  }

  async function removeRecruiter(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      await api.delete(
        `/api/companies/${removeRecruiterCompanyId}/recruiters/${removeRecruiterId}`,
      )
      setSuccess('Recruiter removed')
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  const canCreate = role === 'RECRUITER' || role === 'ADMIN'

  return (
    <div className="space-y-6">
      <SectionCard title="Companies Module" subtitle="Public listing + recruiter/admin company creation.">
        <ErrorMessage text={error} />
        <SuccessMessage text={success} />

        {isLoadingCompanies ? <ListSkeleton rows={4} /> : null}

        {!isLoadingCompanies && companies.length === 0 ? (
          <EmptyState
            title="No companies found"
            message="Create a company record or load a specific company by ID."
          />
        ) : null}

        {!isLoadingCompanies && companies.length > 0 ? (
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            {companies.map((company) => (
              <article key={company.id} className="rounded-2xl border border-slate-200 p-4">
                <p className="text-lg font-black text-slate-900">{company.name}</p>
                <p className="mt-2 text-sm text-slate-700">{company.description}</p>
                <a className="mt-2 inline-block text-sm font-semibold text-cyan-700" href={company.website}>
                  {company.website}
                </a>
              </article>
            ))}
          </div>
        ) : null}

        <form className="mt-3 flex gap-2" onSubmit={getCompanyById}>
          <input
            className="flex-1 rounded-xl border border-slate-300 px-3 py-2"
            placeholder="Load company by ID"
            value={companyIdLookup}
            onChange={(event) => setCompanyIdLookup(event.target.value)}
          />
          <button type="submit" className="rounded-xl bg-slate-900 px-3 py-2 text-sm font-bold text-white">
            Load
          </button>
        </form>
      </SectionCard>

      {canCreate ? (
        <SectionCard title="Create Company" subtitle="Bound to POST /api/companies.">
          <form className="grid gap-3 md:grid-cols-2" onSubmit={createCompany}>
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Company name"
              required
              value={form.name}
              onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
            />
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Website"
              value={form.website}
              onChange={(event) => setForm((prev) => ({ ...prev, website: event.target.value }))}
            />
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Location"
              value={form.location}
              onChange={(event) => setForm((prev) => ({ ...prev, location: event.target.value }))}
            />
            <textarea
              className="rounded-xl border border-slate-300 px-3 py-2 md:col-span-2"
              placeholder="Description"
              rows={4}
              value={form.description}
              onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
            />
            <button
              type="submit"
              className="md:col-span-2 rounded-xl bg-slate-900 px-4 py-2 font-bold text-white"
            >
              Save Company
            </button>
          </form>

          <form className="mt-4 grid gap-3 md:grid-cols-2" onSubmit={updateCompany}>
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Update company ID"
              value={updateId}
              onChange={(event) => setUpdateId(event.target.value)}
            />
            <button type="submit" className="rounded-xl bg-amber-500 px-4 py-2 font-bold text-amber-950">
              Update Company
            </button>
          </form>

          {role === 'ADMIN' ? (
            <form className="mt-3 flex gap-2" onSubmit={deleteCompany}>
              <input
                className="flex-1 rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Delete company ID"
                value={deleteId}
                onChange={(event) => setDeleteId(event.target.value)}
              />
              <button type="submit" className="rounded-xl bg-rose-500 px-4 py-2 font-bold text-white">
                Delete
              </button>
            </form>
          ) : null}
        </SectionCard>
      ) : null}

      {canCreate ? (
        <SectionCard title="Recruiter Management" subtitle="Mapped recruiter sub-resource endpoints.">
          <div className="grid gap-4 lg:grid-cols-3">
            <form className="grid gap-3" onSubmit={addRecruiter}>
              <p className="text-sm font-semibold text-slate-700">POST /api/companies/{'{id}'}/recruiters</p>
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Company ID"
                value={recruiterCompanyId}
                onChange={(event) => setRecruiterCompanyId(event.target.value)}
              />
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="User ID"
                value={recruiterUserId}
                onChange={(event) => setRecruiterUserId(event.target.value)}
              />
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Designation"
                value={recruiterDesignation}
                onChange={(event) => setRecruiterDesignation(event.target.value)}
              />
              <button type="submit" className="rounded-xl bg-slate-900 px-4 py-2 font-bold text-white">
                Add Recruiter
              </button>
            </form>

            <form className="grid gap-3" onSubmit={loadRecruiters}>
              <p className="text-sm font-semibold text-slate-700">GET /api/companies/{'{id}'}/recruiters</p>
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Company ID"
                value={recruiterCompanyId}
                onChange={(event) => setRecruiterCompanyId(event.target.value)}
              />
              <button type="submit" className="rounded-xl bg-cyan-600 px-4 py-2 font-bold text-white">
                Load Recruiters
              </button>
              <div className="rounded-xl border border-slate-200 p-3 text-xs text-slate-600">
                {isLoadingRecruiters ? 'Loading recruiters...' : null}
                {!isLoadingRecruiters
                  ? recruiters.map((x) => `${x.id}: user ${x.userId}`).join(' | ') || 'No recruiters loaded'
                  : null}
              </div>
            </form>

            <form className="grid gap-3" onSubmit={removeRecruiter}>
              <p className="text-sm font-semibold text-slate-700">DELETE recruiter mapping</p>
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Company ID"
                value={removeRecruiterCompanyId}
                onChange={(event) => setRemoveRecruiterCompanyId(event.target.value)}
              />
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Recruiter ID"
                value={removeRecruiterId}
                onChange={(event) => setRemoveRecruiterId(event.target.value)}
              />
              <button type="submit" className="rounded-xl bg-rose-500 px-4 py-2 font-bold text-white">
                Remove Recruiter
              </button>
            </form>
          </div>
        </SectionCard>
      ) : null}
    </div>
  )
}
