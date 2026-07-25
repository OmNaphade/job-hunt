import { z } from 'zod'

const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/

export const loginSchema = z.object({
  email: z.string().trim().email('Enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
})

export const registerSchema = z.object({
  email: z.string().trim().email('Enter a valid email address'),
  password: z
    .string()
    .regex(passwordRegex, 'Password must include letters and numbers and be at least 8 chars'),
  role: z.enum(['JOB_SEEKER', 'RECRUITER']),
})

export function validateForm(schema, payload) {
  const result = schema.safeParse(payload)
  if (result.success) {
    return { ok: true, data: result.data, fieldErrors: {} }
  }

  const fieldErrors = {}
  for (const issue of result.error.issues) {
    const field = issue.path[0]
    if (field && !fieldErrors[field]) {
      fieldErrors[field] = issue.message
    }
  }

  return { ok: false, fieldErrors }
}
