import type { ApiEnvelope, TableEnvelope } from "../client";
import { requestJson, unwrapData, unwrapTable } from "../client";
import type { ExamSummary } from "../contracts";
import { exams } from "../mock/training";

type BackendExamRow = {
  examId?: string | number | null;
  title?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  examStartTime?: string | null;
  examEndTime?: string | null;
  durationMinutes?: number | null;
  questionCount?: number | null;
  status?: number | string | null;
};

function parseTime(value?: string | null) {
  if (!value) return null;
  const date = new Date(value.replace(" ", "T"));
  return Number.isNaN(date.getTime()) ? null : date;
}

function normalizeExamStatus(row: BackendExamRow): ExamSummary["status"] {
  if (row.status === 1 || row.status === "1" || row.status === "进行中") return "进行中";
  if (row.status === 2 || row.status === "2" || row.status === "已结束") return "已结束";

  const start = parseTime(row.startTime ?? row.examStartTime);
  const end = parseTime(row.endTime ?? row.examEndTime);
  const now = new Date();

  if (start && now < start) return "未开始";
  if (end && now > end) return "已结束";
  if (start && end && now >= start && now <= end) return "进行中";
  return "未开始";
}

function calculateDurationMinutes(row: BackendExamRow) {
  if (row.durationMinutes != null) return row.durationMinutes;

  const start = parseTime(row.startTime ?? row.examStartTime);
  const end = parseTime(row.endTime ?? row.examEndTime);
  if (!start || !end) return 0;

  return Math.max(0, Math.round((end.getTime() - start.getTime()) / 60000));
}

function mapExamRow(row: BackendExamRow): ExamSummary {
  return {
    examId: row.examId ? String(row.examId) : "unknown-exam",
    title: row.title ?? "未命名考试",
    status: normalizeExamStatus(row),
    startTime: row.startTime ?? row.examStartTime ?? "--",
    endTime: row.endTime ?? row.examEndTime ?? "--",
    durationMinutes: calculateDurationMinutes(row),
    questionCount: row.questionCount ?? 0
  };
}

export async function fetchLiveExamList() {
  const payload = await requestJson<TableEnvelope<BackendExamRow>>("/friend/exam/semiLogin/list?pageNum=1&pageSize=20");
  return unwrapTable(payload).rows.map(mapExamRow);
}

export async function fetchExamFirstQuestion(examId: string, token: string) {
  const payload = await requestJson<ApiEnvelope<string>>(`/friend/exam/getFirstQuestion?examId=${encodeURIComponent(examId)}`, {
    token
  });
  return unwrapData(payload);
}

export function getExamMockFallback(examId?: string) {
  return exams.find((item) => item.examId === examId) ?? exams[0];
}
