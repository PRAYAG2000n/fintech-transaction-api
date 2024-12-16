import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 100 },
    { duration: '1m', target: 500 },
    { duration: '30s', target: 1000 },
    { duration: '1m', target: 1000 },
    { duration: '30s', target: 0 },
  ],
};

const BASE = 'http://localhost:8081';

export default function () {
  const uid = `${__VU}-${__ITER}-${Date.now()}`;
  const email = `user${uid}@test.com`;

  const reg = http.post(`${BASE}/api/v1/auth/register`, JSON.stringify({
    email: email, password: 'SecurePass123!', firstName: 'Load', lastName: 'Test',
  }), { headers: { 'Content-Type': 'application/json' } });

  check(reg, { 'register status 201': (r) => r.status === 201 });

  if (reg.status === 201) {
    const token = JSON.parse(reg.body).data.accessToken;
    const bal = http.get(`${BASE}/api/v1/accounts/balance`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    check(bal, { 'balance status 200': (r) => r.status === 200 });
  }

  sleep(1);
}
