import { NextRequest, NextResponse } from "next/server";

import type { ApiEnvelope, SubmissionRecord } from "@aioj/api";
import { requestJson, unwrapData } from "@aioj/api";

import { normalizeJudgeOutcome } from "../../../../lib/judge-result";
import { getServerAccessToken } from "../../../../lib/server-auth";

type BackendSubmissionRow = {
  submitId?: string | number | null;
  programType?: number | null;
  pass?: number | null;
  score?: number | null;
  exeMessage?: string | null;
  useTime?: number | null;
  useMemory?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
};

function mapProgramType(programType?: number | null) {
  if (programType === 0) return "Java";
  if (programType === 1) return "C++";
  if (programType === 2) return "Go";
  return "Java";
}

function mapSubmission(row: BackendSubmissionRow, index: number): SubmissionRecord {
  const outcome = normalizeJudgeOutcome({
    pass: row.pass,
    exeMessage: row.exeMessage
  });

  return {
    submissionId: row.submitId ? String(row.submitId) : `submission-${index + 1}`,
    status: outcome.status,
    language: mapProgramType(row.programType),
    runtime: row.useTime != null ? `${row.useTime} ms` : "--",
    memory: row.useMemory != null ? `${row.useMemory} KB` : "--",
    submittedAt: (row.createTime ?? row.updateTime ?? "").replace("T", " ") || "刚刚",
    notes: outcome.message
  };
}

export async function GET(request: NextRequest) {
  const token = await getServerAccessToken();
  if (!token) {
    return NextResponse.json({ message: "未登录，无法读取提交记录。" }, { status: 401 });
  }

  const questionId = request.nextUrl.searchParams.get("questionId");
  const examId = request.nextUrl.searchParams.get("examId");

  const params = new URLSearchParams();
  if (questionId && Number.isFinite(Number(questionId))) params.set("questionId", questionId);
  if (examId && Number.isFinite(Number(examId))) params.set("examId", examId);

  const query = params.toString();
  const payload = await requestJson<ApiEnvelope<BackendSubmissionRow[]>>(
    `/friend/user/question/submission/list${query ? `?${query}` : ""}`,
    { token }
  );

  return NextResponse.json(unwrapData(payload).map(mapSubmission));
}
