import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ConfirmDialogProvider, useConfirm } from './ConfirmDialogProvider'

function Harness() {
  const confirm = useConfirm()
  const [result, setResult] = useState(null)

  async function ask() {
    const ok = await confirm({ title: 'Delete this?', message: 'No going back.', confirmLabel: 'Delete' })
    setResult(ok ? 'confirmed' : 'cancelled')
  }

  return (
    <div>
      <button onClick={ask}>Ask</button>
      <p>Result: {result ?? 'none'}</p>
    </div>
  )
}

function renderHarness() {
  return render(
    <ConfirmDialogProvider>
      <Harness />
    </ConfirmDialogProvider>,
  )
}

describe('ConfirmDialogProvider', () => {
  it('resolves true when the confirm button is clicked', async () => {
    const user = userEvent.setup()
    renderHarness()

    await user.click(screen.getByText('Ask'))
    expect(screen.getByText('Delete this?')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Delete' }))
    expect(await screen.findByText('Result: confirmed')).toBeInTheDocument()
    expect(screen.queryByText('Delete this?')).not.toBeInTheDocument()
  })

  it('resolves false when Cancel is clicked', async () => {
    const user = userEvent.setup()
    renderHarness()

    await user.click(screen.getByText('Ask'))
    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(await screen.findByText('Result: cancelled')).toBeInTheDocument()
  })

  it('resolves false on Escape', async () => {
    const user = userEvent.setup()
    renderHarness()

    await user.click(screen.getByText('Ask'))
    await user.keyboard('{Escape}')

    expect(await screen.findByText('Result: cancelled')).toBeInTheDocument()
  })
})
