import { useCallback, useEffect, useState } from 'react'

const STORAGE_KEY = 'jp-job-alerts-enabled'

function readPreference() {
  if (typeof window === 'undefined') return true
  const raw = window.localStorage.getItem(STORAGE_KEY)
  return raw === null ? true : raw === 'true'
}

export function useJobAlertPreference() {
  const [enabled, setEnabled] = useState(readPreference)

  useEffect(() => {
    function handleStorage(event) {
      if (event.key === STORAGE_KEY) setEnabled(readPreference())
    }
    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  const setPreference = useCallback((value) => {
    setEnabled(value)
    window.localStorage.setItem(STORAGE_KEY, String(value))
  }, [])

  return [enabled, setPreference]
}
