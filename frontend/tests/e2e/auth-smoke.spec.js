import { expect, test } from '@playwright/test'

test('auth page renders login tracks and restricted message', async ({ page }) => {
  await page.goto('/auth')

  await expect(page.getByText('This portal is restricted. Use organization-provided credentials.')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Candidate Login' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Employer Login' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Admin Login' })).toBeVisible()
})

test('protected route redirects unauthenticated users to auth', async ({ page }) => {
  await page.goto('/monitoring')
  await expect(page).toHaveURL(/\/auth$/)
})
