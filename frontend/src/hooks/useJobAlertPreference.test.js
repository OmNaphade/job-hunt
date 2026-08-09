import { act, renderHook } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { useJobAlertPreference } from './useJobAlertPreference'

const STORAGE_KEY = 'jp-job-alerts-enabled'

afterEach(() => {
  window.localStorage.removeItem(STORAGE_KEY)
})

describe('useJobAlertPreference', () => {
  it('defaults to enabled when nothing is stored', () => {
    const { result } = renderHook(() => useJobAlertPreference())
    const [enabled] = result.current
    expect(enabled).toBe(true)
  })

  it('reads a previously stored preference on init', () => {
    window.localStorage.setItem(STORAGE_KEY, 'false')
    const { result } = renderHook(() => useJobAlertPreference())
    const [enabled] = result.current
    expect(enabled).toBe(false)
  })

  it('persists updates to localStorage and reflects them in state', () => {
    const { result } = renderHook(() => useJobAlertPreference())

    act(() => {
      const [, setEnabled] = result.current
      setEnabled(false)
    })

    const [enabled] = result.current
    expect(enabled).toBe(false)
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('false')
  })
})
