import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import SectionCard from '../components/SectionCard'
import { ErrorMessage, SuccessMessage } from '../components/Message'
import { useToast } from '../components/ToastProvider'
import { useAuth } from '../context/AuthContext'
import { useRequestState } from '../hooks/useRequestState'
import { getErrorMessage } from '../lib/api'
import { loginSchema, registerSchema, validateForm } from '../lib/validation'

const registerDefaults = {
  email: '',
  password: '',
  role: 'JOB_SEEKER',
}

const loginTracks = [
  { key: 'candidate', label: 'Candidate', expectedRole: 'JOB_SEEKER' },
  { key: 'employer', label: 'Employer', expectedRole: 'RECRUITER' },
  { key: 'admin', label: 'Admin', expectedRole: 'ADMIN' },
]

function resolveDestination(role) {
  if (role === 'JOB_SEEKER') return '/applications'
  if (role === 'RECRUITER') return '/jobs'
  return '/'
}

function ModeToggle({ mode, allowSelfRegister, onSelectMode }) {
  return (
    <div className="mb-4 flex gap-2">
      <button
        type="button"
        onClick={() => onSelectMode('login')}
        className={`rounded-full px-4 py-2 text-sm font-bold ${
          mode === 'login' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600'
        }`}
      >
        Login
      </button>
      {allowSelfRegister ? (
        <button
          type="button"
          onClick={() => onSelectMode('register')}
          className={`rounded-full px-4 py-2 text-sm font-bold ${
            mode === 'register' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600'
          }`}
        >
          Register
        </button>
      ) : null}
    </div>
  )
}

function LoginForm({
  loading,
  loginTrack,
  loginData,
  selectedTrack,
  fieldErrors,
  onChangeTrack,
  onChangeEmail,
  onChangePassword,
  onSubmit,
  onRefreshToken,
}) {
  return (
    <form className="mt-4 grid gap-3" onSubmit={onSubmit}>
      <div className="grid gap-2 sm:grid-cols-3">
        {loginTracks.map((track) => (
          <button
            key={track.key}
            type="button"
            onClick={() => onChangeTrack(track.key)}
            className={`rounded-xl border px-3 py-2 text-sm font-semibold ${
              loginTrack === track.key
                ? 'border-slate-900 bg-slate-900 text-white'
                : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
            }`}
          >
            {track.label} Login
          </button>
        ))}
      </div>

      <p className="text-xs text-slate-500">
        Employer accounts map to RECRUITER role, Candidate accounts map to JOB_SEEKER role.
      </p>

      <input
        className="rounded-xl border border-slate-300 px-3 py-2"
        placeholder="Email"
        type="email"
        required
        value={loginData.email}
        onChange={(event) => onChangeEmail(event.target.value)}
      />
      {fieldErrors.email ? <p className="text-xs font-semibold text-rose-700">{fieldErrors.email}</p> : null}
      <input
        className="rounded-xl border border-slate-300 px-3 py-2"
        placeholder="Password"
        type="password"
        required
        value={loginData.password}
        onChange={(event) => onChangePassword(event.target.value)}
      />
      {fieldErrors.password ? <p className="text-xs font-semibold text-rose-700">{fieldErrors.password}</p> : null}
      <button
        type="submit"
        disabled={loading}
        className="rounded-xl bg-slate-900 px-4 py-2 font-bold text-white disabled:opacity-60"
      >
        {loading ? 'Signing in...' : `Sign in as ${selectedTrack.label}`}
      </button>
      <button
        type="button"
        onClick={onRefreshToken}
        className="rounded-xl border border-slate-300 px-4 py-2 font-semibold text-slate-700 hover:bg-slate-50"
      >
        Refresh Session Token
      </button>
    </form>
  )
}

function RegisterForm({ loading, registerData, fieldErrors, onChangeRegister, onSubmit }) {
  return (
    <form className="mt-4 grid gap-3" onSubmit={onSubmit}>
      <input
        className="rounded-xl border border-slate-300 px-3 py-2"
        placeholder="Email"
        type="email"
        required
        value={registerData.email}
        onChange={(event) => onChangeRegister({ email: event.target.value })}
      />
      {fieldErrors.email ? <p className="text-xs font-semibold text-rose-700">{fieldErrors.email}</p> : null}
      <input
        className="rounded-xl border border-slate-300 px-3 py-2"
        placeholder="Password"
        type="password"
        required
        value={registerData.password}
        onChange={(event) => onChangeRegister({ password: event.target.value })}
      />
      {fieldErrors.password ? <p className="text-xs font-semibold text-rose-700">{fieldErrors.password}</p> : null}
      <select
        className="rounded-xl border border-slate-300 px-3 py-2"
        value={registerData.role}
        onChange={(event) => onChangeRegister({ role: event.target.value })}
      >
        <option value="JOB_SEEKER">JOB_SEEKER</option>
        <option value="RECRUITER">RECRUITER</option>
      </select>
      {fieldErrors.role ? <p className="text-xs font-semibold text-rose-700">{fieldErrors.role}</p> : null}
      <button
        type="submit"
        disabled={loading}
        className="rounded-xl bg-slate-900 px-4 py-2 font-bold text-white disabled:opacity-60"
      >
        {loading ? 'Creating account...' : 'Create account'}
      </button>
    </form>
  )
}

