import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import SectionCard from '../components/SectionCard'
import { ErrorMessage, SuccessMessage } from '../components/Message'
import { useToast } from '../components/ToastProvider'
import { useAuth } from '../context/AuthContext'
import { useRequestState } from '../hooks/useRequestState'
import { api, getErrorMessage } from '../lib/api'
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
  onForgotPassword,
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
        className="input"
        placeholder="Email"
        type="email"
        required
        value={loginData.email}
        onChange={(event) => onChangeEmail(event.target.value)}
      />
      {fieldErrors.email ? <p className="text-xs font-semibold text-rose-700">{fieldErrors.email}</p> : null}
      <input
        className="input"
        placeholder="Password"
        type="password"
        required
        value={loginData.password}
        onChange={(event) => onChangePassword(event.target.value)}
      />
      {fieldErrors.password ? <p className="text-xs font-semibold text-rose-700">{fieldErrors.password}</p> : null}
      <button
        type="button"
        onClick={onForgotPassword}
        className="-mt-1 justify-self-end text-xs font-semibold text-indigo-600 hover:underline"
      >
        Forgot password?
      </button>
      <button
        type="submit"
        disabled={loading}
        className="btn btn-primary"
      >
        {loading ? 'Signing in...' : `Sign in as ${selectedTrack.label}`}
      </button>
      <button
        type="button"
        onClick={onRefreshToken}
        className="btn btn-secondary"
      >
        Refresh Session Token
      </button>
    </form>
  )
}

function ResetPasswordForm({ onDone }) {
  const [step, setStep] = useState('request')
  const [email, setEmail] = useState('')
  const [token, setToken] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const { loading, error, success, setError, run } = useRequestState()

  async function onRequestReset(event) {
    event.preventDefault()
    try {
      await run(() => api.post('/api/auth/password-reset/request', { email }), {
        successMessage: "If that email exists, a reset token has been generated.",
      })
      setStep('confirm')
    } catch {
      // Request-state hook already surfaces the error.
    }
  }

  async function onConfirmReset(event) {
    event.preventDefault()
    setError('')
    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match')
      return
    }
    try {
      await run(() => api.post('/api/auth/password-reset/confirm', { token, newPassword }), {
        successMessage: 'Password reset. You can sign in with your new password now.',
      })
      setToken('')
      setNewPassword('')
      setConfirmPassword('')
    } catch {
      // Request-state hook already surfaces the error.
    }
  }

  return (
    <div className="mt-4 grid gap-3">
      <p className="text-sm font-semibold text-slate-700">Reset your password</p>
      <ErrorMessage text={error} />
      <SuccessMessage text={success} />

      {step === 'request' ? (
        <form className="grid gap-3" onSubmit={onRequestReset}>
          <input
            className="input"
            placeholder="Account email"
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <button type="submit" disabled={loading} className="btn btn-primary">
            {loading ? 'Sending...' : 'Send reset instructions'}
          </button>
          <button type="button" onClick={() => setStep('confirm')} className="btn btn-secondary btn-sm">
            I already have a reset token
          </button>
        </form>
      ) : (
        <form className="grid gap-3" onSubmit={onConfirmReset}>
          <p className="field-hint -mt-1">
            No email service is wired up in this local environment &mdash; the reset token is written to the
            auth_service application logs (<code>Password reset token for &lt;email&gt;: ...</code>), not sent
            anywhere. Copy it from there.
          </p>
          <input
            className="input"
            placeholder="Reset token"
            required
            value={token}
            onChange={(event) => setToken(event.target.value)}
          />
          <input
            className="input"
            placeholder="New password"
            type="password"
            required
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
          />
          <input
            className="input"
            placeholder="Confirm new password"
            type="password"
            required
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
          />
          <button type="submit" disabled={loading} className="btn btn-primary">
            {loading ? 'Resetting...' : 'Reset password'}
          </button>
          <button type="button" onClick={() => setStep('request')} className="btn btn-secondary btn-sm">
            Back
          </button>
        </form>
      )}

      <button type="button" onClick={onDone} className="text-xs font-semibold text-slate-500 hover:underline">
        &larr; Back to login
      </button>
    </div>
  )
}

