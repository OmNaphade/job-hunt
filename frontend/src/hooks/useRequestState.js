import { useState } from 'react'
import { getErrorMessage } from '../lib/api'
import { useToast } from '../components/ToastProvider'

export function useRequestState() {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function run(action, options = {}) {
    const {
      successMessage,
      onSuccess,
      onError,
      showSuccessToast = true,
      showErrorToast = true,
    } = options

    setLoading(true)
    setError('')
    setSuccess('')

    try {
      const result = await action()

      if (successMessage) {
        setSuccess(successMessage)
        if (showSuccessToast) {
          showToast(successMessage, 'success')
        }
      }

      if (onSuccess) {
        onSuccess(result)
      }

      return result
    } catch (err) {
      const message = getErrorMessage(err)
      setError(message)

      if (showErrorToast) {
        showToast(message, 'error', 4000)
      }

      if (onError) {
        onError(err)
      }

      throw err
    } finally {
      setLoading(false)
    }
  }

  return {
    loading,
    error,
    success,
    setError,
    setSuccess,
    run,
  }
}