function RegisterAssist({ onRegisterAsCandidate, onRegisterAsEmployer }) {
  return (
    <div className="mt-4 rounded-xl border border-sky-200 bg-sky-50 p-3">
      <p className="text-sm font-semibold text-sky-900">Email not found in system</p>
      <p className="mt-1 text-xs text-sky-700">
        You can create a new account as Candidate or Employer.
      </p>
      <div className="mt-3 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={onRegisterAsCandidate}
          className="rounded-lg bg-slate-900 px-3 py-2 text-sm font-semibold text-white"
        >
          Register as Candidate
        </button>
        <button
          type="button"
          onClick={onRegisterAsEmployer}
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-semibold text-slate-700"
        >
          Register as Employer
        </button>
      </div>
    </div>
  )
}

export default function AuthPage() {
  const { login, register, refreshAccessToken } = useAuth()
  const { showToast } = useToast()
  const navigate = useNavigate()
  const allowSelfRegister = import.meta.env.VITE_ALLOW_SELF_REGISTER === 'true'

  const [mode, setMode] = useState('login')
  const [loginTrack, setLoginTrack] = useState('candidate')
  const [loginData, setLoginData] = useState({ email: '', password: '' })
  const [registerData, setRegisterData] = useState(registerDefaults)
  const [showRegisterAssist, setShowRegisterAssist] = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})
  const { loading, error, success, run } = useRequestState()

  const selectedTrack = loginTracks.find((item) => item.key === loginTrack) || loginTracks[0]

  function handleLoginEmailChange(value) {
    setLoginData((prev) => ({ ...prev, email: value }))
  }

  function handleLoginPasswordChange(value) {
    setLoginData((prev) => ({ ...prev, password: value }))
  }

  function handleRegisterChange(changes) {
    setRegisterData((prev) => ({ ...prev, ...changes }))
  }

  async function onLogin(event) {
    event.preventDefault()
    setFieldErrors({})

    const validation = validateForm(loginSchema, loginData)
    if (!validation.ok) {
      setFieldErrors(validation.fieldErrors)
      return
    }

    try {
      const authData = await run(() => login(loginData), {
        successMessage: `${selectedTrack.label} login successful`,
      })
      setShowRegisterAssist(false)

      if (authData?.role && authData.role !== selectedTrack.expectedRole) {
        showToast(
          `Signed in as ${authData.role}, not ${selectedTrack.expectedRole}. Redirect adjusted.`,
          'warning',
          4200,
        )
      }

      const destination = resolveDestination(authData?.role)

      navigate(destination)
    } catch (err) {
      const message = getErrorMessage(err).toLowerCase()
      setShowRegisterAssist(message.includes('user not found') || message.includes('not found'))
    }
  }

  async function onRegister(event) {
    event.preventDefault()
    setFieldErrors({})

    const validation = validateForm(registerSchema, registerData)
    if (!validation.ok) {
      setFieldErrors(validation.fieldErrors)
      return
    }

    try {
      await run(() => register(registerData), {
        successMessage: 'Registration successful',
      })
      setShowRegisterAssist(false)
      navigate('/')
    } catch {
      // Request-state hook already handles error state and toasts.
    }
  }

  function beginRegister(role) {
    setMode('register')
    setRegisterData((prev) => ({
      ...prev,
      email: loginData.email || prev.email,
      role,
    }))
  }

  async function onRefreshToken() {
    try {
      await run(() => refreshAccessToken(), {
        successMessage: 'Access token refreshed',
      })
      navigate('/')
    } catch {
      // Request-state hook already handles error state and toasts.
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <SectionCard title="Secure Access" subtitle="Authorized personnel only.">
        <div className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          This portal is restricted. Use organization-provided credentials.
        </div>

        <ModeToggle mode={mode} allowSelfRegister={allowSelfRegister} onSelectMode={setMode} />

        <ErrorMessage text={error} />
        <SuccessMessage text={success} />

        {mode === 'login' || !allowSelfRegister ? (
          <LoginForm
            loading={loading}
            loginTrack={loginTrack}
            loginData={loginData}
            selectedTrack={selectedTrack}
            fieldErrors={fieldErrors}
            onChangeTrack={setLoginTrack}
            onChangeEmail={handleLoginEmailChange}
            onChangePassword={handleLoginPasswordChange}
            onSubmit={onLogin}
            onRefreshToken={onRefreshToken}
          />
        ) : (
          <RegisterForm
            loading={loading}
            registerData={registerData}
            fieldErrors={fieldErrors}
            onChangeRegister={handleRegisterChange}
            onSubmit={onRegister}
          />
        )}

        {allowSelfRegister && mode === 'login' && showRegisterAssist ? (
          <RegisterAssist
            onRegisterAsCandidate={() => beginRegister('JOB_SEEKER')}
            onRegisterAsEmployer={() => beginRegister('RECRUITER')}
          />
        ) : null}

        {!allowSelfRegister ? (
          <p className="mt-4 text-xs text-slate-500">
            Account creation is disabled in this environment. Contact an administrator.
          </p>
        ) : null}
      </SectionCard>
    </div>
  )
}
