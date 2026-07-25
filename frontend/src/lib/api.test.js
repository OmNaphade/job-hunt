import { describe, expect, it } from 'vitest'
import { getErrorMessage } from './api'

describe('getErrorMessage', () => {
  it('returns response.data.message when available', () => {
    const error = { response: { data: { message: 'Invalid credentials' } } }
    expect(getErrorMessage(error)).toBe('Invalid credentials')
  })

  it('returns response.data.error when message is unavailable', () => {
    const error = { response: { data: { error: 'Forbidden' } } }
    expect(getErrorMessage(error)).toBe('Forbidden')
  })

  it('returns generic fallback for unknown errors', () => {
    expect(getErrorMessage(null)).toBe('Something went wrong')
  })
})
