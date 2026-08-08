import { useState } from 'react'
import SectionCard from '../components/SectionCard'
import { ErrorMessage, SuccessMessage } from '../components/Message'
import { EmptyState, ListSkeleton } from '../components/StateBlocks'
import { useAuth } from '../context/AuthContext'
import { api, getErrorMessage } from '../lib/api'

const profileDefault = {
  headline: '',
  summary: '',
  experienceYears: 0,
  currentLocation: '',
}

export default function ProfilePage() {
  const { user, role } = useAuth()
  const isAdmin = role === 'ADMIN'
  const [profile, setProfile] = useState(profileDefault)
  const [avatarFile, setAvatarFile] = useState(null)
  const [avatarVersion, setAvatarVersion] = useState(0)
  const [avatarLoadFailed, setAvatarLoadFailed] = useState(false)
  const [skills, setSkills] = useState([])
  const [skillName, setSkillName] = useState('')
  const [skillIdToRemove, setSkillIdToRemove] = useState('')
  const [catalogSkills, setCatalogSkills] = useState([])
  const [newCatalogSkill, setNewCatalogSkill] = useState('')
  const [allProfiles, setAllProfiles] = useState([])
  const [authLookupId, setAuthLookupId] = useState('')
  const [authLookupResult, setAuthLookupResult] = useState(null)
  const [passwordTargetId, setPasswordTargetId] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [deleteUserId, setDeleteUserId] = useState('')
  const [isLoadingSkills, setIsLoadingSkills] = useState(false)
  const [isLoadingCatalog, setIsLoadingCatalog] = useState(false)
  const [isLoadingProfiles, setIsLoadingProfiles] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function loadProfile() {
    if (!user?.id) return
    setError('')
    setIsLoadingSkills(true)
    try {
      const profileRes = await api.get(`/api/users/${user.id}/profile`)
      const skillsRes = await api.get(`/api/users/${user.id}/skills`)
      setProfile(profileRes.data)
      setSkills(skillsRes.data)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingSkills(false)
    }
  }

  async function saveProfile(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    if (!user?.id) return

    try {
      await api.post(`/api/users/${user.id}/profile`, profile)
      setSuccess('Profile saved')
    } catch (err) {
      if (err?.response?.status === 409) {
        await api.put(`/api/users/${user.id}/profile`, profile)
        setSuccess('Profile updated')
        return
      }
      setError(getErrorMessage(err))
    }
  }

  async function uploadAvatar(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    if (!user?.id || !avatarFile) return

    try {
      const formData = new FormData()
      formData.append('file', avatarFile)
      await api.post(`/api/users/${user.id}/avatar`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setSuccess('Avatar uploaded')
      setAvatarFile(null)
      setAvatarLoadFailed(false)
      setAvatarVersion((prev) => prev + 1)
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function addSkill(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    if (!user?.id || !skillName.trim()) return

    try {
      const response = await api.post(`/api/users/${user.id}/skills`, { skillName })
      setSkills((prev) => [...prev, response.data])
      setSkillName('')
      setSuccess('Skill added')
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function removeSkill(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    if (!user?.id || !skillIdToRemove) return

    try {
      await api.delete(`/api/users/${user.id}/skills/${skillIdToRemove}`)
      setSuccess('Skill removed')
      setSkillIdToRemove('')
      loadProfile()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function loadCatalogSkills() {
    setError('')
    setIsLoadingCatalog(true)
    try {
      const response = await api.get('/api/users/skills')
      setCatalogSkills(response.data)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingCatalog(false)
    }
  }

  async function createCatalogSkill(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    if (!newCatalogSkill.trim()) return

    try {
      await api.post('/api/users/skills', { name: newCatalogSkill })
      setSuccess('Skill created in catalog')
      setNewCatalogSkill('')
      loadCatalogSkills()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function loadAllProfiles() {
    setError('')
    setIsLoadingProfiles(true)
    try {
      const response = await api.get('/api/users')
      setAllProfiles(response.data)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoadingProfiles(false)
    }
  }

  async function lookupAuthUser(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    const targetId = isAdmin ? authLookupId : user?.id
    if (!targetId) {
      setError('User ID is required')
      return
    }
    try {
      const response = await api.get(`/api/auth/users/${targetId}`)
      setAuthLookupResult(response.data)
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function updatePassword(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    const targetId = isAdmin ? passwordTargetId : user?.id
    if (!targetId) {
      setError('Target user ID is required')
      return
    }
    try {
      await api.put(`/api/auth/users/${targetId}/password`, {
        currentPassword,
        newPassword,
      })
      setSuccess('Password updated')
      setCurrentPassword('')
      setNewPassword('')
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function deleteAuthUser(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      await api.delete(`/api/auth/users/${deleteUserId}`)
      setSuccess('User deleted')
      setDeleteUserId('')
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  return (
    <div className="space-y-6">
      <SectionCard
        title="Profile Module"
        subtitle="Create/update profile and manage user skills from user_service."
        right={
          <button
            type="button"
            onClick={loadProfile}
            className="rounded-xl bg-cyan-600 px-3 py-2 text-sm font-bold text-white hover:bg-cyan-700"
          >
            Load My Profile
          </button>
        }
      >
        <ErrorMessage text={error} />
        <SuccessMessage text={success} />

        <div className="mt-4 flex items-center gap-4">
          {user?.id && !avatarLoadFailed ? (
            <img
              key={avatarVersion}
              src={`${api.defaults.baseURL}/api/users/${user.id}/avatar?v=${avatarVersion}`}
              alt="Profile avatar"
              onError={() => setAvatarLoadFailed(true)}
              className="h-16 w-16 rounded-full border border-slate-200 object-cover"
            />
          ) : (
            <div className="flex h-16 w-16 items-center justify-center rounded-full border border-slate-200 bg-slate-100 text-xs text-slate-500">
              No avatar
            </div>
          )}
          <form className="flex flex-1 flex-col gap-2 sm:flex-row sm:items-center" onSubmit={uploadAvatar}>
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={(event) => setAvatarFile(event.target.files?.[0] || null)}
              className="flex-1 rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
            <button
              type="submit"
              disabled={!avatarFile}
              className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-bold text-white disabled:opacity-50"
            >
              Upload Avatar
            </button>
          </form>
        </div>

        <form className="mt-4 grid gap-3" onSubmit={saveProfile}>
          <input
            className="rounded-xl border border-slate-300 px-3 py-2"
            placeholder="Headline"
            value={profile.headline || ''}
            onChange={(event) => setProfile((prev) => ({ ...prev, headline: event.target.value }))}
          />
          <textarea
            className="rounded-xl border border-slate-300 px-3 py-2"
            placeholder="Summary"
            rows={4}
            value={profile.summary || ''}
            onChange={(event) => setProfile((prev) => ({ ...prev, summary: event.target.value }))}
          />
          <div className="grid gap-3 md:grid-cols-2">
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Years of experience"
              type="number"
              value={profile.experienceYears || 0}
              onChange={(event) =>
                setProfile((prev) => ({ ...prev, experienceYears: Number(event.target.value) }))
              }
            />
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              placeholder="Current location"
              value={profile.currentLocation || ''}
              onChange={(event) =>
                setProfile((prev) => ({ ...prev, currentLocation: event.target.value }))
              }
            />
          </div>
          <button
            type="submit"
            className="rounded-xl bg-slate-900 px-4 py-2 font-bold text-white hover:bg-slate-700"
          >
            Save Profile
          </button>
        </form>
      </SectionCard>

      <SectionCard title="Skills Module" subtitle="Bind skills to your profile using name-based API.">
        <form className="flex flex-col gap-3 sm:flex-row" onSubmit={addSkill}>
          <input
            className="flex-1 rounded-xl border border-slate-300 px-3 py-2"
            placeholder="Add a skill (e.g. Spring Boot)"
            value={skillName}
            onChange={(event) => setSkillName(event.target.value)}
          />
          <button
            type="submit"
            className="rounded-xl bg-emerald-600 px-4 py-2 font-bold text-white hover:bg-emerald-700"
          >
            Add Skill
          </button>
        </form>

        <form className="mt-3 flex flex-col gap-3 sm:flex-row" onSubmit={removeSkill}>
          <input
            className="flex-1 rounded-xl border border-slate-300 px-3 py-2"
            placeholder="Remove skill by ID"
            value={skillIdToRemove}
            onChange={(event) => setSkillIdToRemove(event.target.value)}
          />
          <button
            type="submit"
            className="rounded-xl bg-rose-500 px-4 py-2 font-bold text-white hover:bg-rose-600"
          >
            Remove Skill
          </button>
        </form>

        {isLoadingSkills ? <ListSkeleton rows={2} /> : null}

        {!isLoadingSkills && skills.length === 0 ? (
          <EmptyState title="No skills assigned" message="Add a skill or load profile data." />
        ) : null}

        {!isLoadingSkills && skills.length > 0 ? (
          <div className="mt-4 flex flex-wrap gap-2">
            {skills.map((skill) => (
              <span
                key={skill.id}
                className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-sm font-semibold text-emerald-700"
              >
                {skill.name}
              </span>
            ))}
          </div>
        ) : null}
      </SectionCard>

      <SectionCard
        title="Skill Catalog"
        subtitle="Mapped to GET/POST /api/users/skills for centralized skill taxonomy."
        right={
          <button
            type="button"
            onClick={loadCatalogSkills}
            className="rounded-xl bg-cyan-600 px-3 py-2 text-sm font-bold text-white hover:bg-cyan-700"
          >
            Load Catalog
          </button>
        }
      >
        <form className="mb-4 flex flex-col gap-3 sm:flex-row" onSubmit={createCatalogSkill}>
          <input
            className="flex-1 rounded-xl border border-slate-300 px-3 py-2"
            placeholder="New catalog skill"
            value={newCatalogSkill}
            onChange={(event) => setNewCatalogSkill(event.target.value)}
          />
          <button
            type="submit"
            className="rounded-xl bg-slate-900 px-4 py-2 font-bold text-white"
          >
            Create Skill
          </button>
        </form>
        {isLoadingCatalog ? <ListSkeleton rows={2} /> : null}

        {!isLoadingCatalog && catalogSkills.length === 0 ? (
          <EmptyState title="Skill catalog is empty" message="Create or load catalog entries." />
        ) : null}

        {!isLoadingCatalog && catalogSkills.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {catalogSkills.map((skill) => (
              <span
                key={skill.id}
                className="rounded-full border border-sky-200 bg-sky-50 px-3 py-1 text-sm font-semibold text-sky-700"
              >
                {skill.id}: {skill.name}
              </span>
            ))}
          </div>
        ) : null}
      </SectionCard>

      <SectionCard
        title="Profiles Directory"
        subtitle="Mapped to GET /api/users. Useful for admin and operational visibility."
        right={
          <button
            type="button"
            onClick={loadAllProfiles}
            className="rounded-xl bg-cyan-600 px-3 py-2 text-sm font-bold text-white hover:bg-cyan-700"
          >
            Load Profiles
          </button>
        }
      >
        {isLoadingProfiles ? <ListSkeleton rows={3} /> : null}

        {!isLoadingProfiles && allProfiles.length === 0 ? (
          <EmptyState title="No profiles loaded" message="Click Load Profiles to fetch directory data." />
        ) : null}

        {!isLoadingProfiles && allProfiles.length > 0 ? (
          <div className="grid gap-3 md:grid-cols-2">
            {allProfiles.map((item) => (
              <article key={item.id} className="rounded-xl border border-slate-200 p-3">
                <p className="font-bold text-slate-900">User #{item.userId}</p>
                <p className="text-sm text-slate-600">{item.headline || 'No headline'}</p>
                <p className="text-xs text-slate-500">{item.currentLocation || 'Unknown location'}</p>
              </article>
            ))}
          </div>
        ) : null}
      </SectionCard>

      <SectionCard title="Auth User Ops" subtitle="Mapped auth endpoints for user lookup, password update, and delete.">
        <div className="grid gap-6 lg:grid-cols-3">
          <form className="grid gap-3" onSubmit={lookupAuthUser}>
            <p className="text-sm font-semibold text-slate-700">GET /api/auth/users/{'{id}'}</p>
            {isAdmin ? (
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="User ID"
                value={authLookupId}
                onChange={(event) => setAuthLookupId(event.target.value)}
              />
            ) : (
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
                Lookup is scoped to your own user ID.
              </div>
            )}
            <button type="submit" className="rounded-xl bg-slate-900 px-4 py-2 font-bold text-white">
              Lookup
            </button>
            {authLookupResult ? (
              <p className="text-xs text-slate-600">
                {authLookupResult.id} • {authLookupResult.email} • {authLookupResult.role}
              </p>
            ) : null}
          </form>

          <form className="grid gap-3" onSubmit={updatePassword}>
            <p className="text-sm font-semibold text-slate-700">PUT /api/auth/users/{'{id}'}/password</p>
            {isAdmin ? (
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Target User ID"
                value={passwordTargetId}
                onChange={(event) => setPasswordTargetId(event.target.value)}
              />
            ) : (
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
                Password update is scoped to your own account.
              </div>
            )}
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              type="password"
              placeholder="Current password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              type="password"
              placeholder="New password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
            />
            <button type="submit" className="rounded-xl bg-amber-500 px-4 py-2 font-bold text-amber-950">
              Update Password
            </button>
          </form>

          {isAdmin ? (
            <form className="grid gap-3" onSubmit={deleteAuthUser}>
              <p className="text-sm font-semibold text-slate-700">DELETE /api/auth/users/{'{id}'}</p>
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                placeholder="User ID"
                value={deleteUserId}
                onChange={(event) => setDeleteUserId(event.target.value)}
              />
              <button type="submit" className="rounded-xl bg-rose-500 px-4 py-2 font-bold text-white">
                Delete User
              </button>
            </form>
          ) : (
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-600">
              Admin-only user deletion endpoint is available when signed in as ADMIN.
            </div>
          )}
        </div>
      </SectionCard>
    </div>
  )
}
