import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 25 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.03'],
    http_req_duration: ['p(95)<700'],
  },
}

const BASE = __ENV.BASE_URL || 'http://localhost:8080'
const EMAIL = __ENV.USER_EMAIL || 'candidate@jobportal.local'
const PASSWORD = __ENV.USER_PASSWORD || 'Pass123!'

export default function () {
  const loginRes = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  )

  check(loginRes, {
    'login status is 200': (r) => r.status === 200,
  })

  const token = loginRes.json('accessToken')
  const authHeaders = {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  }

  const searchRes = http.get(`${BASE}/api/jobs/search?keyword=Engineer&minSalary=800000`, {
    headers: authHeaders,
  })

  check(searchRes, {
    'search status is 200': (r) => r.status === 200,
  })

  const jobs = searchRes.json() || []
  if (Array.isArray(jobs) && jobs.length > 0) {
    const applyRes = http.post(
      `${BASE}/api/applications`,
      JSON.stringify({ jobId: jobs[0].id }),
      { headers: authHeaders },
    )

    check(applyRes, {
      'apply status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    })
  }

  sleep(1)
}
