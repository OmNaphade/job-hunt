import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  vus: 10,
  duration: '2m',
  thresholds: {
    http_req_failed: ['rate<0.03'],
    http_req_duration: ['p(95)<500'],
  },
}

const BASE = __ENV.BASE_URL || 'http://localhost:8080'
const EMAIL = __ENV.USER_EMAIL || 'candidate@jobportal.local'
const PASSWORD = __ENV.USER_PASSWORD || 'Pass123!'

function notificationsReadScenario() {
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
  }

  const unreadRes = http.get(`${BASE}/api/notifications/unread`, { headers: authHeaders })
  check(unreadRes, {
    'unread status is 200': (r) => r.status === 200,
  })

  const countRes = http.get(`${BASE}/api/notifications/unread/count`, { headers: authHeaders })
  check(countRes, {
    'count status is 200': (r) => r.status === 200,
  })

  sleep(0.5)
}

export default notificationsReadScenario
