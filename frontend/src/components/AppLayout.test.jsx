import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AppLayout from './AppLayout'
import { useAuth } from '../context/AuthContext'

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}))

vi.mock('../lib/api', () => ({
  api: { get: vi.fn().mockRejectedValue(new Error('not available in tests')) },
}))

function renderLayout() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
        <AppLayout>
          <p>Page content</p>
        </AppLayout>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AppLayout', () => {
  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme')
    window.localStorage.clear()
    useAuth.mockReturnValue({
      isAuthenticated: true,
      user: { email: 'admin@jobportal.local', role: 'ADMIN' },
      logout: vi.fn().mockResolvedValue(undefined),
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('toggles the data-theme attribute and persists the choice', async () => {
    const user = userEvent.setup()
    renderLayout()

    expect(document.documentElement.dataset.theme).toBe('light')

    await user.click(screen.getByLabelText('Switch to night mode'))

    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(window.localStorage.getItem('jp-theme')).toBe('dark')
  })

  it('opens the account dropdown with profile, settings, and logout, and only one Profile entry point', async () => {
    const user = userEvent.setup()
    renderLayout()

    // "Profile" must not appear as a top-level nav link (only inside the dropdown).
    expect(screen.queryByRole('link', { name: 'Profile' })).not.toBeInTheDocument()

    await user.click(screen.getByLabelText('Open account menu'))

    expect(screen.getByText('admin@jobportal.local')).toBeInTheDocument()
    expect(screen.getByText('View profile')).toBeInTheDocument()
    expect(screen.getByText('Account settings')).toBeInTheDocument()
    expect(screen.getByText('Logout')).toBeInTheDocument()
  })

  it('closes the account dropdown on Escape', async () => {
    const user = userEvent.setup()
    renderLayout()

    await user.click(screen.getByLabelText('Open account menu'))
    expect(screen.getByText('View profile')).toBeInTheDocument()

    await user.keyboard('{Escape}')
    await waitFor(() => expect(screen.queryByText('View profile')).not.toBeInTheDocument())
  })

  it('calls logout and does not crash when Logout is clicked', async () => {
    const user = userEvent.setup()
    const logout = vi.fn().mockResolvedValue(undefined)
    useAuth.mockReturnValue({
      isAuthenticated: true,
      user: { email: 'admin@jobportal.local', role: 'ADMIN' },
      logout,
    })
    renderLayout()

    await user.click(screen.getByLabelText('Open account menu'))
    await user.click(screen.getByText('Logout'))

    expect(logout).toHaveBeenCalledTimes(1)
  })
})
