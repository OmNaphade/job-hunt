import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, getErrorMessage, tokenStorage } from '../lib/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  const refreshMe = useCallback(async () => {
    const token = tokenStorage.get()
    if (!token) {
      setUser(null)
      setLoading(false)
      return
    }

    try {
      const response = await api.get('/api/auth/me')
      setUser(response.data)
    } catch {
      tokenStorage.clear()
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refreshMe()
  }, [refreshMe])

  const login = useCallback(async (payload) => {
    const response = await api.post('/api/auth/login', payload)
    tokenStorage.set(response.data.accessToken)
    tokenStorage.setRefresh(response.data.refreshToken)
    await refreshMe()
    return response.data
  }, [refreshMe])

  const register = useCallback(async (payload) => {
    const response = await api.post('/api/auth/register', payload)
    tokenStorage.set(response.data.accessToken)
    tokenStorage.setRefresh(response.data.refreshToken)
    await refreshMe()
    return response.data
  }, [refreshMe])

  const refreshAccessToken = useCallback(async () => {
    const refreshToken = tokenStorage.getRefresh()
    if (!refreshToken) {
      throw new Error('No refresh token available')
    }

    const response = await api.post('/api/auth/refresh', { refreshToken })
    tokenStorage.set(response.data.accessToken)
    if (response.data.refreshToken) {
      tokenStorage.setRefresh(response.data.refreshToken)
    }
    await refreshMe()
    return response.data
  }, [refreshMe])

  const logout = useCallback(async () => {
    try {
      const refreshToken = tokenStorage.getRefresh()
      if (refreshToken) {
        await api.post('/api/auth/logout', { refreshToken })
      }
    } catch {
      // Ignore API errors during logout; local cleanup still proceeds.
    } finally {
      tokenStorage.clear()
      setUser(null)
    }
  }, [])

  const value = useMemo(
    () => ({
      user,
      loading,
      isAuthenticated: Boolean(user),
      role: user?.role || null,
      login,
      register,
      refreshAccessToken,
      logout,
      refreshMe,
      getErrorMessage,
    }),
    [user, loading, login, register, refreshAccessToken, logout, refreshMe],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
