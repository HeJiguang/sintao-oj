import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:19090';
const mode = __ENV.MODE || 'sync';
const token = __ENV.AUTH_TOKEN || '';
const questionId = __ENV.QUESTION_ID || '99992';
const vus = Number(__ENV.VUS || 20);
const duration = __ENV.DURATION || '60s';
const pauseSeconds = Number(__ENV.PAUSE_SECONDS || 1);
const userCode = __ENV.USER_CODE || 'public class Solution { public static int add(int a, int b) { return a + b; } }';

const endpoint = mode === 'async'
  ? '/friend/user/question/rabbit/submit'
  : '/friend/user/question/submit';

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<5000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const response = http.post(
    `${baseUrl}${endpoint}`,
    JSON.stringify({
      questionId: Number(questionId),
      programType: 0,
      userCode,
    }),
    {
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      tags: {
        endpoint,
        mode,
      },
    }
  );

  check(response, {
    'status is 200': (r) => r.status === 200,
    'business code is 1000': (r) => {
      try {
        return JSON.parse(r.body).code === 1000;
      } catch (_) {
        return false;
      }
    },
    'sync or async payload is valid': (r) => {
      try {
        const payload = JSON.parse(r.body);
        if (mode === 'async') {
          return payload?.data?.requestId && payload?.data?.status === 'ACCEPTED';
        }
        return payload?.data && Object.prototype.hasOwnProperty.call(payload.data, 'pass');
      } catch (_) {
        return false;
      }
    },
  });

  if (pauseSeconds > 0) {
    sleep(pauseSeconds);
  }
}
