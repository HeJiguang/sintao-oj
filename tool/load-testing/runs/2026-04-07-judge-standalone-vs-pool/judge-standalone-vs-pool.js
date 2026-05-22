import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:9204';
const mode = __ENV.MODE || 'pool';
const vus = Number(__ENV.VUS || 10);
const duration = __ENV.DURATION || '45s';
const pauseSeconds = Number(__ENV.PAUSE_SECONDS || 0);
const userId = Number(__ENV.JUDGE_USER_ID || 99999);
const questionId = Number(__ENV.JUDGE_QUESTION_ID || 99992);
const programType = Number(__ENV.JUDGE_PROGRAM_TYPE || 0);
const difficulty = Number(__ENV.JUDGE_DIFFICULTY || 1);
const timeLimit = Number(__ENV.JUDGE_TIME_LIMIT || 1000);
const spaceLimit = Number(__ENV.JUDGE_SPACE_LIMIT || 268435456);
const userCode = __ENV.JUDGE_USER_CODE || 'public class Solution { public static int add(int a, int b) { return a + b; } public static void main(String[] args) { System.out.print(add(1, 2)); } }';
const inputList = JSON.parse(__ENV.JUDGE_INPUT_JSON || '[""]');
const outputList = JSON.parse(__ENV.JUDGE_OUTPUT_JSON || '["3"]');

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<15000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const response = http.post(
    `${baseUrl}/judge/doJudgeJavaCode`,
    JSON.stringify({
      userId,
      questionId,
      programType,
      difficulty,
      timeLimit,
      spaceLimit,
      userCode,
      inputList,
      outputList,
    }),
    {
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      tags: {
        endpoint: '/judge/doJudgeJavaCode',
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
    'judge payload is valid': (r) => {
      try {
        const payload = JSON.parse(r.body);
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
