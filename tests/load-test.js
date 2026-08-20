import http from 'k6/http';
import { check, sleep } from 'k6';

// k6 부하 테스트 설정: 단계별 동시 사용자(VU) 증감 시뮬레이션
export const options = {
  stages: [
    { duration: '10s', target: 20 },  // Ramp-up to 20 users over 10s
    { duration: '30s', target: 50 },  // Stay at 50 users for 30s
    { duration: '10s', target: 0 },   // Ramp-down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.01'],   // Error rate must be less than 1%
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080';

export default function () {
  const headers = {
    'Content-Type': 'application/json',
  };

  // 1. 계좌 단건 조회 (GET /accounts/1)
  const getAccountRes = http.get(`${BASE_URL}/accounts/1`, { headers });
  check(getAccountRes, {
    'GET /accounts/1 status is 200 or 401': (r) => r.status === 200 || r.status === 401,
  });

  sleep(0.5);

  // 2. 입금 시뮬레이션 (POST /transactions/deposit)
  const depositPayload = JSON.stringify({
    toAccountId: 1,
    amount: 1000,
  });
  const depositRes = http.post(`${BASE_URL}/transactions/deposit`, depositPayload, { headers });
  check(depositRes, {
    'POST /transactions/deposit status is 201 or 401': (r) => r.status === 201 || r.status === 401,
  });

  sleep(0.5);

  // 3. 알림 내역 조회 (GET /notifications)
  const notifRes = http.get(`${BASE_URL}/notifications`, { headers });
  check(notifRes, {
    'GET /notifications status is 200 or 401': (r) => r.status === 200 || r.status === 401,
  });

  sleep(1);
}
