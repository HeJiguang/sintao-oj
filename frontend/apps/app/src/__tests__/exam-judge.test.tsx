import assert from "node:assert/strict";
import * as React from "react";
import { renderToStaticMarkup } from "react-dom/server";

import ExamWorkspacePage from "../app/exams/[examId]/page";
import { mapJudgeHistoryRow } from "../lib/judge-history";
import { normalizeJudgeOutcome } from "../lib/judge-result";

async function main() {
  const examHtml = renderToStaticMarkup(
    await ExamWorkspacePage({ params: Promise.resolve({ examId: "exam-checkpoint-02" }) })
  );

  assert.match(examHtml, /热度 83/);
  assert.doesNotMatch(examHtml, /热度 98/);
  assert.match(examHtml, /1 \/ 2/);

  const history = mapJudgeHistoryRow(
    {
      submitId: 9,
      programType: 0,
      pass: 1,
      exeMessage: "Accepted",
      useTime: 12,
      useMemory: 2048,
      createTime: "2026-03-31 09:45:00"
    },
    0
  );

  assert.equal(history.runtime, "12 ms");
  assert.equal(history.memory, "2048 KB");
  assert.equal(history.submittedAt, "2026-03-31 09:45:00");

  const outcome = normalizeJudgeOutcome({
    pass: 0,
    exeMessage: "Wrong Answer",
    userExeResultList: [
      { input: "1 2", output: "3", exeOutput: "4" },
      { input: "2 2", output: "4", exeOutput: "4" }
    ]
  });
  assert.equal(outcome.status, "Wrong Answer");
  assert.equal(outcome.failedCases?.length, 1);
  assert.equal(outcome.failedCases?.[0]?.input, "1 2");
  assert.equal(outcome.failedCases?.[0]?.expectedOutput, "3");
  assert.equal(outcome.failedCases?.[0]?.actualOutput, "4");
}

void main();