function RegisterForm({ loading, registerData, fieldErrors, onChangeRegister, onSubmit }) {
  return (
    <form className="mt-4 grid gap-3" onSubmit={onSubmit}>
      <input
        className="input"
        placeholder="Email"
        type="email"
        required
        value={registerData.email}
        onChange={(event) => onChangeRegister({ email: event.target.value })}
      />
      {fieldErrors.email ? <p className="text-xs font-semibold text-rose-700">{fieldErrors.email}</p> : null}
      <input
        className="input"
        placeholder="Password"
        type="password"
        required
        value={registerData.password}
        onChange={(event) => onChangeRegister({ password: event.target.value })}
      />
      {fieldErrors.password ? <p className="text-xs font-semibold text-rose-700">{fieldErrors.password}</p> : null}
      <select
        className="input"
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
        className="btn btn-primary"
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
          className="btn btn-dark btn-sm"
        >
          Register as Candidate
        </button>
        <button
          type="button"
          onClick={onRegisterAsEmployer}
          className="btn btn-secondary btn-sm"
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
  const [showResetFlow, setShowResetFlow] = useState(false)
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
    <div className="grid gap-6 lg:grid-cols-[1.05fr_1fr] lg:items-stretch">
      <div className="relative isolate hidden overflow-hidden rounded-[2rem] bg-[linear-gradient(155deg,#0b0e21,#171b3a_45%,#312e81_85%,#4c1d95)] p-10 text-white shadow-[0_30px_70px_-30px_rgba(30,27,75,0.7)] lg:flex lg:flex-col lg:justify-between">
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 opacity-40"
          style={{
            backgroundImage:
              'radial-gradient(circle at 15% 20%, rgba(129,140,248,0.5), transparent 45%), radial-gradient(circle at 85% 0%, rgba(56,189,248,0.35), transparent 40%), radial-gradient(circle at 90% 90%, rgba(244,114,182,0.3), transparent 45%)',
          }}
        />
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 opacity-[0.15]"
          style={{
            backgroundImage: 'radial-gradient(rgba(255,255,255,0.6) 1px, transparent 1px)',
            backgroundSize: '22px 22px',
          }}
        />

        <div className="relative">
          <span className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-bold uppercase tracking-[0.18em] text-indigo-100 backdrop-blur">
            Internal Operations Portal
          </span>
          <h1 className="nav-title mt-6 text-4xl font-black leading-tight tracking-tight">
            Job Portal
            <br />
            Control Deck
          </h1>
          <p className="mt-4 max-w-sm text-sm leading-relaxed text-indigo-100/80">
            One command surface for candidates, employers, and admins across the auth, job, company,
            application, and notification microservices.
          </p>
        </div>

        <div className="relative mt-10 grid gap-3">
          {[
            { label: 'Candidates', hint: 'Search, save, and apply to live openings' },
            { label: 'Employers', hint: 'Publish roles and manage applicant pipelines' },
            { label: 'Admins', hint: 'Observe service health and manage accounts' },
          ].map((item) => (
            <div key={item.label} className="flex items-start gap-3 rounded-2xl border border-white/10 bg-white/5 p-3 backdrop-blur">
              <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-indigo-400 to-fuchsia-400 text-[11px] font-black text-indigo-950">
                ✓
              </span>
              <div>
                <p className="text-sm font-bold text-white">{item.label}</p>
                <p className="text-xs text-indigo-100/70">{item.hint}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="flex flex-col justify-center">
        <SectionCard title="Secure Access" subtitle="Authorized personnel only.">
          <div className="flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
            <span aria-hidden>🔒</span>
            <span>This portal is restricted. Use organization-provided credentials.</span>
          </div>

          {showResetFlow ? (
            <ResetPasswordForm onDone={() => setShowResetFlow(false)} />
          ) : (
            <>
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
                  onForgotPassword={() => setShowResetFlow(true)}
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
            </>
          )}
        </SectionCard>
      </div>
    </div>
  )
}
