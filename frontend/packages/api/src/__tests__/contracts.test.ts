import assert from "node:assert/strict";

import * as api from "../index";

async function main() {
  const problems = await api.getProblemList();
  const detail = await api.getProblemDetail("1");

  assert.ok(problems.length >= 1);
  assert.ok(detail.questionId);
  assert.equal(typeof api.getHotProblemList, "function");
  assert.equal(typeof api.getPublicMessages, "function");

  assert.equal(api.resolveBackendBaseUrl("http://localhost:19090/"), "http://localhost:19090");
  assert.equal(api.resolveJudgeWebSocketUrl("http://localhost:19090"), "ws://localhost:19090/friend/ws/judge/result");
  assert.equal(api.unwrapData({ code: 1000, msg: "ok", data: { ok: true } }).ok, true);
  assert.equal(api.unwrapTable({ code: 1000, msg: "ok", rows: [1, 2], total: 2 }).rows.length, 2);
  assert.equal(api.normalizeDifficulty(1), "Easy");
  assert.equal(api.normalizeDifficulty(2), "Medium");
  assert.equal(api.normalizeDifficulty(3), "Hard");
  assert.equal(api.programTypeFromLanguage("java"), 0);
  assert.equal(api.programTypeFromLanguage("cpp"), 1);
  assert.equal(api.programTypeFromLanguage("go"), 2);
  assert.equal(api.isJudgeLanguageSupported("python"), false);
}

void main();
