import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { DEFAULT_AGENT_API_BASE_URL, resolveAgentApiBaseUrl } from "../lib/agent-base-url";
import {
  buildJudgeResultParams,
  buildJudgeRunPayload,
  buildJudgeSubmitPayload,
  normalizeBackendLongId
} from "../lib/judge-route-contracts";

async function main() {
  assert.equal(normalizeBackendLongId("2040750992449384449"), "2040750992449384449");
  assert.equal(normalizeBackendLongId(" 2040750992449384449 "), "2040750992449384449");
  assert.equal(normalizeBackendLongId("two-sum"), null);

  const runPayload = buildJudgeRunPayload(
    {
      questionId: "2040750992449384449",
      examId: "2041108424049139714",
      customInputs: ["1 2"]
    },
    0,
    "class Solution {}"
  );
  assert.equal(runPayload?.questionId, "2040750992449384449");
  assert.equal(typeof runPayload?.questionId, "string");
  assert.equal(runPayload?.examId, "2041108424049139714");
  assert.deepEqual(runPayload?.customInputs, ["1 2"]);

  const submitPayload = buildJudgeSubmitPayload(
    {
      questionId: "2040750992449384449",
      examId: "2041108424049139714"
    },
    0,
    "class Solution {}"
  );
  assert.equal(submitPayload?.questionId, "2040750992449384449");
  assert.equal(typeof submitPayload?.questionId, "string");
  assert.equal(submitPayload?.examId, "2041108424049139714");

  const params = buildJudgeResultParams("2040750992449384449", "req-1", "2041108424049139714");
  assert.equal(params?.get("questionId"), "2040750992449384449");
  assert.equal(params?.get("examId"), "2041108424049139714");
  assert.equal(params?.get("requestId"), "req-1");

  const originalSyncodeAgentBaseUrl = process.env.SYNCODE_AGENT_BASE_URL;

  try {
    delete process.env.SYNCODE_AGENT_BASE_URL;
    assert.equal(resolveAgentApiBaseUrl(), DEFAULT_AGENT_API_BASE_URL);

    process.env.SYNCODE_AGENT_BASE_URL = "http://127.0.0.1:29090/";
    assert.equal(resolveAgentApiBaseUrl(), "http://127.0.0.1:29090");
  } finally {
    if (typeof originalSyncodeAgentBaseUrl === "string") {
      process.env.SYNCODE_AGENT_BASE_URL = originalSyncodeAgentBaseUrl;
    } else {
      delete process.env.SYNCODE_AGENT_BASE_URL;
    }
  }

  const createRunRouteSource = readFileSync(
    fileURLToPath(new URL("../app/api/ai/runs/route.ts", import.meta.url)),
    "utf8"
  );
  assert.match(createRunRouteSource, /requestJson<AiRunCreateResponse>\("\/ai\/runs"/);

  const eventRouteSource = readFileSync(
    fileURLToPath(new URL("../app/api/ai/runs/[runId]/events/route.ts", import.meta.url)),
    "utf8"
  );
  assert.match(eventRouteSource, /\/ai\/runs\/\$\{encodeURIComponent\(runId\)\}\/events/);

  const artifactRouteSource = readFileSync(
    fileURLToPath(new URL("../app/api/ai/runs/[runId]/artifacts/route.ts", import.meta.url)),
    "utf8"
  );
  assert.match(artifactRouteSource, /requestJson<AiArtifact\[]>\(`\/ai\/runs\/\$\{encodeURIComponent\(runId\)\}\/artifacts`/);

  const agentContractDoc = readFileSync(
    fileURLToPath(new URL("../../../../../docs/agent-frontend-contract.md", import.meta.url)),
    "utf8"
  );
  assert.match(agentContractDoc, /external retrieval evidence/i);
}

void main();
